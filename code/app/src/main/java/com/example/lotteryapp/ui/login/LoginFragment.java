package com.example.lotteryapp.ui.login;

import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.lotteryapp.DeviceData;
import com.example.lotteryapp.databinding.FragmentLoginBinding;

import com.example.lotteryapp.R;

public class LoginFragment extends Fragment {

    private LoginViewModel loginViewModel;
    private FragmentLoginBinding binding;
    private DeviceData deviceData;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loginViewModel = new ViewModelProvider(this, new LoginViewModelFactory())
                .get(LoginViewModel.class);

        deviceData = DeviceData.getInstance(getContext());

        final EditText usernameEditText = binding.usernameProvided;
        final EditText passwordEditText = binding.password;
        final Button loginButton = binding.login;
        final ProgressBar loadingProgressBar = binding.loading;

        loginViewModel.getLoginFormState().observe(getViewLifecycleOwner(), new Observer<LoginFormState>() {
            @Override
            public void onChanged(@Nullable LoginFormState loginFormState) {
                if (loginFormState == null) {
                    return;
                }
                loginButton.setEnabled(loginFormState.isDataValid());
                if (loginFormState.getUsernameError() != null) {
                    usernameEditText.setError(getString(loginFormState.getUsernameError()));
                }
                if (loginFormState.getPasswordError() != null) {
                    passwordEditText.setError(getString(loginFormState.getPasswordError()));
                }
            }
        });

        loginViewModel.getLoginResult().observe(getViewLifecycleOwner(), new Observer<LoginResult>() {
            @Override
            public void onChanged(@Nullable LoginResult loginResult) {
                if (loginResult == null) {
                    return;
                }
                loadingProgressBar.setVisibility(View.GONE);
                if (loginResult.getError() != null) {
                    showLoginFailed(loginResult.getError());
                }
                if (loginResult.getSuccess() != null) {
                    updateUiWithUser(loginResult.getSuccess());
                }
            }
        });

        TextWatcher afterTextChangedListener = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // ignore
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // ignore
            }

            @Override
            public void afterTextChanged(Editable s) {
                loginViewModel.loginDataChanged(usernameEditText.getText().toString(),
                        passwordEditText.getText().toString());
            }
        };
        usernameEditText.addTextChangedListener(afterTextChangedListener);
        passwordEditText.addTextChangedListener(afterTextChangedListener);
        passwordEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    loginViewModel.login(usernameEditText.getText().toString(),
                            passwordEditText.getText().toString());
                }
                return false;
            }
        });

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkLoginValid();
            }
        });
    }


    // Used Gemini to help with this function 02-13-26, 'how can I use checkLoginValic to see if the person entered the right credentials ?'
    private void checkLoginValid() {
        if (binding == null) return;

        String inputUsername = binding.usernameProvided.getText().toString().trim();
        String inputPassword = binding.password.getText().toString().trim();

        if (inputUsername.isEmpty() || inputPassword.isEmpty()) {
            Toast.makeText(getContext(), "Please enter both username and password", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.loading.setVisibility(View.VISIBLE);

        // Query Firestore to find a user with the matching username
        com.example.lotteryapp.FirestoreHelper.getDb().collection("accounts")
                .whereEqualTo("username", inputUsername)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (binding == null) return;
                    binding.loading.setVisibility(View.GONE);

                    if (!queryDocumentSnapshots.isEmpty()) {
                        // We found at least one document with this username
                        com.google.firebase.firestore.DocumentSnapshot userDoc = queryDocumentSnapshots.getDocuments().get(0);
                        com.example.lotteryapp.ProfileModel userProfile = userDoc.toObject(com.example.lotteryapp.ProfileModel.class);

                        if (userProfile != null && inputPassword.equals(userProfile.getPassword())) {
                            // Password matches
                            
                            // Create session
                            deviceData.createLoginSession(userProfile.getAccountID(), userProfile.getUsername());

                            // Navigate to profile activity
                            android.content.Intent intent = new android.content.Intent(getActivity(), com.example.lotteryapp.ProfileActivity.class);
                            intent.putExtra("accountID", userProfile.getAccountID());
                            startActivity(intent);
                            if (getActivity() != null) getActivity().finish();

                        } else {
                            // Password does not match
                            Toast.makeText(getContext(), "Incorrect password", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // No user found with that username
                        Toast.makeText(getContext(), "Username not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (binding == null) return;
                    binding.loading.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Login Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateUiWithUser(LoggedInUserView model) {
        // No toast yet
    }

    private void showLoginFailed(@StringRes Integer errorString) {
        if (getContext() != null && getContext().getApplicationContext() != null) {
            Toast.makeText(
                    getContext().getApplicationContext(),
                    errorString,
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
