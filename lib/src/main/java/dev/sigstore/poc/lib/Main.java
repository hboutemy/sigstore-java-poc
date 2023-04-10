package dev.sigstore.poc.lib;

import java.nio.file.Paths;
import java.security.cert.CertPath;

import dev.sigstore.KeylessSigner;
import dev.sigstore.bundle.BundleFactory;
import dev.sigstore.encryption.certificates.Certificates;

public class Main {

    public static void main(String[] args) throws Exception {
        String filename = args.length > 0 ? args[0] : "pom.xml";

        var signer = KeylessSigner.builder().sigstorePublicDefaults().build();
        var result = signer.signFile(Paths.get(filename));
        
        // resulting signature information
        
        // artifact digest
        byte[] digest = result.getDigest();
        
        // certificate from fulcio
        CertPath certs = result.getCertPath(); // java representation of a certificate path
        byte[] certsBytes = Certificates.toPemBytes(result.getCertPath()); // converted to PEM encoded byte array
        
        // artifact signature
        byte[] sig = result.getSignature();
        
        // sigstore bundle format (json string)
        String bundle = BundleFactory.createBundle(result);

        System.out.println(bundle);
    }
}
