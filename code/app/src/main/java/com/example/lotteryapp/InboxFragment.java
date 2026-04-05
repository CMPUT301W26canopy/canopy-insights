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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A fragment representing a list of items for the inbox of a user.
 * Fetches notifications from Firestore based on the current user's accountID.
 */
public class InboxFragment extends Fragment {

    private String accountID;
    private MyInboxRecyclerViewAdapter adapter;
    private final List<NotificationModel> notificationList = new ArrayList<>();
    private RecyclerView recyclerView;
    private TextView emptyView;

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

        recyclerView = view.findViewById(R.id.list);
        emptyView = view.findViewById(R.id.tv_empty_inbox);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new MyInboxRecyclerViewAdapter(notificationList);
        recyclerView.setAdapter(adapter);

        ImageButton btnClose = view.findViewById(R.id.btn_close_inbox);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> {
                getParentFragmentManager().beginTransaction()
                        .remove(InboxFragment.this)
                        .commit();
            });
        }

        if (accountID != null && !accountID.trim().isEmpty()) {
            fetchNotifications();
        } else {
            Toast.makeText(getContext(), "Error: No Account ID found", Toast.LENGTH_SHORT).show();
            updateEmptyState();
        }
    }

    private void fetchNotifications() {
        FirestoreHelper.getDb().collection("notifications")
                .document(accountID)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    notificationList.clear();

                    if (documentSnapshot.exists()) {
                        Object listObject = documentSnapshot.get("notificationList");
                        if (listObject instanceof List) {
                            List<?> rawList = (List<?>) listObject;

                            for (Object rawItem : rawList) {
                                if (!(rawItem instanceof Map)) {
                                    continue;
                                }

                                Map<String, Object> map = (Map<String, Object>) rawItem;
                                NotificationModel notification = new NotificationModel();
                                notification.setSenderAccountID(asString(map.get("senderAccountID")));
                                notification.setReceiverAccountID(asString(map.get("receiverAccountID")));
                                notification.setMessage(asString(map.get("message")));
                                notification.setTimestamp(asString(map.get("timestamp")));
                                notification.setEventId(firstNonBlank(
                                        asString(map.get("eventID")),
                                        asString(map.get("eventId"))
                                ));
                                notificationList.add(notification);
                            }
                        }
                    }

                    Collections.sort(notificationList, (left, right) ->
                            Long.compare(parseTimestamp(right.getTimestamp()), parseTimestamp(left.getTimestamp())));

                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Failed to load notifications", Toast.LENGTH_SHORT).show();
                    }
                    updateEmptyState();
                });
    }

    private void updateEmptyState() {
        boolean isEmpty = notificationList.isEmpty();
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }

    private long parseTimestamp(String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return 0L;
        }

        if (rawValue.matches("\\d{10}")) {
            try {
                int year = Calendar.getInstance().get(Calendar.YEAR);
                String expanded = year + rawValue;
                SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
                format.setLenient(false);
                Date parsed = format.parse(expanded);
                return parsed != null ? parsed.getTime() : 0L;
            } catch (ParseException ignored) {
            }
        }

        String[] patterns = {
                "yyyy-MM-dd HH:mm:ss",
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

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.trim().isEmpty()) {
            return first;
        }
        if (second != null && !second.trim().isEmpty()) {
            return second;
        }
        return null;
    }
}
