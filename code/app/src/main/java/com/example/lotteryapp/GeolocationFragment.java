package com.example.lotteryapp;

import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Shows the organizer map view for entrant join locations.
 */
public class GeolocationFragment extends Fragment {

    private final List<String> availableLocations = new ArrayList<>();
    private final List<String> filteredLocations = new ArrayList<>();
    private final List<String> addedLocations = new ArrayList<>();
    
    private RecyclerView.Adapter searchAdapter;
    private RecyclerView.Adapter addedAdapter;
    private TextView tvAddedHeader;
    private Geocoder geocoder;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load existing locations from Activity
        if (getActivity() instanceof CreateEventActivity) {
            addedLocations.addAll(((CreateEventActivity) getActivity()).getAddedLocations());
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_geolocation, container, false);

        geocoder = new Geocoder(getContext(), Locale.getDefault());

        SwitchCompat switchGeolocation = view.findViewById(R.id.switchGeolocation);
        EditText etSearch = view.findViewById(R.id.etSearchLocation);
        RecyclerView rvSearch = view.findViewById(R.id.rvLocations);
        RecyclerView rvAdded = view.findViewById(R.id.rvAddedLocations);
        tvAddedHeader = view.findViewById(R.id.tvAddedHeader);
        Button btnClose = view.findViewById(R.id.btnCloseGeolocation);
        Button btnSave = view.findViewById(R.id.btnSaveGeolocation);

        // Load existing toggle state from Activity
        if (getActivity() instanceof CreateEventActivity) {
            switchGeolocation.setChecked(((CreateEventActivity) getActivity()).isGeolocationVerification());
        }

        // Search Results Setup
        rvSearch.setLayoutManager(new LinearLayoutManager(getContext()));
        searchAdapter = new RecyclerView.Adapter() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
                return new RecyclerView.ViewHolder(v) {};
            }
            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                String location = filteredLocations.get(position);
                ((TextView) holder.itemView.findViewById(android.R.id.text1)).setText(location);
                holder.itemView.setOnClickListener(v -> {
                    if (!addedLocations.contains(location)) {
                        addedLocations.add(location);
                        updateAddedList();
                    } else {
                        Toast.makeText(getContext(), location + " already added", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            @Override
            public int getItemCount() { return filteredLocations.size(); }
        };
        rvSearch.setAdapter(searchAdapter);

        // Added Locations Setup
        rvAdded.setLayoutManager(new LinearLayoutManager(getContext()));
        addedAdapter = new RecyclerView.Adapter() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
                return new RecyclerView.ViewHolder(v) {};
            }
            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                String location = addedLocations.get(position);
                TextView tv = holder.itemView.findViewById(android.R.id.text1);
                tv.setText(location);
                tv.setTextColor(0xFF6B5FA6); // Purple color for added items
                holder.itemView.setOnClickListener(v -> {
                    addedLocations.remove(location);
                    updateAddedList();
                });
            }
            @Override
            public int getItemCount() { return addedLocations.size(); }
        };
        rvAdded.setAdapter(addedAdapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchLocation(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnClose.setOnClickListener(v -> closeFragment());
        btnSave.setOnClickListener(v -> {
            // Sync changes back to Activity
            if (getActivity() instanceof CreateEventActivity) {
                CreateEventActivity activity = (CreateEventActivity) getActivity();
                activity.getAddedLocations().clear();
                activity.getAddedLocations().addAll(addedLocations);
                activity.setGeolocationVerification(switchGeolocation.isChecked());
            }
            Toast.makeText(getContext(), "Saved Geolocation settings", Toast.LENGTH_SHORT).show();
            closeFragment();
        });

        updateAddedList(); // Update UI with loaded data
        return view;
    }

    /**
     * Performs an asynchronous search for locations matching the given query using Geocoder.
     * @param query The search string (minimum 3 characters).
     */
    private void searchLocation(String query) {
        if (query.length() < 3) {
            filteredLocations.clear();
            searchAdapter.notifyDataSetChanged();
            return;
        }

        executorService.execute(() -> {
            try {
                List<Address> addresses = geocoder.getFromLocationName(query, 5);
                mainHandler.post(() -> {
                    filteredLocations.clear();
                    if (addresses != null) {
                        for (Address address : addresses) {
                            String addressLine = address.getAddressLine(0);
                            if (addressLine != null) {
                                filteredLocations.add(addressLine);
                            }
                        }
                    }
                    searchAdapter.notifyDataSetChanged();
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Refreshes the display list of added locations and updates the header count.
     */
    private void updateAddedList() {
        if (addedAdapter != null) addedAdapter.notifyDataSetChanged();
        if (tvAddedHeader != null) tvAddedHeader.setText("Added Locations (" + addedLocations.size() + ")");
    }

    /**
     * Removes the fragment from the parent Activity's fragment manager and pops the back stack.
     */
    private void closeFragment() {
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().beginTransaction()
                    .remove(this)
                    .commit();
            getActivity().getSupportFragmentManager().popBackStack();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}
