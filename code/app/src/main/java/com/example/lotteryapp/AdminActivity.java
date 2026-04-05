package com.example.lotteryapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AdminActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private AdminAdapter adapter;
    private List<Object> itemList = new ArrayList<>();
    private String currentTab = "Events";
    private FirebaseFirestore db;
    private DeviceData deviceData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        deviceData = DeviceData.getInstance(this);
        String username = deviceData.getUsername();
        
        // Security check
        if (username == null || !(username.equals("Heeya") || username.equals("fasih"))) {
            Toast.makeText(this, "Admin Access Denied", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setContentView(R.layout.activity_admin);

        db = FirebaseFirestore.getInstance();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.adminRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminAdapter();
        recyclerView.setAdapter(adapter);

        TabLayout tabLayout = findViewById(R.id.adminTabLayout);
        
        // Explicitly clear and add tabs to avoid issues with layout-defined tabs
        tabLayout.removeAllTabs();
        tabLayout.addTab(tabLayout.newTab().setText("Events"));
        tabLayout.addTab(tabLayout.newTab().setText("Profiles"));
        tabLayout.addTab(tabLayout.newTab().setText("Images"));
        tabLayout.addTab(tabLayout.newTab().setText("Notifications"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getText().toString();
                loadData();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        loadData();
    }

    private void loadData() {
        itemList.clear();
        adapter.notifyDataSetChanged();

        if (currentTab.equalsIgnoreCase("Images")) {
            loadImages();
        } else if (currentTab.equalsIgnoreCase("Notifications")) {
            loadNotifications();
        } else {
            String collectionPath = currentTab.equalsIgnoreCase("Events") ? "events" : "accounts";
            db.collection(collectionPath).get().addOnSuccessListener(queryDocumentSnapshots -> {
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    if (currentTab.equalsIgnoreCase("Events")) {
                        try {
                            EventModel event = doc.toObject(EventModel.class);
                            event.setId(doc.getId());
                            itemList.add(event);
                        } catch (Exception e) {
                            // Fallback for broken data
                            EventModel event = new EventModel();
                            event.setId(doc.getId());
                            event.setName(doc.getString("name"));
                            itemList.add(event);
                        }
                    } else {
                        try {
                            ProfileModel profile = doc.toObject(ProfileModel.class);
                            if (profile.getAccountID() == null) profile.setAccountID(doc.getId());
                            itemList.add(profile);
                        } catch (Exception e) {
                            ProfileModel profile = new ProfileModel();
                            profile.setAccountID(doc.getId());
                            profile.setUsername(doc.getString("username"));
                            itemList.add(profile);
                        }
                    }
                }
                adapter.notifyDataSetChanged();
            }).addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    private void loadImages() {
        db.collection("events").get().addOnSuccessListener(snapshots -> {
            for (QueryDocumentSnapshot doc : snapshots) {
                String eventId = doc.getId();
                String eventName = doc.getString("name");
                // Check if it has a poster field
                if (doc.contains("poster") && doc.get("poster") != null) {
                    itemList.add(new ImageItem(eventId, eventName != null ? eventName : "Unnamed Event"));
                }
            }
            adapter.notifyDataSetChanged();
        });
    }

    private void loadNotifications() {
        db.collection("notifications").get().addOnSuccessListener(snapshots -> {
            for (QueryDocumentSnapshot doc : snapshots) {
                Object listObj = doc.get("notificationList");
                if (listObj instanceof List) {
                    List<Map<String, Object>> list = (List<Map<String, Object>>) listObj;
                    for (Map<String, Object> map : list) {
                        NotificationModel n = new NotificationModel();
                        n.setMessage(String.valueOf(map.get("message")));
                        n.setSenderAccountID(String.valueOf(map.get("senderAccountID")));
                        n.setReceiverAccountID(String.valueOf(map.get("receiverAccountID")));
                        n.setTimestamp(String.valueOf(map.get("timestamp")));
                        itemList.add(n);
                    }
                }
            }
            adapter.notifyDataSetChanged();
        });
    }

    private void deleteItem(Object item) {
        if (item instanceof EventModel) {
            db.collection("events").document(((EventModel) item).getId())
                    .delete().addOnSuccessListener(v -> {
                        Toast.makeText(this, "Event deleted", Toast.LENGTH_SHORT).show();
                        loadData();
                    });
        } else if (item instanceof ProfileModel) {
            db.collection("accounts").document(((ProfileModel) item).getAccountID())
                    .delete().addOnSuccessListener(v -> {
                        Toast.makeText(this, "Profile deleted", Toast.LENGTH_SHORT).show();
                        loadData();
                    });
        } else if (item instanceof ImageItem) {
            db.collection("events").document(((ImageItem) item).eventId)
                    .update("poster", null).addOnSuccessListener(v -> {
                        Toast.makeText(this, "Image removed", Toast.LENGTH_SHORT).show();
                        loadData();
                    });
        }
    }

    private static class ImageItem {
        String eventId, eventName;
        ImageItem(String id, String name) { this.eventId = id; this.eventName = name; }
    }

    private class AdminAdapter extends RecyclerView.Adapter<AdminAdapter.ViewHolder> {
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Object item = itemList.get(position);
            holder.btnDelete.setVisibility(View.VISIBLE);

            if (item instanceof EventModel) {
                EventModel e = (EventModel) item;
                holder.title.setText(e.getName() != null ? e.getName() : "Unnamed Event");
                holder.subtitle.setText("Event ID: " + e.getId());
            } else if (item instanceof ProfileModel) {
                ProfileModel p = (ProfileModel) item;
                holder.title.setText(p.getUsername() != null ? p.getUsername() : "Unnamed Profile");
                holder.subtitle.setText("User ID: " + p.getAccountID());
            } else if (item instanceof ImageItem) {
                ImageItem img = (ImageItem) item;
                holder.title.setText("Poster: " + img.eventName);
                holder.subtitle.setText("Click delete to clear field");
            } else if (item instanceof NotificationModel) {
                NotificationModel n = (NotificationModel) item;
                holder.title.setText(n.getMessage());
                holder.subtitle.setText("From: " + n.getSenderAccountID());
                holder.btnDelete.setVisibility(View.GONE);
            }

            holder.btnDelete.setOnClickListener(v -> deleteItem(item));
        }

        @Override
        public int getItemCount() { return itemList.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView title, subtitle;
            Button btnDelete;
            ViewHolder(View v) {
                super(v);
                title = v.findViewById(R.id.tvAdminItemTitle);
                subtitle = v.findViewById(R.id.tvAdminItemSub);
                btnDelete = v.findViewById(R.id.btnAdminDelete);
            }
        }
    }
}