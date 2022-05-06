package dev.sigstore.poc;

import static org.apache.commons.codec.digest.MessageDigestAlgorithms.SHA_256;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.security.PublicKey;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.gson.GsonFactory;

public class RekorClient extends AbstractClient {
    public URL rekorInstanceURL = toURL("https://rekor.sigstore.dev");

    //public URL tsaURL = toURL("https://rekor.sigstore.dev/api/v1/timestamp");

    public boolean rekord;

    private URL submitToRekor(String kind, Map<String, Object> spec) throws IOException {
        HttpTransport httpTransport = getHttpTransport();

        Map<String, Object> rekorPostContent = new HashMap<>();
        rekorPostContent.put("kind", kind); 
        rekorPostContent.put("apiVersion", "0.0.1");
        rekorPostContent.put("spec", spec);

        JsonHttpContent rekorJsonContent = new JsonHttpContent(new GsonFactory(), rekorPostContent);

        ByteArrayOutputStream rekorStream = new ByteArrayOutputStream();
        rekorJsonContent.writeTo(rekorStream);
        info("submitting to rekor: " + rekorStream.toString());

        GenericUrl rekorPostUrl = new GenericUrl(rekorInstanceURL + "/api/v1/log/entries");
        HttpRequest rekorReq = httpTransport.createRequestFactory().buildPostRequest(rekorPostUrl, rekorJsonContent);

        rekorReq.getHeaders().set("Accept", "application/json");
        rekorReq.getHeaders().set("Content-Type", "application/json");

        HttpResponse rekorResp = rekorReq.execute();
        if (rekorResp.getStatusCode() != 201) {
            throw new IOException("bad response from rekor: " + rekorResp.parseAsString());
        }

        URL rekorEntryUrl = new URL(rekorInstanceURL, rekorResp.getHeaders().getLocation());
        info(String.format("Created %s entry in transparency log @ '%s'", kind, rekorEntryUrl));
        return rekorEntryUrl;
    }

    public URL submitToRekor(byte[] content, String signature, PublicKey publicKey) throws IOException {
        // https://github.com/sigstore/rekor/blob/main/pkg/types/hashedrekord/v0.0.1/hashedrekord_v0_0_1_schema.json
        // https://github.com/sigstore/rekor/blob/main/pkg/types/rekord/v0.0.1/rekord_v0_0_1_schema.json
        Map<String, Object> hashContent = new HashMap<>();
        hashContent.put("algorithm", "sha256");
        hashContent.put("value", new DigestUtils(SHA_256).digestAsHex(content));

        Map<String, Object> dataContent = new HashMap<>();
        dataContent.put("hash", hashContent);
        if (rekord) {
            dataContent.put("content", Base64.getEncoder().encodeToString(content)); // could not avoid to send content: shouldn't sha256 be sufficient?
        }

        Map<String, Object> publicKeyContent = new HashMap<>();
        byte[] rawKeyText = publicKey.getEncoded();

        Base64.Encoder encoder = Base64.getMimeEncoder(64, System.lineSeparator().getBytes());
        String encodedKeyText = new String(encoder.encode(rawKeyText));
        String prettifiedKey = "-----BEGIN PUBLIC KEY-----" + System.lineSeparator()
                + encodedKeyText + System.lineSeparator()
                + "-----END PUBLIC KEY-----";

        publicKeyContent.put("content", Base64.getEncoder().encodeToString(prettifiedKey.getBytes()));

        Map<String, Object> signatureContent = new HashMap<>();
        if (rekord) {
            signatureContent.put("format", "x509"); // rekord also supports "pgp", "minisign" and "ssh"
        }
        signatureContent.put("publicKey", publicKeyContent);
        signatureContent.put("content", signature);

        Map<String, Object> specContent = new HashMap<>();
        specContent.put("signature", signatureContent); // format publicKey content
        specContent.put("data", dataContent);

        return submitToRekor(rekord ? "rekord" : "hashedrekord", specContent);
    }
}
