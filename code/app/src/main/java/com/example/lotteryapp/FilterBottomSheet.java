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

    public interface FilterCallback {
        void onApply(double minPrice, double maxPrice, String month,
                     String year, String ageGroup, String country);
        void onReset();
    }

    private FilterCallback callback;
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
                String minStr = etMinPrice.getText().toString();
                String maxStr = etMaxPrice.getText().toString();
                double min = minStr.isEmpty() ? 0 : Double.parseDouble(minStr);
                double max = maxStr.isEmpty() ? 9999 : Double.parseDouble(maxStr);
                callback.onApply(
                        min, max,
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