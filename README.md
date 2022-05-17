sigstore Java PoC
=======

Simple Java PoC code (extracted from [sigstore-maven-plugin PoC](https://github.com/sigstore/sigstore-maven-plugin/pull/85))
to implement sigstore operations equivalent to [`cosign sign-blob` in keyless mode](https://docs.sigstore.dev/cosign/working_with_blobs/#signing-blobs-as-files),
then be able to test it in any situation.

## Build

Require Java 8

```
mvn package
```

## Run

### Basic Executable Jar

```
java -jar target/sigstore*.jar [optional file to sign: defaults to pom.xml]
```

will display:

```
Crypto generating keypair using EC with secp256r1 parameters
Crypto signing file content pom.xml
Sigstore Starting sigstore steps to record the signature
OidcClient >> getting OIDC token from https://oauth2.sigstore.dev/auth/token with auth https://oauth2.sigstore.dev/auth/auth
Please open the following address in your browser:
  https://oauth2.sigstore.dev/auth/auth?client_id=sigstore&code_challenge=OJYmpeku1AddJbLf_St5RosjBnSkbcKPFtrI-hdDVS0&code_challenge_method=S256&redirect_uri=http://localhost:43997/Callback&response_type=code&scope=openid%20email
```

you need to authenticate using your browser, then the sigstore work will finish:

```
OidcClient << received token for email herve.boutemy@gmail.com
Crypto signing email address 'herve.boutemy@gmail.com' as proof of possession of private key
FulcioClient >> requesting signing certificate from https://fulcio.sigstore.dev/api/v1/signingCert
FulcioClient << parsing signing certificate
RekorClient >> submitting to rekor https://rekor.sigstore.dev with payload {"apiVersion":"0.0.1","kind":"hashedrekord","spec":{"data":{"hash":{"value":"2a080e38dd0214bc46ae9330e0a235589d113bda1e0f1f8032605668c27ed8fd","algorithm":"sha256"}},"signature":{"publicKey":{"content":"LS0tLS1CRUdJTiBQVUJMSUMgS0VZLS0tLS0KTUZrd0V3WUhLb1pJemowQ0FRWUlLb1pJemowREFRY0RRZ0FFYlV5eS93TDdkNjczVXZnY3dDSkp2WVVSRmU2Twp4amY4Zi8xNitJTHFNTUc1MWFrLzZwTERqMmh0MFQyU0IxZGJIOSthUlhFR1VFU05xOU1tcStrK2xnPT0KLS0tLS1FTkQgUFVCTElDIEtFWS0tLS0t"},"content":"MEUCIC0SVsPCThPdi56OJQ/oz0wSA9uw/IjkBwTh+w1mpRsCAiEAu/8Oas0coH7oQPGgrtwxkZvDG36a4yInPOf1/rZ5nNc="}}}
RekorClient << Created hashedrekord entry in transparency log @ 'https://rekor.sigstore.dev/api/v1/log/entries/024908dcab6ac9602ce1c3da1adf1fdeb2b7de86b8e3c010bf4c4cdf3d7ec046'
```

### Running against a directory

If you point to a directory instead of a file, the key pair will be used to sign every file found recursively and each signature will be recorded in sigstore:

```
❯ java -jar target/sigstore-poc-0.1.0-SNAPSHOT.jar src
Crypto generating keypair using EC with secp256r1 parameters
signing 7 files
signed 26832 bytes, created 7 .sig signature files for 672 bytes = 96 bytes per sig
Recording signatures to sigstore...
press ENTER to get Fulcio certificate:
OidcClient >> getting OIDC token from https://oauth2.sigstore.dev/auth/token with auth https://oauth2.sigstore.dev/auth/auth
Please open the following address in your browser:
  https://oauth2.sigstore.dev/auth/auth?client_id=sigstore&code_challenge=WcMI_quzd2icY9soD41y_JuzBAvplWmWcX3ED_kOuXU&code_challenge_method=S256&redirect_uri=http://localhost:56851/Callback&response_type=code&scope=openid%20email
Attempting to open that address in the default browser now...
OidcClient << received token for email herve.boutemy@gmail.com
Crypto signing email address 'herve.boutemy@gmail.com' as proof of possession of private key
FulcioClient >> requesting signing certificate from https://fulcio.sigstore.dev/api/v1/signingCert
FulcioClient << parsing signing certificate
Crypto writing signing certificate to /Users/hboutemy/dev/workspace/sigstore-poc/src/signing-certificate.pem
press ENTER to get 7 Rekor entries for previous signatures:
RekorClient >> submitting to rekor https://rekor.sigstore.dev with payload {"apiVersion":"0.0.1","kind":"hashedrekord","spec":{"data":{"hash":{"value":"ca882bac39e1ae2399285495105626538a227a41073620c4653e1fe24ec03435","algorithm":"sha256"}},"signature":{"publicKey":{"content":"LS0tLS1CRUdJTiBQVUJMSUMgS0VZLS0tLS0KTUZrd0V3WUhLb1pJemowQ0FRWUlLb1pJemowREFRY0RRZ0FFOEZIb0VrOE5WSWVSNmMvTE1lS3JvcE4zeWs0WQpVcTZzMjUydEdKTmEzUDJKWUw2L2RXQ1U4bDB0YkVrK3lVb00ycVVIRGYzS21kVzVrTWFFY256TU13PT0KLS0tLS1FTkQgUFVCTElDIEtFWS0tLS0t"},"content":"MEQCIGN/z6q/ErSW4+VgB1uRd5r2/cThm0eqW/4HM1pqUUD6AiBQCJ83loT4h7GmNOLWLgQQzDvQVags+/24mb/phD3rMQ=="}}}
RekorClient << Created hashedrekord entry in transparency log @ 'https://rekor.sigstore.dev/api/v1/log/entries/d24599fd16dd287a6dd2e7897a40df506956d4b62b42b84591f3e109acf2da30'
[...]
created 7 rekor entries, saved in .rekor files for 13567 bytes = 1938 bytes per rekor entry
```

### JShell Interactive Discovery

You may also load the jar with `jshell` to do interactive test of either a basic file signature or more step by step usage for every sigstore piece:

```
$ jshell --class-path target/sigstore-poc-0.1.0-SNAPSHOT.jar 
|  Welcome to JShell -- Version 11.0.7
|  For an introduction type: /help intro

jshell> import dev.sigstore.poc.*

jshell> var sigstore = new Sigstore()
sigstore ==> dev.sigstore.poc.Sigstore@4b952a2d

jshell> sigstore.sign(new File("pom.xml"))
Crypto generating keypair using EC with secp256r1 parameters
Crypto signing file content pom.xml
Sigstore Starting sigstore steps to record the signature
...


jshell> var crypto = new Crypto()
crypto ==> dev.sigstore.poc.Crypto@192d74fb

jshell> var keypair = crypto.generateKeyPair()
Crypto generating keypair using EC with secp256r1 parameters
keypair ==> java.security.KeyPair@62ea3440

jshell> var bin = Files.readAllBytes(new File("pom.xml").toPath())
bin ==> byte[4326] { 60, 112, 114, 111, 106, 101, 99, 116 ... 1, 106, 101, 99, 116, 62 }

jshell> var signature = crypto.signContent(bin, keypair.getPrivate())
signature ==> "MEQCIAHJNMt7JIiHkgiwWBdytp4eWlsSMZBDaL1Zdn6h463V ... fG6EZ6J7UqOz18lFGubd8CA=="

jshell> var certs = sigstore.getFulcioCert(keypair)
OidcClient >> getting OIDC token from https://oauth2.sigstore.dev/auth/token with auth https://oauth2.sigstore.dev/auth/auth
Please open the following address in your browser:
  https://oauth2.sigstore.dev/auth/auth?client_id=sigstore&code_challenge=xqx3jeRsrmZf19OTkmwzvoT6M-1iJ3lUfLaLgoLpa_o&code_challenge_method=S256&redirect_uri=http://localhost:45427/Callback&response_type=code&scope=openid%20email
...

jshell> var rekor = new RekorClient()
rekor ==> dev.sigstore.poc.RekorClient@48c35007

jshell> rekor.submitToRekor(bin, signature, keypair.getPublic())
RekorClient >> submitting to rekor https://rekor.sigstore.dev with payload {"apiVersion":"0.0.1","kind":"hashedrekord","spec":{"data":{"hash":{"value":"6bf06905efee651f935adc9af9cfc23f35327df908979fb9d9b4aa83176611e9","algorithm":"sha256"}},"signature":{"publicKey":{"content":"LS0tLS1CRUdJTiBQVUJMSUMgS0VZLS0tLS0KTUZrd0V3WUhLb1pJemowQ0FRWUlLb1pJemowREFRY0RRZ0FFU1pzRnJNZEJzYTRqNnUrMW85b1NiWTFmNHpVeQpMVnF5Y25LNHJSYmwrUDNybWNsSUJuOEJEYUpQZFQxS2FrVThyR2RBTEpVNmpORERMOUlYbDhld2Z3PT0KLS0tLS1FTkQgUFVCTElDIEtFWS0tLS0t"},"content":"MEQCIAHJNMt7JIiHkgiwWBdytp4eWlsSMZBDaL1Zdn6h463VAiBAsirOrFwuT+6ClCh2bKgfG6EZ6J7UqOz18lFGubd8CA=="}}}
RekorClient << Created hashedrekord entry in transparency log @ 'https://rekor.sigstore.dev/api/v1/log/entries/8517bc33c73fb5f8f8e216229885e3ed6316cc1b5ef001caf350cdeefa52dbeb'
$12 ==> https://rekor.sigstore.dev/api/v1/log/entries/8517bc33c73fb5f8f8e216229885e3ed6316cc1b5ef001caf350cdeefa52dbeb
```
