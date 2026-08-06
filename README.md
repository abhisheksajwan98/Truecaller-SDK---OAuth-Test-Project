# Truecaller SDK - OAuth Test Project

This is a sample Android application demonstrating the integration and usage of the **Truecaller SDK** for **OAuth-based authentication**. The project showcases how to implement a seamless login experience using Truecaller, supporting both Truecaller and non-Truecaller users.

## Features

- **Truecaller One-Tap Login**: Seamless authentication for users with the Truecaller app installed.
- **Manual Verification**: Fallback verification methods for users without the Truecaller app:
    - **Missed Call Verification**: Automatic verification via a missed call.
    - **IM OTP Verification**: Verification via an OTP sent through instant messaging.
- **Customizable SDK Options**:
    - **UI Themes**: Support for both Light and Dark modes.
    - **Consent Modes**: Choose between BottomSheet and Popup modes.
    - **Button Customization**: Customize button color, text color, and shape (Rounded/Rectangle).
    - **Footer Options**: Various footer styles for the consent screen.
- **Scope Management**: Request specific user permissions such as Phone, Profile, OpenID, Email, and Address.
- **Advanced Configuration**:
    - Enhanced BottomSheet support.
    - Locale customization.
    - PKCE (Proof Key for Code Exchange) support for secure OAuth flows.

## Getting Started

### Prerequisites

- Android Studio Flamingo or newer.
- A physical Android device or emulator with Google Play Services.
- A Truecaller Partner Account to obtain a `clientId`.

### Configuration

1.  Open the project in Android Studio.
2.  Navigate to `app/src/main/res/values/strings.xml`.
3.  Replace the placeholder value for `clientId` with your actual Truecaller Client ID:
    ```xml
    <string name="clientId">YOUR_CLIENT_ID_HERE</string>
    ```
4.  Ensure your app's package name and SHA-1 fingerprint are registered in the Truecaller Developer Console.

## Project Structure

- **`SignInActivity`**: The main entry point where the SDK is initialized and the authentication flow starts. It includes settings to customize the SDK's behavior and UI.
- **`SignedInActivity`**: Handles the result of a successful OAuth authorization, showing the received authorization code and state.
- **`SignedInSuccessfulActivity`**: A simple screen displayed after a successful manual verification flow.
- **`networking`**: Contains Retrofit services and data models for interacting with backend services (if applicable).

## Permissions

The app requires the following permissions for manual verification flows:
- `INTERNET`
- `READ_PHONE_STATE`
- `CALL_PHONE`
- `READ_CALL_LOG`
- `ANSWER_PHONE_CALLS`

## License

This project is for demonstration purposes and follows the [Truecaller SDK License Agreement](https://www.truecaller.com/docs/android-sdk).
