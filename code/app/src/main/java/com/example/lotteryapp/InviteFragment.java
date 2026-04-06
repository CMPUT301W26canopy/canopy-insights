package com.example.lotteryapp;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Bottom sheet used by organizers to search accounts and invite them either
 * as private-event participants or as co-organizers.
 */
public class InviteFragment extends BottomSheetDialogFragment {

    private static final String ARG_EVENT_ID = "event_id";
    private String eventId;
    private ProfileSearchAdapter searchAdapter;
    private FirebaseFirestore db;

    /**
     * Default constructor for InviteFragment.
     */
    public InviteFragment() {
        // Required empty public constructor
    }

    /**
     * Creates a new instance of InviteFragment for a specific event.
     * @param eventId The unique ID of the event.
     * @return A new instance of InviteFragment.
     */
    public static InviteFragment newInstance(String eventId) {
        InviteFragment fragment = new InviteFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EVENT_ID, eventId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            eventId = getArguments().getString(ARG_EVENT_ID);
        }
        db = FirebaseFirestore.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_invite, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageButton closeBtn = view.findViewById(R.id.btn_close_invite);
        if (closeBtn != null) {
            closeBtn.setOnClickListener(v -> dismiss());
        }

        Button hostOrParticipantBtn = view.findViewById(R.id.hostOrParticipant);
        if (hostOrParticipantBtn != null) {
            hostOrParticipantBtn.setText("Participant");
            hostOrParticipantBtn.setOnClickListener(v -> {
                String currentText = hostOrParticipantBtn.getText().toString();
                if (currentText.equalsIgnoreCase("Participant")) {
                    hostOrParticipantBtn.setText("Co-host");
                } else {
                    hostOrParticipantBtn.setText("Participant");
                }
            });
        }

        RecyclerView searchRecyclerView = view.findViewById(R.id.search_profiles);
        searchRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        searchAdapter = new ProfileSearchAdapter();
        searchRecyclerView.setAdapter(searchAdapter);

