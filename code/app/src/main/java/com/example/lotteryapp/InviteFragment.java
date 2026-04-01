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
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * A pop-up fragment for inviting participants to an event.
 */
public class InviteFragment extends BottomSheetDialogFragment {

    private static final String ARG_EVENT_ID = "event_id";
    private String eventId;
    private ProfileSearchAdapter searchAdapter;
    private FirebaseFirestore db;

    public InviteFragment() {
        // Required empty public constructor
    }

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

        view.findViewById(R.id.btn_confirm_invite).setOnClickListener(v -> {
            sendInvites();
        });
    }

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

        Button hostOrParticipantBtn = getView().findViewById(R.id.hostOrParticipant);
        String role = (hostOrParticipantBtn != null) ? hostOrParticipantBtn.getText().toString() : "Participant";
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        WriteBatch batch = db.batch();
        
        // If inviting as Co-host, add them to the invitedHosts list in the event document
        if (role.equalsIgnoreCase("Co-host") && eventId != null) {
            batch.update(db.collection("events").document(eventId), 
                    "invitedHosts", FieldValue.arrayUnion(selectedIds.toArray()));
        }

        for (String receiverId : selectedIds) {
            // Create the notification map to store in the array
            Map<String, Object> notificationMap = new HashMap<>();
            notificationMap.put("senderAccountID", senderId);
            notificationMap.put("receiverAccountID", receiverId);
            notificationMap.put("message", "Invitation: You have been invited as a " + role + " to join an event!");
            notificationMap.put("timestamp", timestamp);
            notificationMap.put("eventID", eventId);

            // Use set with SetOptions.merge() so the document is created if it doesn't exist,
            // and arrayUnion to add to the existing notificationList.
            Map<String, Object> updateData = new HashMap<>();
            updateData.put("notificationList", FieldValue.arrayUnion(notificationMap));
            
            batch.set(db.collection("notifications").document(receiverId), updateData, SetOptions.merge());
        }

        batch.commit().addOnSuccessListener(aVoid -> {
            if (isAdded()) {
                Toast.makeText(getContext(), "Invites sent successfully!", Toast.LENGTH_SHORT).show();
                dismiss();
            }
        }).addOnFailureListener(e -> {
            if (isAdded()) {
                Toast.makeText(getContext(), "Failed to send invites: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
