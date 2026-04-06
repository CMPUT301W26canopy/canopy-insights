package com.example.lotteryapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

/**
 * Passes filter values back to MainActivity through FilterCallback
 */
public class FilterBottomSheet extends BottomSheetDialogFragment {

    /**
     * Interface definition for a callback to be invoked when filters are applied or reset.
     */
    public interface FilterCallback {
        /**
         * Called when the user clicks the "Apply" button with selected filter values.
         * @param minPrice The minimum price selected.
         * @param maxPrice The maximum price selected.
         * @param minSpots The minimum number of spots selected.
         * @param maxSpots The maximum number of spots selected.
         * @param month The month selected for filtering (e.g., "Jan", "Any").
         * @param year The year selected for filtering (e.g., "2025", "Any").
         * @param ageGroup The age group category selected.
         * @param country The location/country string entered for filtering.
         */
        void onApply(double minPrice, double maxPrice, int minSpots, int maxSpots,
                     String month, String year, String ageGroup, String country);

        /**
         * Called when the user clicks the "Reset" button to clear all active filters.
         */
        void onReset();
    }

    private FilterCallback callback;

    /**
     * Sets the callback listener to handle filter actions.
     * @param callback The {@link FilterCallback} implementation.
     */
    public void setFilterCallback(FilterCallback callback) {
        this.callback = callback;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_filter, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        EditText etMinPrice = view.findViewById(R.id.etMinPrice);
        EditText etMaxPrice = view.findViewById(R.id.etMaxPrice);
        EditText etMinSpots = view.findViewById(R.id.etMinSpots);
        EditText etMaxSpots = view.findViewById(R.id.etMaxSpots);
        Spinner spinnerMonth = view.findViewById(R.id.spinnerMonth);
        String[] months = {"Any", "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        spinnerMonth.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, months));
        Spinner spinnerYear = view.findViewById(R.id.spinnerYear);
        String[] years = {"Any", "2025", "2026", "2027"};
        spinnerYear.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, years));

        Spinner spinnerAge = view.findViewById(R.id.spinnerAge);
        String[] ageGroups = {"All Age Groups", "Under 18", "18-30", "31-50", "50+"};
        spinnerAge.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, ageGroups));

        AutoCompleteTextView etCountry = view.findViewById(R.id.etCountry);
        String[] locations = {"Edmonton", "Vancouver", "Calgary", "Toronto", "Montreal"};
        etCountry.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, locations));

        // apply filter
        Button btnApply = view.findViewById(R.id.btnApply);
        btnApply.setOnClickListener(v -> {
            if (callback != null) {
                String minPriceStr = etMinPrice.getText().toString();
                String maxPriceStr = etMaxPrice.getText().toString();
                double minPrice = minPriceStr.isEmpty() ? 0 : Double.parseDouble(minPriceStr);
                double maxPrice = maxPriceStr.isEmpty() ? 9999 : Double.parseDouble(maxPriceStr);

                String minSpotsStr = etMinSpots.getText().toString();
                String maxSpotsStr = etMaxSpots.getText().toString();
                int minSpots = minSpotsStr.isEmpty() ? 0 : Integer.parseInt(minSpotsStr);
                int maxSpots = maxSpotsStr.isEmpty() ? 999999 : Integer.parseInt(maxSpotsStr);

                callback.onApply(
                        minPrice, maxPrice, minSpots, maxSpots,
                        spinnerMonth.getSelectedItem().toString(),
                        spinnerYear.getSelectedItem().toString(),
                        spinnerAge.getSelectedItem().toString(),
                        etCountry.getText().toString().trim()
                );
            }
            dismiss();
        });

        // reset filter
        Button btnReset = view.findViewById(R.id.btnReset);
        btnReset.setOnClickListener(v -> {
            if (callback != null) callback.onReset();
            dismiss();
        });
    }
}
