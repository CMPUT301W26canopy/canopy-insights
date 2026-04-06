package com.example.lotteryapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * A fragment representing a list of items for the inbox of a user.
 * Fetches notifications from Firestore based on the current user's accountID.
 */
public class InboxFragment extends Fragment {

    private String accountID;
    private MyInboxRecyclerViewAdapter adapter;
    private final List<NotificationModel> notificationList = new ArrayList<>();

    public InboxFragment() {}

    public static InboxFragment newInstance(String accountID) {
        InboxFragment fragment = new InboxFragment();
        Bundle args = new Bundle();
        args.putString("accountID", accountID);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            accountID = getArguments().getString("accountID");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_inbox_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView recyclerView = view.findViewById(R.id.list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new MyInboxRecyclerViewAdapter(notificationList);
        recyclerView.setAdapter(adapter);

        ImageButton btnClose = view.findViewById(R.id.btn_close_inbox);
        if (btnClose != null) {
            btnClose.setOnClickListener(v ->
                    getParentFragmentManager().beginTransaction()
                            .remove(InboxFragment.this)
                            .commit());
        }

        if (accountID != null) {
            checkOptOutThenFetch();
        } else {
            Toast.makeText(getContext(), "Please log in to view notifications",
                    Toast.LENGTH_SHORT).show();
        }
    }

    // checks if user opted out before loading — covers #12
    private void checkOptOutThenFetch() {
        FirestoreHelper.getDb().collection("accounts")
                .document(accountID)
                .get()
                .addOnSuccessListener(doc -> {
                    ProfileModel profile = doc.toObject(ProfileModel.class);
                    if (profile != null && !profile.isNotificationEnabled()) {
                        showEmpty("Notifications are turned off");
                        return;
                    }
                    fetchNotifications();
                })
                .addOnFailureListener(e -> fetchNotifications());
    }

    // reads individual notification documents by receiverAccountID
    private void fetchNotifications() {
        FirestoreHelper.getDb().collection("notifications")
                .whereEqualTo("receiverAccountID", accountID)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    notificationList.clear();
                    for (QueryDocumentSnapshot doc : snap) {
                        NotificationModel notif = new NotificationModel();
                        notif.setSenderAccountID(doc.getString("senderAccountID"));
                        notif.setReceiverAccountID(doc.getString("receiverAccountID"));
                        notif.setMessage(doc.getString("message"));
                        notif.setTimestamp(doc.getString("timestamp"));
                        notif.setEventId(doc.getString("eventId"));
                        notificationList.add(notif);
                    }
                    adapter.notifyDataSetChanged();
                    if (notificationList.isEmpty()) showEmpty("No notifications yet");
                })
                .addOnFailureListener(e -> {
                    if (isAdded())
                        Toast.makeText(getContext(), "Failed to load notifications",
                                Toast.LENGTH_SHORT).show();
                });
    }

    private void showEmpty(String message) {
        if (!isAdded() || getView() == null) return;
        TextView tv = getView().findViewById(R.id.tvEmptyInbox);
        if (tv != null) {
            tv.setVisibility(View.VISIBLE);
            tv.setText(message);
        }
    }
}