package dev.sigstore.poc;

import java.net.MalformedURLException;
import java.net.URL;

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
}
