package dev.sigstore.poc;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.net.URL;
import java.util.Arrays;
import java.util.Base64;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.auth.openidconnect.IdToken;
import com.google.api.client.auth.openidconnect.IdTokenVerifier;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.DataStoreFactory;
import com.google.api.client.util.store.MemoryDataStoreFactory;

public class OidcClient extends AbstractClient {
    public boolean oidcDeviceCodeFlow = false;

    public String oidcClientID = "sigstore";

    public URL oidcAuthURL = toURL("https://oauth2.sigstore.dev/auth/auth");

    public URL oidcTokenURL = toURL("https://oauth2.sigstore.dev/auth/token");

    //public URL oidcDeviceCodeURL = toURL("https://oauth2.sigstore.dev/auth/auth/device/code");

    public String emailAddress;

    public String getIDToken() throws IOException {
        return getIDToken(null);
    }

    public String getIDToken(String expectedEmailAddress) throws IOException {
        JsonFactory jsonFactory = new GsonFactory();
        HttpTransport httpTransport = getHttpTransport();
        DataStoreFactory memStoreFactory = new MemoryDataStoreFactory();

        final String idTokenKey = "id_token";

        if (!oidcDeviceCodeFlow) {
            info(String.format(">> getting OIDC token for 'sigstore' audience from %s with auth %s", oidcTokenURL.toString(), oidcAuthURL.toString()));
            AuthorizationCodeFlow.Builder flowBuilder = new AuthorizationCodeFlow.Builder(
                    BearerToken.authorizationHeaderAccessMethod(), httpTransport, jsonFactory,
                    new GenericUrl(oidcTokenURL.toString()), new ClientParametersAuthentication(oidcClientID, null),
                    oidcClientID, oidcAuthURL.toString())
                    .enablePKCE()
                    .setScopes(Arrays.asList("openid", "email"))
                    .setCredentialCreatedListener(new AuthorizationCodeFlow.CredentialCreatedListener() {
                        @Override
                        public void onCredentialCreated(Credential credential, TokenResponse tokenResponse)
                            throws IOException {
                            memStoreFactory.getDataStore("user").set(idTokenKey, tokenResponse.get(idTokenKey).toString());
                        }
                    });
            AuthorizationCodeInstalledApp app = new AuthorizationCodeInstalledApp(flowBuilder.build(),
                    new LocalServerReceiver());
            app.authorize("user");
        }
        // TODO: add device code flow support

        String idTokenString = (String) memStoreFactory.getDataStore("user").get(idTokenKey);

        IdTokenVerifier idTokenVerifier = new IdTokenVerifier();
        IdToken parsedIdToken = IdToken.parse(jsonFactory, idTokenString);
        if (!idTokenVerifier.verify(parsedIdToken)) {
            throw new InvalidObjectException("id token could not be verified");
        }

        String emailFromIDToken = (String) parsedIdToken.getPayload().get("email");
        Boolean emailVerified = (Boolean) parsedIdToken.getPayload().get("email_verified");
        if (expectedEmailAddress != null && !emailFromIDToken.equals(expectedEmailAddress)) {
            throw new InvalidObjectException(
                    String.format("email in ID token '%s' does not match address specified to plugin '%s'",
                            emailFromIDToken, emailAddress));
        } else if (Boolean.FALSE.equals(emailVerified)) {
            throw new InvalidObjectException(
                    String.format("identity provider '%s' reports email address '%s' has not been verified",
                            parsedIdToken.getPayload().getIssuer(), emailAddress));
        }
        emailAddress = emailFromIDToken;

        String[] parts = idTokenString.split("\\.");
        String jwtHeader = parts[0];
        String jwtPayload = parts[1];
        String jwtSignature = parts[2];

        output(String.format("OIDC token:\n- header = %s\n- payload = %s\n- signature = %s",
            new String(Base64.getDecoder().decode(jwtHeader)),
            new String(Base64.getDecoder().decode(jwtPayload)).replace(",", ",\n  "),
            jwtSignature));

        info(String.format("<< received token for email %s", emailAddress));

        return idTokenString;
    }
}
