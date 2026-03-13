package com.example.lotteryapp.ui.login;

import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;

import android.content.Context;
import android.content.Intent;
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

import com.example.lotteryapp.FirestoreHelper;
import com.example.lotteryapp.ProfileModel;
import com.example.lotteryapp.databinding.FragmentSignUpBinding;

import com.example.lotteryapp.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SignUpFragment extends Fragment {

    private LoginViewModel loginViewModel;
    private FragmentSignUpBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentSignUpBinding.inflate(inflater, container, false);
        return binding.getRoot();

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loginViewModel = new ViewModelProvider(this, new LoginViewModelFactory())
                .get(LoginViewModel.class);

        final EditText usernameEditText = binding.usernameProvided;
        final EditText passwordEditText = binding.password;
        final Button registerButton = binding.register;
        final ProgressBar loadingProgressBar = binding.loading;

        loginViewModel.getLoginFormState().observe(getViewLifecycleOwner(), new Observer<LoginFormState>() {
            @Override
            public void onChanged(@Nullable LoginFormState loginFormState) {
                if (loginFormState == null) {
                    return;
                }
                registerButton.setEnabled(loginFormState.isDataValid());
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
                    addToFireStore();
                }
                return false;
            }
        });

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addToFireStore();
            }
        });
    }

    private void addToFireStore() {
        if (binding == null) return;

        String username = binding.usernameProvided.getText().toString().trim();
        String password = binding.password.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(getContext(), "Username or password cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.loading.setVisibility(View.VISIBLE);

        // Safely check if username already exists
        FirestoreHelper.getDb().collection("accounts")
                .document(username)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!isAdded() || binding == null) return;

                    if (documentSnapshot.exists()) {
                        binding.loading.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Username is already taken!", Toast.LENGTH_SHORT).show();
                    } else {
                        performRegistration(username, password);
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isAdded() || binding == null) return;
                    binding.loading.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Database error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void performRegistration(String username, String password) {
        ProfileModel profile = new ProfileModel();
        profile.setUsername(username);
        profile.setPassword(password);
        profile.setAccountID(username);
        
        FirestoreHelper.getDb().collection("accounts")
                .document(username)
                .set(profile)
                .addOnSuccessListener(aVoid -> {
                    if (!isAdded() || binding == null) return;
                    
                    binding.loading.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Account created successfully!", Toast.LENGTH_SHORT).show();

                    // Safely start ProfileActivity
                    Context context = getContext();
                    if (context != null) {
                        Intent intent = new Intent(context, com.example.lotteryapp.ProfileActivity.class);
                        intent.putExtra("accountID", username);
                        startActivity(intent);
                        if (getActivity() != null) getActivity().finish();
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isAdded() || binding == null) return;
                    binding.loading.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Error creating profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateUiWithUser(LoggedInUserView model) {
        if (!isAdded()) return;
        Context context = getContext();
        if (context == null) return;

        String welcome = context.getString(R.string.welcome) + model.getDisplayName();
        Toast.makeText(context.getApplicationContext(), welcome, Toast.LENGTH_LONG).show();
    }

    private void showLoginFailed(@StringRes Integer errorString) {
        if (!isAdded()) return;
        Context context = getContext();
        if (context == null) return;

        Toast.makeText(context.getApplicationContext(), errorString, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
