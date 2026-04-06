package com.example.lotteryapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * A fragment that allows the user to input registration dates and a waitlist limit for an event.
 * This fragment interacts with {@link CreateEventActivity} to save the provided information.
 */
public class RegistrationPeriodFragment extends Fragment {

    private EditText etRegStartDate, etRegEndDate, etWaitlistLimit;
    private Button btnCancelReg, btnSaveReg;

    /**
     * Configures the UI for the registration period settings.
     *
     * @param inflater           The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container          If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     * @return The View for the fragment's UI.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_registration_period, container, false);

        etRegStartDate = view.findViewById(R.id.etRegStartDate);
        etRegEndDate = view.findViewById(R.id.etRegEndDate);
        etWaitlistLimit = view.findViewById(R.id.etWaitlistLimit);
        btnCancelReg = view.findViewById(R.id.btnCancelReg);
        btnSaveReg = view.findViewById(R.id.btnSaveReg);

        btnCancelReg.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        btnSaveReg.setOnClickListener(v -> {
            String startDate = etRegStartDate.getText().toString().trim();
            String endDate = etRegEndDate.getText().toString().trim();
            String waitlistLimitStr = etWaitlistLimit.getText().toString().trim();
            Integer waitlistLimit = null;
            if (!waitlistLimitStr.isEmpty()) {
                try {
                    waitlistLimit = Integer.parseInt(waitlistLimitStr);
                } catch (NumberFormatException ignored) {}
            }

            if (getActivity() instanceof CreateEventActivity) {
                ((CreateEventActivity) getActivity()).setRegistrationPeriod(startDate, endDate, waitlistLimit);
            }
            getParentFragmentManager().popBackStack();
        });

        return view;
    }
}
