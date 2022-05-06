package dev.sigstore.poc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.PublicKey;
import java.security.cert.CertPath;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.PemReader;
import com.google.api.client.util.PemReader.Section;

public class FulcioClient extends AbstractClient {
    public URL fulcioInstanceURL = toURL("https://fulcio.sigstore.dev");

    public CertPath getSigningCert(String signedEmail, PublicKey pubKey, String idToken) throws IOException, CertificateException {
        HttpTransport httpTransport = getHttpTransport();

        String publicKeyB64 = Base64.getEncoder().encodeToString(pubKey.getEncoded());
        Map<String, Object> fulcioPostContent = new HashMap<>();
        Map<String, Object> publicKeyContent = new HashMap<>();
        publicKeyContent.put("content", publicKeyB64);
        // TODO: look at signingAlgorithm and set accordingly
        if (pubKey.getAlgorithm().equals("EC")) {
            publicKeyContent.put("algorithm", "ecdsa");
        }

        fulcioPostContent.put("signedEmailAddress", signedEmail);
        fulcioPostContent.put("publicKey", publicKeyContent);
        JsonHttpContent jsonContent = new JsonHttpContent(new GsonFactory(), fulcioPostContent);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        jsonContent.writeTo(stream);

        GenericUrl fulcioPostUrl = new GenericUrl(fulcioInstanceURL + "/api/v1/signingCert");
        HttpRequest req = httpTransport.createRequestFactory().buildPostRequest(fulcioPostUrl, jsonContent);

        req.getHeaders().set("Accept", "application/pem-certificate-chain");
        req.getHeaders().set("Authorization", "Bearer " + idToken);

        info(String.format("requesting signing certificate from %s", fulcioPostUrl.toString()));
        HttpResponse resp = req.execute();
        if (resp.getStatusCode() != 201) {
            throw new IOException(
                    String.format("bad response from fulcio @ '%s' : %s", fulcioPostUrl, resp.parseAsString()));
        }

        info("parsing signing certificate");
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        ArrayList<X509Certificate> certList = new ArrayList<>();
        PemReader pemReader = new PemReader(new InputStreamReader(resp.getContent()));
        while (true) {
            Section section = pemReader.readNextSection();
            if (section == null) {
                break;
            }

            byte[] certBytes = section.getBase64DecodedBytes();
            certList.add((X509Certificate) cf.generateCertificate(new ByteArrayInputStream(certBytes)));
        }
        if (certList.isEmpty()) {
            throw new IOException("no certificates were found in response from Fulcio instance");
        }
        return cf.generateCertPath(certList);
    }
}
