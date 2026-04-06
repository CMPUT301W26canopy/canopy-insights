package com.example.lotteryapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * A dialog fragment that allows an organizer to send notifications to specific groups of applicants
 * (e.g., waiting, selected, cancelled, accepted) for a particular event.
 */
public class SendNotificationFragment extends DialogFragment {

    private String eventId;
    private String eventName;
    private Spinner spinnerGroups;
    private EditText etMessage;

    /**
     * Creates a new instance of SendNotificationFragment with event details.
     *
     * @param eventId   The unique ID of the event.
     * @param eventName The name of the event.
     * @return A new instance of SendNotificationFragment.
     */
    public static SendNotificationFragment newInstance(String eventId, String eventName) {
        SendNotificationFragment fragment = new SendNotificationFragment();
        Bundle args = new Bundle();
        args.putString("EVENT_ID", eventId);
        args.putString("EVENT_NAME", eventName);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Initializes the fragment and retrieves event details from arguments.
     *
     * @param savedInstanceState If the fragment is being re-created from a previous saved state.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            eventId = getArguments().getString("EVENT_ID");
            eventName = getArguments().getString("EVENT_NAME");
        }
    }

    /**
     * Inflates the layout for the notification dialog and initializes UI components.
     *
     * @param inflater           The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container          If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     * @return The View for the fragment's UI.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_send_notification, container, false);

        spinnerGroups = view.findViewById(R.id.spinnerGroups);
        etMessage = view.findViewById(R.id.etNotificationMessage);
        Button btnCancel = view.findViewById(R.id.btnCancel);
        Button btnSend = view.findViewById(R.id.btnSend);

        String[] groups = {"Waiting", "Selected", "Cancelled", "Accepted"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, groups);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGroups.setAdapter(adapter);

        btnCancel.setOnClickListener(v -> dismiss());

        btnSend.setOnClickListener(v -> {
            String message = etMessage.getText().toString().trim();
            if (message.isEmpty()) {
                Toast.makeText(getContext(), "Please enter a message", Toast.LENGTH_SHORT).show();
                return;
            }
            sendNotification(spinnerGroups.getSelectedItem().toString().toLowerCase(), message);
        });

        return view;
    }

    /**
     * Fetches users with the specified status for the current event and sends a notification to them.
     *
     * @param status  The applicant status group to target (e.g., "waiting", "selected").
     * @param message The content of the notification message.
     */
    private void sendNotification(String status, String message) {
        FirestoreHelper.getDb().collection("applications")
                .whereEqualTo("eventId", eventId)
                .whereEqualTo("status", status)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<String> userIds = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String userId = doc.getString("userId");
                        if (userId != null) userIds.add(userId);
                    }

                    if (userIds.isEmpty()) {
                        Toast.makeText(getContext(), "No users found in this group", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String senderId = DeviceData.getInstance(requireContext()).getAccountID();
                    if (senderId == null) senderId = "SYSTEM";

                    NotificationHelper.sendCustomNotification(senderId, eventId, eventName, userIds, message)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(getContext(), "Notification sent to " + userIds.size() + " users", Toast.LENGTH_SHORT).show();
                                dismiss();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(getContext(), "Failed to send notification: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error fetching users: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Sets the dialog window dimensions when the fragment starts.
     */
    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}
