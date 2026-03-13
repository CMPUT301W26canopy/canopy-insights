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
import com.example.lotteryapp.NotificationModel;
import com.example.lotteryapp.ProfileModel;
import com.example.lotteryapp.databinding.FragmentSignUpBinding;

import com.example.lotteryapp.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
                .whereEqualTo("username", username)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!isAdded() || binding == null) return;

                    if (!queryDocumentSnapshots.isEmpty()) {
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
        String timestamp = new SimpleDateFormat("MMddHHmmss", Locale.getDefault()).format(new Date());
        profile.setAccountID(timestamp);

        FirestoreHelper.getDb().collection("accounts")
                .whereEqualTo("username", "System")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!isAdded() || binding == null) return;

                    String systemAccountID;
                    if (!queryDocumentSnapshots.isEmpty()) {
                        systemAccountID = queryDocumentSnapshots.getDocuments().get(0).getId();
                    } else {
                        // Fallback if System user doesn't exist yet
                        systemAccountID = "SYSTEM_DEFAULT";
                    }

                    NotificationModel notification = new NotificationModel();
                    notification.setSenderAccountID(systemAccountID);
                    notification.setReceiverAccountID(profile.getAccountID());
                    notification.setMessage("Welcome to Lottery App!");
                    notification.setTimestamp(timestamp);

                    saveUserAndNotification(profile, notification);
                })
                .addOnFailureListener(e -> {
                    // If query fails, proceed with a default sender ID
                    NotificationModel notification = new NotificationModel();
                    notification.setSenderAccountID("SYSTEM_DEFAULT");
                    notification.setReceiverAccountID(profile.getAccountID());
                    notification.setMessage("Welcome to Lottery App!");
                    notification.setTimestamp(timestamp);

                    saveUserAndNotification(profile, notification);
                });
    }

    private void saveUserAndNotification(ProfileModel profile, NotificationModel notification) {
        FirestoreHelper.getDb().collection("accounts")
                .document(profile.getAccountID())
                .set(profile)
                .addOnSuccessListener(aVoid -> {
                    // Create a list and add the notification to it
                    List<NotificationModel> notificationList = new ArrayList<>();
                    notificationList.add(notification);

                    // Create a data map to hold the list
                    Map<String, Object> notifData = new HashMap<>();
                    notifData.put("notificationList", notificationList);

                    // Save to 'notifications' collection, document identified by accountID
                    FirestoreHelper.getDb().collection("notifications")
                            .document(profile.getAccountID())
                            .set(notifData)
                            .addOnSuccessListener(aVoidNotif -> {
                                if (!isAdded() || binding == null) return;
                                binding.loading.setVisibility(View.GONE);
                                Toast.makeText(getContext(), "Account created successfully!", Toast.LENGTH_SHORT).show();
                                navigateToProfile(profile.getAccountID());
                            })
                            .addOnFailureListener(e -> {
                                if (!isAdded() || binding == null) return;
                                binding.loading.setVisibility(View.GONE);
                                Toast.makeText(getContext(), "Failed to save welcome notification", Toast.LENGTH_SHORT).show();
                                // Still navigate even if notification fails
                                navigateToProfile(profile.getAccountID());
                            });
                })
                .addOnFailureListener(e -> {
                    if (!isAdded() || binding == null) return;
                    binding.loading.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Error creating profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void navigateToProfile(String accountID) {
        Context context = getContext();
        if (context != null) {
            Intent intent = new Intent(context, com.example.lotteryapp.ProfileActivity.class);
            intent.putExtra("accountID", accountID);
            startActivity(intent);
            if (getActivity() != null) getActivity().finish();
        }
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
