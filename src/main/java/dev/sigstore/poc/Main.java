package dev.sigstore.poc;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;

public class Main {

    public static void main(String[] args) throws GeneralSecurityException, IOException {
        String filename = args.length > 0 ? args[0] : "pom.xml";
        new Sigstore().sign(new File(filename));
    }

}
