package com.example.testoauth.ui.login;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.testoauth.R;
import com.example.testoauth.networking.RetrofitAdapter;
import com.example.testoauth.networking.accessToken.OAuthAccessTokenResponse;
import com.example.testoauth.networking.accessToken.OAuthAccessTokenService;
import com.example.testoauth.networking.profile.OAuthProfileService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.truecaller.android.sdk.oAuth.TcOAuthData;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SignedInActivity extends AppCompatActivity {

    private TextView textViewData, textViewAccessToken, textViewProfile;
    private Button accessTokenBtn, profileBtn;
    private TcOAuthData oAuthData;
    private String codeVerifier;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signed_in);

        textViewData = findViewById(R.id.textViewData);
        textViewAccessToken = findViewById(R.id.textViewAccessToken);
        textViewProfile = findViewById(R.id.textViewProfile);
        accessTokenBtn = findViewById(R.id.accessTokenBtn);
        profileBtn = findViewById(R.id.profileBtn);

        oAuthData = getIntent().getParcelableExtra("data");
        codeVerifier = getIntent().getStringExtra("cv");

        if (oAuthData != null) {
            textViewData.setText("Auth Code: " + oAuthData.getAuthorizationCode() + "\nState: " + oAuthData.getState());
        }

        accessTokenBtn.setOnClickListener(v -> fetchAccessToken());
        profileBtn.setOnClickListener(v -> fetchProfile());
    }

    private void fetchAccessToken() {
        OAuthAccessTokenService service = RetrofitAdapter.createService(OAuthAccessTokenService.BaseUrl.FETCH_ACCESS_TOKEN_BASE_URL, OAuthAccessTokenService.class);
        String clientId = getString(R.string.clientId);
        service.fetchAccessToken("authorization_code", clientId, oAuthData.getAuthorizationCode(), codeVerifier)
                .enqueue(new Callback<OAuthAccessTokenResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<OAuthAccessTokenResponse> call, @NonNull Response<OAuthAccessTokenResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            textViewAccessToken.setText("Access Token: " + response.body().getAccessToken());
                            profileBtn.setEnabled(true);
                        } else {
                            Toast.makeText(SignedInActivity.this, "Failed to fetch token", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<OAuthAccessTokenResponse> call, @NonNull Throwable t) {
                        Toast.makeText(SignedInActivity.this, t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void fetchProfile() {
        String token = "Bearer " + textViewAccessToken.getText().toString().replace("Access Token: ", "");
        OAuthProfileService service = RetrofitAdapter.createService(OAuthProfileService.BaseUrl.FETCH_PROFILE_BASE_URL, OAuthProfileService.class);
        service.fetchProfile(token).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        String json = response.body().string();
                        Gson gson = new GsonBuilder().setPrettyPrinting().create();
                        Object obj = gson.fromJson(json, Object.class);
                        textViewProfile.setText(gson.toJson(obj));
                    } else {
                        Toast.makeText(SignedInActivity.this, "Failed to fetch profile", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(SignedInActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                Toast.makeText(SignedInActivity.this, t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
