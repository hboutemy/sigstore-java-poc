package dev.sigstore.poc;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertPath;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;

public class Sigstore extends AbstractClient {

    /**
     * All in one file signing with Sigstore.
     * 
     * @param binary the file to sign
     * @throws GeneralSecurityException
     * @throws IOException
     */
    public void sign(File binary) throws GeneralSecurityException, IOException {
        KeyPair keypair = new Crypto().generateKeyPair();
        output(String.format("%s\n   - private: %s\n   - public: %s\n", keypair, keypair.getPrivate(), keypair.getPublic()));

        byte[] content = Files.readAllBytes(binary.toPath());
        String signature = new Crypto().signFileContent(binary, content, keypair.getPrivate());
        output(String.format("%s\n", signature));

        info("Starting sigstore steps to record the signature");

        CertPath certs = getFulcioCert(keypair);
        //new Crypto().writeSigningCertToFile(certs, new File(binary.getParentFile(), binary.getName() + ".pem"));

        new RekorClient().submitToRekor(content, signature, certs.getCertificates().get(0));
    }


    public CertPath getFulcioCert(KeyPair keypair) throws GeneralSecurityException, IOException {
        OidcClient oidcClient = new OidcClient();
        String rawIdToken = oidcClient.getIDToken(); // do OIDC dance, get ID token and email
    
        // sign email address with private key
        String signedEmail = new Crypto().signEmailAddress(oidcClient.emailAddress, keypair.getPrivate());
    
        // push to fulcio, get signing cert chain
        CertPath certs = new FulcioClient().getSigningCert(signedEmail, keypair.getPublic(), rawIdToken);

        output(String.format("%s", new Crypto().prettifySigningCert(certs)));
        return certs;
    }


    public KeyPair generateKeyPair(String signingAlgorithm, String signingAlgorithmSpec) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException {
        info(String.format("generating keypair using '%s' algorithm with '%s' parameter", signingAlgorithm,
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
}
