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

import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class InboxFragment extends Fragment {

    private String accountID;
    private MyInboxRecyclerViewAdapter adapter;
    private final List<NotificationModel> notificationList = new ArrayList<>();
    private RecyclerView recyclerView;
    private TextView emptyView;

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

        recyclerView = view.findViewById(R.id.list);
        emptyView = view.findViewById(R.id.tvEmptyInbox);
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

        if (accountID != null && !accountID.trim().isEmpty()) {
            fetchNotifications();
        } else {
            Toast.makeText(getContext(), "Please log in to view notifications",
                    Toast.LENGTH_SHORT).show();
            showEmpty("Please log in to view notifications");
        }
    }

    private void fetchNotifications() {
        FirestoreHelper.getDb().collection("notifications")
                .whereEqualTo("receiverAccountID", accountID)
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

                    Collections.sort(notificationList, (left, right) ->
                            Long.compare(parseTimestamp(right.getTimestamp()), parseTimestamp(left.getTimestamp())));

                    adapter.notifyDataSetChanged();
                    if (notificationList.isEmpty()) {
                        showEmpty("No notifications yet");
                    } else {
                        showList();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Failed to load notifications",
                                Toast.LENGTH_SHORT).show();
                    }
                    showEmpty("Failed to load notifications");
                });
    }

    private long parseTimestamp(String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return 0L;
        }

        String[] patterns = {
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd HH:mm",
                "yyyy-MM-dd",
                "MM-dd-yyyy"
        };

        for (String pattern : patterns) {
            try {
                SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.getDefault());
                format.setLenient(false);
                Date parsed = format.parse(rawValue);
                if (parsed != null) {
                    return parsed.getTime();
                }
            } catch (ParseException ignored) {
            }
        }

        return 0L;
    }

    private void showEmpty(String message) {
        if (!isAdded() || emptyView == null || recyclerView == null) {
            return;
        }

        emptyView.setVisibility(View.VISIBLE);
        emptyView.setText(message);
        recyclerView.setVisibility(View.GONE);
    }

    private void showList() {
        if (!isAdded() || emptyView == null || recyclerView == null) {
            return;
        }

        emptyView.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
    }
}
