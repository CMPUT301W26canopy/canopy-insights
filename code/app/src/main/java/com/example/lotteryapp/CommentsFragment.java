package com.example.lotteryapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Displays the threaded event comments feed, including replies, likes,
 * organizer moderation, and basic profile details for each poster.
 */
public class CommentsFragment extends Fragment {

    private static final String ARG_EVENT_ID = "event_id";
    private String eventId;
    private List<CommentModel> commentsList = new ArrayList<>();
    private Set<String> removedCommentsSet = new HashSet<>();
    private CommentsAdapter adapter;
    private FirebaseFirestore db;
    private String replyingToId = "0";
    private String currentUserId;
    
    private boolean isOrganizerOrCohost = false;

    private View replyBar;
    private TextView tvReplyStatus, tvReplySnippet;
    
    private Map<String, String> userNamesCache = new HashMap<>();
    private Map<String, String> userUsernamesCache = new HashMap<>();
    private Map<String, String> userImagesCache = new HashMap<>();

    public static CommentsFragment newInstance(String eventId) {
        CommentsFragment fragment = new CommentsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EVENT_ID, eventId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            eventId = getArguments().getString(ARG_EVENT_ID);
        }
        db = FirestoreHelper.getDb();
        if (getContext() != null) {
            currentUserId = DeviceData.getInstance(requireContext().getApplicationContext()).getAccountID();
        }
        checkUserPrivileges();
    }

    private void checkUserPrivileges() {
        if (eventId == null || currentUserId == null) return;
        
        db.collection("events").document(eventId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                String organizerId = doc.getString("organizerId");
                List<String> cohosts = (List<String>) doc.get("invitedHosts");
                
                if (currentUserId.equals(organizerId) || (cohosts != null && cohosts.contains(currentUserId))) {
                    isOrganizerOrCohost = true;
                    if (adapter != null) adapter.notifyDataSetChanged();
                }
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_comments, container, false);

        ImageButton btnClose = view.findViewById(R.id.btnCloseComments);
        btnClose.setOnClickListener(v -> getParentFragmentManager().beginTransaction().remove(this).commit());

        RecyclerView rvComments = view.findViewById(R.id.rvComments);
        rvComments.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new CommentsAdapter(commentsList);
        rvComments.setAdapter(adapter);

        EditText etInput = view.findViewById(R.id.etCommentInput);
        Button btnSend = view.findViewById(R.id.btnSendComment);

        replyBar = view.findViewById(R.id.replyBar);
        tvReplyStatus = view.findViewById(R.id.tvReplyStatus);
        tvReplySnippet = view.findViewById(R.id.tvReplySnippet);
        ImageButton btnCancelReply = view.findViewById(R.id.btnCancelReply);

        btnCancelReply.setOnClickListener(v -> cancelReply());

        btnSend.setOnClickListener(v -> {
            String text = etInput.getText().toString().trim();
            if (!text.isEmpty()) {
                sendComment(text);
                etInput.setText("");
            }
        });

        loadComments();

        return view;
    }

    private void cancelReply() {
        replyingToId = "0";
        if (replyBar != null) replyBar.setVisibility(View.GONE);
    }

    private void loadComments() {
        if (eventId == null) return;
        
        db.collection("eventComments").document(eventId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        return;
                    }
                    if (snapshot != null && snapshot.exists()) {
                        List<String> removedIds = (List<String>) snapshot.get("removedComments");
                        removedCommentsSet.clear();
                        if (removedIds != null) {
                            removedCommentsSet.addAll(removedIds);
                        }

                        List<Map<String, Object>> rawList = (List<Map<String, Object>>) snapshot.get("commentsList");
                        if (rawList != null) {
                            List<CommentModel> allComments = new ArrayList<>();
                            Set<String> userIdsToFetch = new HashSet<>();
                            for (Map<String, Object> map : rawList) {
                                CommentModel comment = new CommentModel();
                                comment.setMessage((String) map.get("message"));
                                comment.setPosterID((String) map.get("posterID"));
                                comment.setMessageID((String) map.get("messageID"));
                                comment.setParentID((String) map.get("parentID"));
                                comment.setTimestamp((String) map.get("timestamp"));
                                
                                List<String> likedBy = (List<String>) map.get("likedBy");
                                comment.setLikedBy(likedBy != null ? new ArrayList<>(likedBy) : new ArrayList<>());
                                
                                allComments.add(comment);
                                
                                String posterId = comment.getPosterID();
                                if (posterId != null && !userNamesCache.containsKey(posterId)) {
                                    userIdsToFetch.add(posterId);
                                }
                            }
                            
                            if (userIdsToFetch.isEmpty()) {
                                updateDisplayList(allComments);
                            } else {
                                fetchUsernamesAndRefresh(userIdsToFetch, allComments);
                            }
                        }
                    } else {
                        commentsList.clear();
                        removedCommentsSet.clear();
                        if (adapter != null) adapter.notifyDataSetChanged();
                    }
                });
    }

    /**
     * Resolves poster names and profile images so the comment list can show
     * friendlier identity details than raw account IDs.
     */
    private void fetchUsernamesAndRefresh(Set<String> userIds, List<CommentModel> allComments) {
        if (userIds.isEmpty()) {
            updateDisplayList(allComments);
            return;
        }

        List<Task<DocumentSnapshot>> tasks = new ArrayList<>();
        for (String uid : userIds) {
            tasks.add(db.collection("accounts").document(uid).get());
        }

        Tasks.whenAllComplete(tasks).addOnCompleteListener(t -> {
            if (!isAdded()) return;
            for (Task<DocumentSnapshot> task : tasks) {
                if (task.isSuccessful() && task.getResult() != null) {
                    DocumentSnapshot doc = task.getResult();
                    String uid = doc.getId();
                    String username = doc.getString("username");
                    String name = doc.getString("name");
                    userNamesCache.put(uid, firstNonBlank(name, username, "Unknown"));
                    userUsernamesCache.put(uid, firstNonBlank(username, uid));
                    userImagesCache.put(uid, doc.getString("profileImage"));
                }
            }
            updateDisplayList(allComments);
        });
    }

    private void updateDisplayList(List<CommentModel> allComments) {
        commentsList.clear();
        commentsList.addAll(organizeComments(allComments));
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private List<CommentModel> organizeComments(List<CommentModel> all) {
        Collections.sort(all, (c1, c2) -> c1.getTimestamp().compareTo(c2.getTimestamp()));

        Map<String, List<CommentModel>> childrenMap = new HashMap<>();
        List<CommentModel> topLevel = new ArrayList<>();

        for (CommentModel c : all) {
            String parent = c.getParentID();
            if (parent == null || parent.equals("0")) {
                topLevel.add(c);
            } else {
                if (!childrenMap.containsKey(parent)) {
                    childrenMap.put(parent, new ArrayList<>());
                }
                childrenMap.get(parent).add(c);
            }
        }

        List<CommentModel> result = new ArrayList<>();
        for (CommentModel parent : topLevel) {
            addCommentAndReplies(parent, result, childrenMap, 0);
        }
        return result;
    }

    private void addCommentAndReplies(CommentModel current, List<CommentModel> result, Map<String, List<CommentModel>> childrenMap, int depth) {
        current.setDepth(depth);
        result.add(current);
        List<CommentModel> replies = childrenMap.get(current.getMessageID());
        if (replies != null) {
            for (CommentModel reply : replies) {
                addCommentAndReplies(reply, result, childrenMap, depth + 1);
            }
        }
    }

    private void sendComment(String text) {
        if (eventId == null || currentUserId == null) return;
        
        String timestamp = String.valueOf(System.currentTimeMillis());

        Map<String, Object> comment = new HashMap<>();
        comment.put("message", text);
        comment.put("posterID", currentUserId);
        comment.put("timestamp", timestamp);
        comment.put("messageID", timestamp + "_" + currentUserId);
        comment.put("parentID", replyingToId);
        comment.put("likedBy", new ArrayList<String>());

        Map<String, Object> data = new HashMap<>();
        data.put("commentsList", FieldValue.arrayUnion(comment));

        db.collection("eventComments").document(eventId)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Comment posted!", Toast.LENGTH_SHORT).show();
                        cancelReply();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Failed to post: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Soft-removes a comment by adding it to the event's removed list.
     */
    private void deleteComment(CommentModel commentToDelete) {
        if (eventId == null) return;

        db.collection("eventComments").document(eventId)
                .update("removedComments", FieldValue.arrayUnion(commentToDelete.getMessageID()))
                .addOnSuccessListener(aVoid -> {
                    if (isAdded()) Toast.makeText(getContext(), "Comment marked as removed", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Failed to remove: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Toggles the current user's like on a single comment inside the stored list.
     */
    private void toggleLike(CommentModel comment) {
        if (eventId == null || currentUserId == null) return;

        db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(db.collection("eventComments").document(eventId));
            List<Map<String, Object>> rawList = (List<Map<String, Object>>) snapshot.get("commentsList");
            
            if (rawList != null) {
                List<Map<String, Object>> updatedList = new ArrayList<>();
                for (Map<String, Object> map : rawList) {
                    Map<String, Object> commentMap = new HashMap<>(map);
                    if (comment.getMessageID().equals(commentMap.get("messageID"))) {
                        List<String> likedByRaw = (List<String>) commentMap.get("likedBy");
                        List<String> likedBy = likedByRaw == null ? new ArrayList<>() : new ArrayList<>(likedByRaw);

                        if (likedBy.contains(currentUserId)) {
                            likedBy.remove(currentUserId);
                        } else {
                            likedBy.add(currentUserId);
                        }
                        commentMap.put("likedBy", likedBy);
                    }
                    updatedList.add(commentMap);
                }
                transaction.update(db.collection("eventComments").document(eventId), "commentsList", updatedList);
            }
            return null;
        }).addOnFailureListener(e -> {
            if (isAdded()) Toast.makeText(getContext(), "Error updating like", Toast.LENGTH_SHORT).show();
        });
    }

    private class CommentsAdapter extends RecyclerView.Adapter<CommentsAdapter.ViewHolder> {
        private List<CommentModel> data;
        CommentsAdapter(List<CommentModel> data) { this.data = data; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            CommentModel c = data.get(position);
            
            boolean isRemoved = removedCommentsSet.contains(c.getMessageID());

            if (isRemoved) {
                holder.tvUser.setText("Removed");
                holder.tvMeta.setText("");
                holder.tvText.setText("This comment was removed.");
                bindProfileImage(holder.avatarView, null);
                holder.btnDelete.setVisibility(View.GONE);
                holder.btnReply.setVisibility(View.GONE);
                holder.btnLike.setVisibility(View.GONE);
            } else {
                String displayName = userNamesCache.get(c.getPosterID());
                holder.tvUser.setText(displayName != null ? displayName : c.getPosterID());
                String username = userUsernamesCache.get(c.getPosterID());
                holder.tvMeta.setText(username != null ? "@" + username : "");
                holder.tvText.setText(c.getMessage());
                bindProfileImage(holder.avatarView, userImagesCache.get(c.getPosterID()));
                
                if (currentUserId != null && (currentUserId.equals(c.getPosterID()) || isOrganizerOrCohost)) {
                    holder.btnDelete.setVisibility(View.VISIBLE);
                } else {
                    holder.btnDelete.setVisibility(View.GONE);
                }

                holder.btnReply.setVisibility(View.VISIBLE);
                holder.btnLike.setVisibility(View.VISIBLE);

                boolean isLiked = currentUserId != null && c.getLikedBy() != null && c.getLikedBy().contains(currentUserId);
                int count = c.getLikedBy() != null ? c.getLikedBy().size() : 0;
                holder.btnLike.setText((isLiked ? "Liked (" : "Like (") + count + ")");
                holder.btnLike.setTextColor(isLiked ? Color.parseColor("#FF69B4") : Color.GRAY);
            }

            int depth = c.getDepth();
            int paddingStart = 12 + (depth * 16);
            float scale = holder.itemView.getContext().getResources().getDisplayMetrics().density;
            int paddingPx = (int) (paddingStart * scale + 0.5f);
            holder.itemView.setPadding(paddingPx, (int)(12 * scale), (int)(12 * scale), (int)(12 * scale));

            holder.btnLike.setOnClickListener(v -> toggleLike(c));

            holder.btnReply.setOnClickListener(v -> {
                replyingToId = c.getMessageID();
                if (replyBar != null) {
                    replyBar.setVisibility(View.VISIBLE);
                    String replyToName = userNamesCache.get(c.getPosterID());
                    tvReplyStatus.setText("Replying to " + (replyToName != null ? replyToName : c.getPosterID()));
                    tvReplySnippet.setText(c.getMessage());
                }
            });

            holder.btnDelete.setOnClickListener(v -> {
                deleteComment(c);
            });
        }

        @Override
        public int getItemCount() { return data.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvUser, tvMeta, tvText;
            ImageView avatarView;
            Button btnReply, btnDelete, btnLike;
            ViewHolder(View v) {
                super(v);
                avatarView = v.findViewById(R.id.ivCommentAvatar);
                tvUser = v.findViewById(R.id.tvCommentUser);
                tvMeta = v.findViewById(R.id.tvCommentMeta);
                tvText = v.findViewById(R.id.tvCommentText);
                btnReply = v.findViewById(R.id.btnReplyComment);
                btnDelete = v.findViewById(R.id.btnDeleteComment);
                btnLike = v.findViewById(R.id.btnLikeComment);
            }
        }
    }

    private void bindProfileImage(ImageView imageView, String profileImage) {
        if (imageView == null) {
            return;
        }
        if (profileImage != null && !profileImage.trim().isEmpty()) {
            try {
                byte[] decoded = Base64.decode(profileImage, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap);
                    return;
                }
            } catch (Exception ignored) {
            }
        }
        imageView.setImageResource(R.drawable.ic_person);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value;
            }
        }
        return null;
    }
}
