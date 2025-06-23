
# Local Development Setup for Android App (Development Only)

> **⚠️ Important Note:**  
> The following configurations are intended _only_ for local development. Some of these are **temporary workarounds** to get the app running on an Android emulator and are **not suitable for production use**.  
> Feel free to explore and implement more robust or cleaner solutions as needed.

----------

## Step 1: Configure Development Settings

Open the project in **Android Studio** and update the development configuration file:

`default_config/dev/config.yaml`

Update the following keys with the appropriate values:

`API_HOST_URL:  'http://10.0.2.2:8000' `

` OAUTH_CLIENT_ID:  '<your-oauth-client-id>'`


### Why use `10.0.2.2`?

When running the app in an Android emulator, it cannot resolve `local.openedx.io` because that domain points to your local machine, which is _outside_ the emulator's network.

As a workaround, `10.0.2.2` acts as a special alias that allows the emulator to access the host machine’s localhost (`127.0.0.1`).

----------

## Step 2: Allow Cleartext Traffic to `10.0.2.2`

### 1. Modify `AndroidManifest.xml`

Add the following line inside the `<application>` tag in:

`app/src/main/AndroidManifest.xml`

`android:networkSecurityConfig="@xml/network_security_config"`

### 2. Create Network Security Config File

Create the following XML file at:

`app/src/main/res/xml/network_security_config.xml`

    <?xml version="1.0" encoding="utf-8"?>
    <network-security-config>
       <domain-config cleartextTrafficPermitted="true">
           <domain includeSubdomains="true">10.0.2.2</domain>
           <domain includeSubdomains="true">192.168.1.6</domain>
       </domain-config>
    </network-security-config>


> This configuration allows HTTP (cleartext) traffic to `10.0.2.2`, which is necessary for local development if your server does not support HTTPS.

----------

## You’re All Set!

You should now be able to run and test the app in the Android emulator using your local development server.

----------

## Reminder

> ⚠️ **These changes are strictly for local development purposes only.**
> Do **not** commit or push these configurations to version control.  
> They include development-only settings such as insecure network access
> (`cleartextTrafficPermitted`) and hardcoded localhost URLs
> (`10.0.2.2`) which are **not safe** or suitable for production
> environments.   Always use secure, environment-specific configurations
> for production builds.
