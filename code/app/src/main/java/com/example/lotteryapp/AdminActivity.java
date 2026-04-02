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

public class AdminActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private AdminAdapter adapter;
    private List<Object> itemList = new ArrayList<>();
    private String currentTab = "Events";
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        db = FirebaseFirestore.getInstance();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.adminRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminAdapter();
        recyclerView.setAdapter(adapter);

        TabLayout tabLayout = findViewById(R.id.adminTabLayout);
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

        String collectionPath = currentTab.equalsIgnoreCase("Events") ? "events" : 
                              currentTab.equalsIgnoreCase("Profiles") ? "profiles" : "events";

        if (currentTab.equalsIgnoreCase("Images")) {
            loadImages();
            return;
        }

        db.collection(collectionPath).get().addOnSuccessListener(queryDocumentSnapshots -> {
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                if (currentTab.equalsIgnoreCase("Events")) {
                    EventModel event = null;
                    try {
                        event = doc.toObject(EventModel.class);
                    } catch (Exception e) {
                        event = new EventModel();
                        event.setName(doc.getString("name"));
                        event.setWaitingList(new ArrayList<>());
                    }
                    if (event != null) {
                        event.setId(doc.getId());
                        itemList.add(event);
                    }
                } else if (currentTab.equalsIgnoreCase("Profiles")) {
                    ProfileModel profile = null;
                    try {
                        profile = doc.toObject(ProfileModel.class);
                    } catch (Exception e) {
                        profile = new ProfileModel();
                        profile.setName(doc.getString("name"));
                        profile.setAccountID(doc.getId());
                    }
                    if (profile != null) {
                        itemList.add(profile);
                    }
                }
            }
            adapter.notifyDataSetChanged();
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Load failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void loadImages() {
        db.collection("events").get().addOnSuccessListener(snapshots -> {
            for (QueryDocumentSnapshot doc : snapshots) {
                EventModel event = null;
                try {
                    event = doc.toObject(EventModel.class);
                } catch (Exception e) {
                    event = new EventModel();
                    event.setName(doc.getString("name"));
                }
                if (event != null) {
                    event.setId(doc.getId());
                    itemList.add(event);
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
            db.collection("profiles").document(((ProfileModel) item).getAccountID())
                    .delete().addOnSuccessListener(v -> {
                        Toast.makeText(this, "Profile deleted", Toast.LENGTH_SHORT).show();
                        loadData();
                    });
        }
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
            if (item instanceof EventModel) {
                EventModel e = (EventModel) item;
                holder.title.setText(e.getName() != null ? e.getName() : "Unnamed Event");
                holder.subtitle.setText("ID: " + e.getId());
            } else if (item instanceof ProfileModel) {
                ProfileModel p = (ProfileModel) item;
                holder.title.setText(p.getName() != null ? p.getName() : p.getUsername() != null ? p.getUsername() : "Unnamed Profile");
                holder.subtitle.setText("ID: " + p.getAccountID());
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