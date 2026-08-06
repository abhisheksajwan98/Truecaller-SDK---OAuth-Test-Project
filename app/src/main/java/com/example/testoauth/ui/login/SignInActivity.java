package com.example.testoauth.ui.login;

import static com.truecaller.android.sdk.oAuth.TcSdkOptions.CONSENT_MODE_BOTTOMSHEET;
import static com.truecaller.android.sdk.oAuth.TcSdkOptions.CONSENT_MODE_POPUP;
import static com.truecaller.android.sdk.oAuth.TcSdkOptions.DISMISS_OPTION_CROSS_BUTTON;
import static com.truecaller.android.sdk.oAuth.TcSdkOptions.DISMISS_OPTION_SECONDARY_CTA_BORDER;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.SwitchCompat;

import com.example.testoauth.R;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.truecaller.android.sdk.common.TrueException;
import com.truecaller.android.sdk.common.VerificationCallback;
import com.truecaller.android.sdk.common.VerificationDataBundle;
import com.truecaller.android.sdk.common.callVerification.RequestPermissionHandler;
import com.truecaller.android.sdk.common.models.TrueProfile;
import com.truecaller.android.sdk.oAuth.CodeVerifierUtil;
import com.truecaller.android.sdk.oAuth.OAuthThemeOptions;
import com.truecaller.android.sdk.oAuth.TcOAuthCallback;
import com.truecaller.android.sdk.oAuth.TcOAuthData;
import com.truecaller.android.sdk.oAuth.TcOAuthError;
import com.truecaller.android.sdk.oAuth.TcSdk;
import com.truecaller.android.sdk.oAuth.TcSdkOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class SignInActivity extends AppCompatActivity {
    private static final String TAG = "SignInActivity";

    private static final int LANDING_LAYOUT = 1;
    private static final int PROFILE_LAYOUT = 2;
    private static final int LOADER_LAYOUT = 3;
    private static final int FORM_LAYOUT = 4;
    private static final int SETTINGS_LAYOUT = 5;

    private RadioGroup additionalFooterSelector;
    private int verificationCallbackType;
    private Spinner ctaTextSpinner, headingSpinner;
    private Spinner colorSpinner, colorTextSpinner, dismissOptionsSpinner;
    private AppCompatTextView timerTextViewMissedCall;
    private ProgressBar progressBar;
    private MaterialCheckBox phoneCheckbox, profileCheckbox, openIdCheckbox, offlineAccessCheckbox, emailCheckbox, addressCheckbox;
    private CountDownTimer timer;
    private String state;
    private RequestPermissionHandler permissionHandler;
    private String codeVerifier;

    private final TcOAuthCallback sdkCallback = new TcOAuthCallback() {
        @Override
        public void onSdkReady() {
            showLayout(LANDING_LAYOUT);
            try {
                if (TcSdk.getInstance().isOAuthFlowUsable()) {
                    EditText localeEt = findViewById(R.id.localeEt);
                    String locale = null;
                    if (!TextUtils.isEmpty(localeEt.getText())) {
                        locale = localeEt.getText().toString();
                    }
                    if (locale != null && !locale.isEmpty()) {
                        TcSdk.getInstance().setLocale(new Locale(locale));
                    }
                    codeVerifier = CodeVerifierUtil.Companion.generateRandomCodeVerifier();
                    String codeChallenge = CodeVerifierUtil.Companion.getCodeChallenge(codeVerifier);
                    if (codeChallenge != null) {
                        TcSdk.getInstance().setCodeChallenge(codeChallenge);
                    } else {
                        Toast.makeText(SignInActivity.this, "code challenge is required", Toast.LENGTH_SHORT).show();
                    }
                    state = UUID.randomUUID().toString();
                    TcSdk.getInstance().setOAuthState(state);
                    TcSdk.getInstance().setOAuthScopes(getRequestedScopes());
                    TcSdk.getInstance().setTheme(((SwitchCompat) findViewById(R.id.darkModeOptions)).isChecked() ? OAuthThemeOptions.DARK : OAuthThemeOptions.LIGHT);
                    TcSdk.getInstance().getAuthorizationCode(SignInActivity.this, launcher);
                } else {
                    Toast.makeText(SignInActivity.this, "OAuth flow not usable", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(SignInActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onSuccess(@NonNull final TcOAuthData oAuthData) {
            Log.i(TAG, "\ncode: " + oAuthData.getAuthorizationCode() + "\nverifier: " + codeVerifier);
            Toast.makeText(SignInActivity.this, "onSuccess : state = " + oAuthData.getState(), Toast.LENGTH_SHORT).show();
            showLayout(LANDING_LAYOUT);
            Intent intent = new Intent(SignInActivity.this, SignedInActivity.class);
            intent.putExtra("data", oAuthData);
            intent.putExtra("state", state);
            intent.putExtra("cv", codeVerifier);
            startActivity(intent);
        }

        @Override
        public void onFailure(@NonNull final TcOAuthError oAuthError) {
            Toast.makeText(SignInActivity.this, "onFailure: " + oAuthError.getErrorCode() + ": " + oAuthError.getErrorMessage(), Toast.LENGTH_SHORT).show();
            showLayout(LANDING_LAYOUT);
        }

        @Override
        public void onVerificationRequired(@Nullable final TcOAuthError tcOAuthError) {
            if (tcOAuthError != null) {
                Toast.makeText(SignInActivity.this, "Verification Required : " + tcOAuthError.getErrorMessage(), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(SignInActivity.this, "Verification Required", Toast.LENGTH_SHORT).show();
            }
            showLayout(FORM_LAYOUT);
            findViewById(R.id.btnProceed).setOnClickListener(proceedClickListener);
        }
    };

    private final VerificationCallback apiCallback = new VerificationCallback() {
        @Override
        public void onRequestSuccess(final int requestCode, @Nullable VerificationDataBundle bundle) {
            if (requestCode == VerificationCallback.TYPE_MISSED_CALL_INITIATED) {
                verificationCallbackType = VerificationCallback.TYPE_MISSED_CALL_INITIATED;
                String ttl = bundle.getString(VerificationDataBundle.KEY_TTL);
                if (ttl != null) {
                    Toast.makeText(SignInActivity.this, "Missed call initiated with TTL : " + ttl, Toast.LENGTH_SHORT).show();
                    showCountDownTimer(Double.parseDouble(ttl) * 1000);
                }
                showLoader("Waiting for call");
            } else if (requestCode == VerificationCallback.TYPE_MISSED_CALL_RECEIVED) {
                Toast.makeText(SignInActivity.this, "Missed call received", Toast.LENGTH_SHORT).show();
                showLayout(PROFILE_LAYOUT);
                findViewById(R.id.btnVerify).setOnClickListener(verifyClickListener);
            } else if (requestCode == VerificationCallback.TYPE_IM_OTP_INITIATED) {
                verificationCallbackType = VerificationCallback.TYPE_IM_OTP_INITIATED;
                String ttl = bundle.getString(VerificationDataBundle.KEY_TTL);
                if (ttl != null) {
                    Toast.makeText(SignInActivity.this, "IM OTP initiated with TTL : " + bundle.getString(VerificationDataBundle.KEY_TTL), Toast.LENGTH_SHORT).show();
                    showCountDownTimer(Double.parseDouble(ttl) * 1000);
                }
                showLayout(PROFILE_LAYOUT);
                findViewById(R.id.btnVerify).setOnClickListener(verifyClickListener);
            } else if (requestCode == VerificationCallback.TYPE_IM_OTP_RECEIVED) {
                Toast.makeText(SignInActivity.this.getApplicationContext(), "IM OTP received", Toast.LENGTH_SHORT).show();
                fillOtp(bundle.getString(VerificationDataBundle.KEY_OTP));
            } else if (requestCode == VerificationCallback.TYPE_PROFILE_VERIFIED_BEFORE) {
                Toast.makeText(SignInActivity.this, "Profile verified for your app before: " + bundle.getProfile().firstName, Toast.LENGTH_SHORT).show();
                showLayout(LANDING_LAYOUT);
                onSuccessfullManualVerification(bundle.getProfile().firstName);
            } else if (requestCode == VerificationCallback.TYPE_VERIFICATION_COMPLETE) {
                dismissCountDownTimer();
                Toast.makeText(SignInActivity.this, "Success: Verified", Toast.LENGTH_SHORT).show();
                showLayout(LANDING_LAYOUT);
                onSuccessfullManualVerification(((EditText) findViewById(R.id.edtFirstName)).getText().toString());
            }
        }

        @Override
        public void onRequestFailure(final int requestCode, @NonNull final TrueException e) {
            Toast.makeText(SignInActivity.this, "OnFailureApiCallback: " + e.getExceptionType() + "\n" + e.getExceptionMessage(), Toast.LENGTH_SHORT).show();
            showLayout(FORM_LAYOUT);
        }
    };

    private void onSuccessfullManualVerification(String name) {
        Intent intent = new Intent(this, SignedInSuccessfulActivity.class);
        intent.putExtra("name", name);
        startActivity(intent);
    }

    private final View.OnClickListener verifyClickListener = view -> {
        final String firstName = ((EditText) findViewById(R.id.edtFirstName)).getText().toString();
        final String lastName = ((EditText) findViewById(R.id.edtLastName)).getText().toString();
        final TrueProfile profile = new TrueProfile.Builder(firstName, lastName).build();
        if (verificationCallbackType == VerificationCallback.TYPE_IM_OTP_INITIATED) {
            final String otp = ((EditText) findViewById(R.id.edtOtpCode)).getText().toString();
            if (TextUtils.isEmpty(otp)) {
                return;
            }
            showLoader("Verifying profile...");
            TcSdk.getInstance().verifyOtp(profile, otp, apiCallback);
        } else {
            TcSdk.getInstance().verifyMissedCall(profile, apiCallback);
        }
    };

    ActivityResultLauncher<Intent> launcher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> TcSdk.getInstance().onActivityResultObtained(SignInActivity.this, result.getResultCode(), result.getData()));

    private String[] getRequestedScopes() {
        List<String> scopes = new ArrayList<>();
        if (phoneCheckbox.isChecked()) {
            scopes.add(phoneCheckbox.getTag().toString());
        }
        if (profileCheckbox.isChecked()) {
            scopes.add(profileCheckbox.getTag().toString());
        }
        if (openIdCheckbox.isChecked()) {
            scopes.add(openIdCheckbox.getTag().toString());
        }
        if (offlineAccessCheckbox.isChecked()) {
            scopes.add(offlineAccessCheckbox.getTag().toString());
        }
        if (emailCheckbox.isChecked()) {
            scopes.add(emailCheckbox.getTag().toString());
        }
        if (addressCheckbox.isChecked()) {
            scopes.add(addressCheckbox.getTag().toString());
        }
        return scopes.toArray(new String[0]);
    }

    private final View.OnClickListener proceedClickListener = view -> checkAndRequestPermissions();

    @SuppressLint("NewApi")
    private final View.OnClickListener btnGoClickListner = v -> {
        initTruecallerSDK();
        showLoader(getString(R.string.init_sdk_loader_message));
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);
        findViewById(R.id.buttonGo).setOnClickListener(btnGoClickListner);
        additionalFooterSelector = findViewById(R.id.additionalFooters);
        dismissOptionsSpinner = findViewById(R.id.dismiss_options_spinner);
        colorSpinner = findViewById(R.id.color_spinner);
        colorTextSpinner = findViewById(R.id.color_text_spinner);
        ctaTextSpinner = findViewById(R.id.cta_prefix_spinner);
        headingSpinner = findViewById(R.id.heading_spinner);
        phoneCheckbox = findViewById(R.id.phone_scope);
        profileCheckbox = findViewById(R.id.profile_scope);
        openIdCheckbox = findViewById(R.id.openId_scope);
        offlineAccessCheckbox = findViewById(R.id.offline_access_scope);
        emailCheckbox = findViewById(R.id.email_access_scope);
        addressCheckbox = findViewById(R.id.address_access_scope);
        timerTextViewMissedCall = findViewById(R.id.timerTextProgress);
        progressBar = findViewById(R.id.progress_bar);
        setSpinnerAdapters();
        showLayout(SETTINGS_LAYOUT);
    }

    private void setSpinnerAdapters() {
        String[] headingOptions = getResources().getStringArray(R.array.SdkPartnerHeadingOptionsArray);
        String[] headingOptionsFormated = new String[headingOptions.length];
        String appName = getString(R.string.app_name);
        for (int i = 0; i < headingOptionsFormated.length; i++) {
            headingOptionsFormated[i] = String.format(headingOptions[i], appName);
        }
        ArrayAdapter<String> adapterP = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, headingOptionsFormated);
        adapterP.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        headingSpinner.setAdapter(adapterP);

        ArrayAdapter<CharSequence> adapterCP = ArrayAdapter.createFromResource(this, R.array.SdkPartnerCTAOptionsArray, android.R.layout.simple_spinner_item);
        adapterCP.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        ctaTextSpinner.setAdapter(adapterCP);

        ArrayAdapter<CharSequence> adapterColor = ArrayAdapter.createFromResource(this, R.array.SdkPartnerSampleColors, android.R.layout.simple_spinner_item);
        adapterColor.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        ArrayAdapter<CharSequence> adapterDismissOptions = ArrayAdapter.createFromResource(this, R.array.SdkPartnerDismissOptions, android.R.layout.simple_spinner_item);
        adapterDismissOptions.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        dismissOptionsSpinner.setAdapter(adapterDismissOptions);
        colorSpinner.setAdapter(adapterColor);
        colorTextSpinner.setAdapter(adapterColor);
        headingSpinner.setSelection(0);
        ctaTextSpinner.setSelection(0);
        colorSpinner.setSelection(0);
        colorTextSpinner.setSelection(1);
        dismissOptionsSpinner.setSelection(0);
    }

    private void initTruecallerSDK() {
        TcSdk.clear();
        TcSdkOptions.Builder trueScopeBuilder = new TcSdkOptions.Builder(this, sdkCallback)
                .buttonColor(Color.parseColor(colorSpinner.getSelectedItem().toString()))
                .buttonTextColor(Color.parseColor(colorTextSpinner.getSelectedItem().toString()))
                .consentHeadingOption(headingSpinner.getSelectedItemPosition())
                .ctaText(ctaTextSpinner.getSelectedItemPosition())
                .consentMode(((SwitchCompat) findViewById(R.id.popupModeOptions)).isChecked() ? CONSENT_MODE_POPUP : CONSENT_MODE_BOTTOMSHEET)
                .buttonShapeOptions(((SwitchCompat) findViewById(R.id.shapeOptions)).isChecked() ? TcSdkOptions.BUTTON_SHAPE_RECTANGLE : TcSdkOptions.BUTTON_SHAPE_ROUNDED)
                .footerType(additionalFooterSelector.getCheckedRadioButtonId() == -1 ? TcSdkOptions.FOOTER_TYPE_SKIP : resolveAdditionalFooter(additionalFooterSelector.getCheckedRadioButtonId()))
                .sdkOptions(((SwitchCompat) findViewById(R.id.sdkOptions)).isChecked() ? TcSdkOptions.OPTION_VERIFY_ALL_USERS : TcSdkOptions.OPTION_VERIFY_ONLY_TC_USERS);

        if (dismissOptionsSpinner.getSelectedItemPosition() != 0) {
            trueScopeBuilder.dismissOptions(getDismissOptions(dismissOptionsSpinner.getSelectedItemPosition()));
        }
        if (!((SwitchCompat) findViewById(R.id.neverCallEnhancedBtmSheetEnabled)).isChecked()) {
            trueScopeBuilder.setEnhancedBottomSheet(((SwitchCompat) findViewById(R.id.enhancedBtmSheetEnabled)).isChecked());
        }
        TcSdk.initAsync(trueScopeBuilder.build());
    }

    private int getDismissOptions(int selectedItemPosition) {
        if (selectedItemPosition == 1) {
            return DISMISS_OPTION_CROSS_BUTTON;
        }
        return DISMISS_OPTION_SECONDARY_CTA_BORDER;
    }

    private int resolveAdditionalFooter(final int checkedRadioButtonId) {
        if (checkedRadioButtonId == R.id.uan) {
            return TcSdkOptions.FOOTER_TYPE_ANOTHER_MOBILE_NO;
        } else if (checkedRadioButtonId == R.id.uam) {
            return TcSdkOptions.FOOTER_TYPE_ANOTHER_METHOD;
        } else if (checkedRadioButtonId == R.id.edm) {
            return TcSdkOptions.FOOTER_TYPE_MANUALLY;
        } else if (checkedRadioButtonId == R.id.idl) {
            return TcSdkOptions.FOOTER_TYPE_LATER;
        }
        return TcSdkOptions.FOOTER_TYPE_SKIP;
    }

    private void fillOtp(final String otp) {
        ((EditText) findViewById(R.id.edtOtpCode)).setText(otp);
    }

    public void requestVerification() {
        final String phone = ((EditText) findViewById(R.id.edtPhone)).getText().toString().trim();
        if (!TextUtils.isEmpty(phone)) {
            showLoader("Trying to verify your number...");
            try {
                TcSdk.getInstance().requestVerification("IN", phone, apiCallback, this);
            } catch (RuntimeException e) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void checkAndRequestPermissions() {
        permissionHandler = new RequestPermissionHandler(this, new RequestPermissionHandler.Listener() {
            @Override
            public boolean onShowSettingRationale(@NonNull final Set<String> set) {
                return false;
            }

            @Override
            public boolean onShowPermissionRationale(@NonNull final Set<String> set) {
                new AlertDialog.Builder(SignInActivity.this)
                        .setMessage("For verifying your number, we need Calls and Phone permission")
                        .setCancelable(false)
                        .setPositiveButton("OK", (dialogInterface, i) -> permissionHandler.retryRequestDeniedPermission())
                        .setNegativeButton("Cancel", (dialogInterface, i) -> {
                            permissionHandler.cancel();
                            dialogInterface.dismiss();
                        }).show();
                return true;
            }

            @Override
            public void onComplete(@NonNull final Set<String> grantedPermissions, @NonNull final Set<String> deniedPermissions) {
                if (deniedPermissions.isEmpty()) {
                    requestVerification();
                } else {
                    Toast.makeText(SignInActivity.this, "Cannot proceed ahead unless permissions are granted", Toast.LENGTH_SHORT).show();
                }
            }
        });
        permissionHandler.requestPermission();
    }

    public void showLoader(String message) {
        showLayout(LOADER_LAYOUT);
        ((TextView) findViewById(R.id.txtLoader)).setText(message);
    }

    private void showCountDownTimer(Double ttl) {
        if (verificationCallbackType == VerificationCallback.TYPE_MISSED_CALL_INITIATED) {
            timerTextViewMissedCall.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.VISIBLE);
        }
        timer = new CountDownTimer(ttl.longValue(), 1000) {
            @Override
            public void onTick(final long millisUntilFinished) {
                if (verificationCallbackType == VerificationCallback.TYPE_MISSED_CALL_INITIATED) {
                    timerTextViewMissedCall.setPaintFlags(timerTextViewMissedCall.getPaintFlags() & ~Paint.UNDERLINE_TEXT_FLAG);
                    timerTextViewMissedCall.setText(String.format(getString(R.string.retry_timer), millisUntilFinished / 1000));
                }
            }

            @Override
            public void onFinish() {
                if (verificationCallbackType == VerificationCallback.TYPE_MISSED_CALL_INITIATED) {
                    timerTextViewMissedCall.setPaintFlags(timerTextViewMissedCall.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
                    timerTextViewMissedCall.setText(getString(R.string.retry_now));
                    progressBar.setVisibility(View.GONE);
                    timerTextViewMissedCall.setOnClickListener(v -> {
                        showLayout(FORM_LAYOUT);
                    });
                }
            }
        };
        timer.start();
    }

    private void dismissCountDownTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        if (verificationCallbackType == VerificationCallback.TYPE_MISSED_CALL_INITIATED) {
            timerTextViewMissedCall.setVisibility(View.GONE);
        }
    }

    @Override
    public void onBackPressed() {
        if (findViewById(R.id.optionsMenu).getVisibility() != View.VISIBLE) {
            showLayout(SETTINGS_LAYOUT);
        } else {
            super.onBackPressed();
        }
    }

    public void showLayout(int id) {
        if (id == PROFILE_LAYOUT) {
            if (verificationCallbackType == VerificationCallback.TYPE_MISSED_CALL_INITIATED) {
                findViewById(R.id.tvOtp).setVisibility(View.GONE);
                findViewById(R.id.edtOtpCode).setVisibility(View.GONE);
            } else {
                findViewById(R.id.edtOtpCode).setVisibility(View.VISIBLE);
                findViewById(R.id.tvOtp).setVisibility(View.VISIBLE);
            }
        }
        findViewById(R.id.landingLayout).setVisibility(id == LANDING_LAYOUT ? View.VISIBLE : View.GONE);
        findViewById(R.id.profileLayout).setVisibility(id == PROFILE_LAYOUT ? View.VISIBLE : View.GONE);
        findViewById(R.id.loaderLayout).setVisibility(id == LOADER_LAYOUT ? View.VISIBLE : View.GONE);
        findViewById(R.id.formLayout).setVisibility(id == FORM_LAYOUT ? View.VISIBLE : View.GONE);
        findViewById(R.id.optionsMenu).setVisibility(id == SETTINGS_LAYOUT ? View.VISIBLE : View.GONE);
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(findViewById(R.id.landingLayout).getWindowToken(), 0);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dismissCountDownTimer();
        TcSdk.clear();
        permissionHandler = null;
    }
}