        SearchView searchView = view.findViewById(R.id.search_view);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                performSearch(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.length() >= 2) {
                    performSearch(newText);
                } else if (newText.isEmpty()) {
                    searchAdapter.setProfiles(new ArrayList<>());
                }
                return true;
            }
        });

        view.findViewById(R.id.btn_confirm_invite).setOnClickListener(v -> sendInvites());
    }

    /**
     * Searches accounts by the common profile fields shown in the story.
     * @param query The search query string.
     */
    private void performSearch(String query) {
        Task<QuerySnapshot> nameQuery = db.collection("accounts")
                .orderBy("name")
                .startAt(query)
                .endAt(query + "\uf8ff")
                .get();

        Task<QuerySnapshot> userNameQuery = db.collection("accounts")
                .orderBy("username")
                .startAt(query)
                .endAt(query + "\uf8ff")
                .get();

        Task<QuerySnapshot> emailQuery = db.collection("accounts")
                .orderBy("email")
                .startAt(query)
                .endAt(query + "\uf8ff")
                .get();

        Task<QuerySnapshot> phoneQuery = db.collection("accounts")
                .orderBy("phoneNumber")
                .startAt(query)
                .endAt(query + "\uf8ff")
                .get();

        Tasks.whenAllSuccess(nameQuery, emailQuery, phoneQuery, userNameQuery).addOnSuccessListener(results -> {
            Set<ProfileModel> mergedProfiles = new HashSet<>();
            for (Object result : results) {
                QuerySnapshot snapshot = (QuerySnapshot) result;
                for (DocumentSnapshot doc : snapshot.getDocuments()) {
                    ProfileModel profile = doc.toObject(ProfileModel.class);
                    if (profile != null) {
                        profile.setAccountID(doc.getId());
                        mergedProfiles.add(profile);
                    }
                }
            }
            searchAdapter.setProfiles(new ArrayList<>(mergedProfiles));
        }).addOnFailureListener(e -> {
            Log.e("InviteFragment", "Search failed", e);
            Toast.makeText(getContext(), "Search failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Persists the chosen invite type on the event and sends the matching
     * in-app notifications.
     */
    private void sendInvites() {
        Set<String> selectedIds = searchAdapter.getSelectedProfileIds();
        if (selectedIds.isEmpty()) {
            Toast.makeText(getContext(), "No participants selected", Toast.LENGTH_SHORT).show();
            return;
        }

        String senderId = DeviceData.getInstance(requireContext()).getAccountID();
        if (senderId == null) {
            Toast.makeText(getContext(), "Error: Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        Button hostOrParticipantBtn = getView() != null
                ? getView().findViewById(R.id.hostOrParticipant)
                : null;
        String role = hostOrParticipantBtn != null
                ? hostOrParticipantBtn.getText().toString()
                : "Participant";
        String message = role.equalsIgnoreCase("Co-host")
                ? "Invitation: You have been assigned as a co-organizer for this event."
                : "Invitation: You have been invited to join the waiting list for this event.";

        Task<Void> eventUpdateTask = Tasks.forResult(null);
        if (role.equalsIgnoreCase("Co-host") && eventId != null) {
            eventUpdateTask = assignCoHosts(selectedIds);
        } else if (eventId != null) {
            eventUpdateTask = assignParticipantInvites(selectedIds);
        }

        eventUpdateTask
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        Exception exception = task.getException();
                        return Tasks.forException(exception != null
                                ? exception
                                : new IllegalStateException("Failed to update event invitees"));
                    }
                    return NotificationHelper.sendNotifications(
                            senderId,
                            new ArrayList<>(selectedIds),
                            message,
                            eventId
                    );
                })
                .addOnSuccessListener(aVoid -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Invites sent successfully!", Toast.LENGTH_SHORT).show();
                        dismiss();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(),
                                "Failed to send invites: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Marks users as invited participants for a private event so they can
     * accept or decline that invite from the event screen.
     * @param selectedIds The set of user IDs to invite.
     * @return A Task representing the asynchronous update.
     */
    private Task<Void> assignParticipantInvites(Set<String> selectedIds) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("invitedParticipants", FieldValue.arrayUnion(selectedIds.toArray()));
        updates.put("declinedParticipantInvites", FieldValue.arrayRemove(selectedIds.toArray()));
        return db.collection("events").document(eventId).update(updates);
    }

    /**
     * Assigns users as co-hosts and removes them from the entrant pool for the
     * same event if they were already waiting.
     * @param selectedIds The set of user IDs to assign as co-hosts.
     * @return A Task representing the asynchronous update.
     */
    private Task<Void> assignCoHosts(Set<String> selectedIds) {
        Map<String, Object> eventUpdates = new HashMap<>();
        eventUpdates.put("invitedHosts", FieldValue.arrayUnion(selectedIds.toArray()));
        eventUpdates.put("invitedParticipants", FieldValue.arrayRemove(selectedIds.toArray()));
        eventUpdates.put("declinedParticipantInvites", FieldValue.arrayRemove(selectedIds.toArray()));
        eventUpdates.put("waitingList", FieldValue.arrayRemove(selectedIds.toArray()));

        Task<Void> updateEventTask = db.collection("events").document(eventId).update(eventUpdates);
        Task<Void> removeApplicationsTask = db.collection("applications")
                .whereEqualTo("eventId", eventId)
                .get()
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        Exception exception = task.getException();
                        return Tasks.forException(exception != null
                                ? exception
                                : new IllegalStateException("Failed to load event applications"));
                    }

                    QuerySnapshot snapshot = task.getResult();
                    if (snapshot == null || snapshot.isEmpty()) {
                        return Tasks.forResult(null);
                    }

                    WriteBatch batch = db.batch();
                    boolean hasDeletes = false;
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String userId = doc.getString("userId");
                        if (userId != null && selectedIds.contains(userId)) {
                            batch.delete(doc.getReference());
                            hasDeletes = true;
                        }
                    }

                    if (!hasDeletes) {
                        return Tasks.forResult(null);
                    }
                    return batch.commit();
                });

        return Tasks.whenAll(updateEventTask, removeApplicationsTask);
    }
}
