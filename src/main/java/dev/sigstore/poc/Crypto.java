package dev.sigstore.poc;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.cert.CertPath;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;

public class Crypto extends AbstractClient {
    public String signingAlgorithm = "EC";

    public String signingAlgorithmSpec = "secp256r1";

    public KeyPair generateKeyPair() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException {
        return generateKeyPair(signingAlgorithm, signingAlgorithmSpec);
    }


    public KeyPair generateKeyPair(String signingAlgorithm, String signingAlgorithmSpec) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException {
        info(String.format("generating keypair using %s with %s parameters", signingAlgorithm,
                signingAlgorithmSpec));
        KeyPairGenerator kpg = KeyPairGenerator.getInstance(signingAlgorithm);
        AlgorithmParameterSpec aps = null;
        switch (signingAlgorithm) {
        case "EC":
            aps = new ECGenParameterSpec(signingAlgorithmSpec);
            break;
        default:
            throw new IllegalArgumentException(String
                    .format("unable to create signing algorithm spec for signing algorithm %s", signingAlgorithm));
        }
        kpg.initialize(aps, new SecureRandom());
        return kpg.generateKeyPair();
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

    public String signFileContent(File file, byte[] content, PrivateKey privKey) throws GeneralSecurityException {
        info(String.format("signing %s file content (%d bytes) with private key %s", file.toString(), file.length(), privKey));
        return signContent(content, privKey);
    }

    public String signEmailAddress(String emailAddress, PrivateKey privKey) throws GeneralSecurityException {
        if (emailAddress == null) {
            throw new IllegalArgumentException("email address must not be null");
        }

        info(String.format("signing email address '%s' as proof of possession of private key\n", emailAddress));
        return signContent(emailAddress.getBytes(), privKey);
    }

    public void writeSigningCertToFile(CertPath certs, File outputSigningCert) throws IOException, GeneralSecurityException {
        info("writing signing certificate to " + outputSigningCert.getAbsolutePath());

        String prettifiedCert = prettifySigningCert(certs);

        try (FileWriter fw = new FileWriter(outputSigningCert)) {
            fw.write(prettifiedCert);
        }
    }

    public String prettifySigningCert(CertPath certs) throws GeneralSecurityException {
        // we only write the first one, not the entire chain
        byte[] rawCrtText = certs.getCertificates().get(0).getEncoded();

        Base64.Encoder encoder = Base64.getMimeEncoder(64, System.lineSeparator().getBytes());
        String encodedCertText = new String(encoder.encode(rawCrtText));

        return "-----BEGIN CERTIFICATE-----" + System.lineSeparator()
                + encodedCertText + System.lineSeparator()
                + "-----END CERTIFICATE-----";
    }
}
