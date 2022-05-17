package dev.sigstore.poc;

import static org.apache.commons.codec.digest.MessageDigestAlgorithms.SHA_256;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.cert.CertPath;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;

import com.google.common.io.ByteStreams;

public class Main {

    public static void main(String[] args) throws GeneralSecurityException, IOException {
        String filename = args.length > 0 ? args[0] : "pom.xml";
        File file = new File(filename);
        if (file.isFile()) {
            new Sigstore().sign(file);
        } else {
            signDirectory(file);
        }
    }

    private static void signDirectory(File dir) throws GeneralSecurityException, IOException {
        Sigstore sigstore = new Sigstore();

        Crypto crypto = new Crypto();
        KeyPair keypair = crypto.generateKeyPair();

        // TODO save public key to dir as .pem? or Fulcio cert will be a better choice?

        List<SignedFile> files = listFiles(dir);
        System.out.println("signing " + files.size() + " files");
        long size = 0;
        long sizeSig = 0;
        for (SignedFile file: files) {
            byte[] content = Files.readAllBytes(file.file.toPath());
            size += content.length;
            file.sha256 = new DigestUtils(SHA_256).digestAsHex(content);
            file.signature = crypto.signContent(content, keypair.getPrivate());

            // save base64 encoded signature to .sig
            File sig = new File(file.file.getParentFile(), file.file.getName() + ".sig");
            try (FileWriter fw = new FileWriter(sig)) {
                fw.write(file.signature);
            }
            sizeSig += sig.length();
        }
        System.out.println("signed " + size + " bytes, created " + files.size() + " .sig signature files for " + sizeSig + " bytes = " + (sizeSig / files.size()) + " bytes per sig");

        System.out.println("Recording signatures to sigstore...");
        System.out.print("press ENTER to get Fulcio certificate:");
        enter();
        CertPath certs = sigstore.getFulcioCert(keypair);

        // save PEM encoded signing certificate to signing-certificate.pem file in root dir
        new Crypto().writeSigningCertToFile(certs, new File(dir, "signing-certificate.pem"));

        System.out.print("press ENTER to get " + files.size() + " Rekor entries for previous signatures:");
        enter();
        RekorClient rekorClient = new RekorClient();
        long sizeRek = 0;
        for (SignedFile file: files) {
            URL rekorEntry = rekorClient.submitToRekor(file.sha256, file.signature, keypair.getPublic());

            // save Rekor entry to .rekor file
            File rek = new File(file.file.getParentFile(), file.file.getName() + ".rekor");
            try (InputStream in = rekorEntry.openStream(); OutputStream out = new FileOutputStream(rek)) {
                ByteStreams.copy(in, out);
            }
            sizeRek += rek.length();
        }
        System.out.println("created " + files.size() + " rekor entries, saved in .rekor files for " + sizeRek + " bytes = " + (sizeRek / files.size()) + " bytes per rekor entry");
    }

    private static List<SignedFile> listFiles(File dir) throws IOException {
        List<SignedFile> files = new ArrayList<>();
        for (File file: dir.listFiles()) {
            if (file.isDirectory()) {
                files.addAll(listFiles(file));
            } else if (accept(file.getName())) {
                files.add(new SignedFile(file));
            }
        }
        return files;
    }

    private static boolean accept(String name) {
        String extension = name.substring(name.lastIndexOf('.') + 1);
        return !(IGNORED_EXTENSIONS.contains(extension) || name.endsWith("metadata.xml"));
    }

    private static final Set<String> IGNORED_EXTENSIONS = new HashSet<>(Arrays.asList("asc", "pem", "rekor", "md5", "sha1", "sha256", "sha512"));

    private static class SignedFile {
      private File file;
      private String sha256;
      private String signature;

      private SignedFile(File file) {
          this.file = file;
      }
    }

    private static void enter() throws IOException {
        System.in.read();
        int available = System.in.available();
        if (available > 0) {
            System.in.skip(available);
        }
    }
}
