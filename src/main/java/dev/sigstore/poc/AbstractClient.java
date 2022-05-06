package dev.sigstore.poc;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Base64;

import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.HttpClientBuilder;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.apache.v2.ApacheHttpTransport;

public class AbstractClient {
    public boolean sslVerfication = true;

    public void info(String msg) {
      System.out.print(String.format("%-12s > ",this.getClass().getName().substring(17)));
      System.out.println(msg);
    }
    
    public URL toURL(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            throw new RuntimeException("invalid URL", e);
        }
    }
    
    public HttpTransport getHttpTransport() {
        HttpClientBuilder hcb = ApacheHttpTransport.newDefaultHttpClientBuilder();
        if (!sslVerfication) {
            hcb = hcb.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE);
        }
        return new ApacheHttpTransport(hcb.build());
    }

    public String signContent(byte[] content, PrivateKey privKey) throws GeneralSecurityException {
        if (privKey == null) {
            throw new IllegalArgumentException("private key must be specified");
        }
        if (content == null) {
            throw new IllegalArgumentException("content must not be null");
        }

        Signature sig = null;
        switch (privKey.getAlgorithm()) {
        case "EC":
            sig = Signature.getInstance("SHA256withECDSA");
            break;
        default:
            throw new NoSuchAlgorithmException(
                    String.format("unable to generate signature for signing algorithm %s", privKey.getAlgorithm()));
        }
        sig.initSign(privKey);
        sig.update(content);
      return Base64.getEncoder().encodeToString(sig.sign());
  }

    public String signEmailAddress(String emailAddress, PrivateKey privKey) throws GeneralSecurityException {
        if (emailAddress == null) {
            throw new IllegalArgumentException("email address must not be null");
        }

        info(String.format("signing email address '%s' as proof of possession of private key", emailAddress));
        return signContent(emailAddress.getBytes(), privKey);
    }


}
