package com.example.lotteryapp;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A fragment representing a list of items for the inbox of a user.
 * Fetches notifications from Firestore based on the current user's accountID.
 */
public class InboxFragment extends Fragment {

    private String accountID;
    private MyInboxRecyclerViewAdapter adapter;
    private List<NotificationModel> notificationList = new ArrayList<>();

    public InboxFragment() {
    }

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

        // Close button logic
        ImageButton btnClose = view.findViewById(R.id.btn_close_inbox);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> {
                getParentFragmentManager().beginTransaction()
                        .remove(InboxFragment.this)
                        .commit();
            });
        }

        if (accountID != null) {
            fetchNotifications();
        } else {
            Toast.makeText(getContext(), "Error: No Account ID found", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchNotifications() {
        FirestoreHelper.getDb().collection("notifications")
                .document(accountID)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Retrieve the list named "notificationList" from the document
                        List<Map<String, Object>> list = (List<Map<String, Object>>) documentSnapshot.get("notificationList");
                        if (list != null) {
                            notificationList.clear();
                            for (Map<String, Object> map : list) {
                                NotificationModel notif = new NotificationModel();
                                notif.setSenderAccountID((String) map.get("senderAccountID"));
                                notif.setReceiverAccountID((String) map.get("receiverAccountID"));
                                notif.setMessage((String) map.get("message"));
                                notif.setTimestamp((String) map.get("timestamp"));
                                notificationList.add(notif);
                            }
                            adapter.notifyDataSetChanged();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Failed to load notifications", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
