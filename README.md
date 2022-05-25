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
RekorClient >> submitting to rekor https://rekor.sigstore.dev with payload {"apiVersion":"0.0.1","kind":"hashedrekord","spec":{"data":{"hash":{"value":"a3a7e29372082134a6dc2b3eb59d7a2fa6466b328e849530d75d01f181d0645f","algorithm":"sha256"}},"signature":{"publicKey":{"content":"LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSUNGVENDQVpxZ0F3SUJBZ0lUQ2laMS9FUVA4ODRsZnluTHpUanJTNFZoZHpBS0JnZ3Foa2pPUFFRREF6QXEKTVJVd0V3WURWUVFLRXd4emFXZHpkRzl5WlM1a1pYWXhFVEFQQmdOVkJBTVRDSE5wWjNOMGIzSmxNQjRYRFRJeQpNRFV5TlRFMk1EUXhOVm9YRFRJeU1EVXlOVEUyTVRReE5Gb3dBREJaTUJNR0J5cUdTTTQ5QWdFR0NDcUdTTTQ5CkF3RUhBMElBQlAvVVE0NmlnYTYwQlNzT2FEcUlSVmNRTUh3YVlwNzlrSkQwNDgweU9vUmo2T0c1OTNPYVFldlkKaDZ6YTJkZExjaDM4NjBNVWVBcHBBTUQvYTdmblIvQ2pnY2d3Z2NVd0RnWURWUjBQQVFIL0JBUURBZ2VBTUJNRwpBMVVkSlFRTU1Bb0dDQ3NHQVFVRkJ3TURNQXdHQTFVZEV3RUIvd1FDTUFBd0hRWURWUjBPQkJZRUZPcnBYclN0Ck9zMlFJRkMwOHlNcUVQSlUwL1g0TUI4R0ExVWRJd1FZTUJhQUZGakFIbCtSUmFWbXFYck1rS0dUSXRBcXhjWDYKTUNVR0ExVWRFUUVCL3dRYk1CbUJGMmhsY25abExtSnZkWFJsYlhsQVoyMWhhV3d1WTI5dE1Da0dDaXNHQVFRQgpnNzh3QVFFRUcyaDBkSEJ6T2k4dllXTmpiM1Z1ZEhNdVoyOXZaMnhsTG1OdmJUQUtCZ2dxaGtqT1BRUURBd05wCkFEQm1BakVBM29JMTN5bW1yUW9od1Y1MlM2R1hhWk1HVFhLeDZSdUpBYkpIQ3AycDh3VzZ0MzlYNjN1UkNlaUwKTjkybWMrWkhBakVBdXhoU3AzcjFLVVVRRHhJK2Fsd0dac3RGeWI5REhnQnBXRGhjTjJETWRVKzhHeithbFBnQQpBNjQ5bU9IOVZuLysKLS0tLS1FTkQgQ0VSVElGSUNBVEUtLS0tLQ=="},"content":"MEUCIDxs/kuUecXPFYBNaQ/g06sRXJpj0WLzgOmV/QMApnPYAiEAtmIw7gQiv8J5XLtOW4ITOuRB6srhWJoWWQ2wBGjU/2c="}}}RekorClient << Created hashedrekord entry in transparency log @ 'https://rekor.sigstore.dev/api/v1/log/entries/19242336d73a5a435caecd68470fba38ed2438f56ac5bd6b818f0f23757714c1'
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

jshell> rekor.submitToRekor(bin, signature, certs.getCertificates().get(0))
RekorClient >> submitting to rekor https://rekor.sigstore.dev with payload {"apiVersion":"0.0.1","kind":"hashedrekord","spec":{"data":{"hash":{"value":"ca882bac39e1ae2399285495105626538a227a41073620c4653e1fe24ec03435","algorithm":"sha256"}},"signature":{"publicKey":{"content":"LS0tLS1CRUdJTiBQVUJMSUMgS0VZLS0tLS0KTUZrd0V3WUhLb1pJemowQ0FRWUlLb1pJemowREFRY0RRZ0FFOEZIb0VrOE5WSWVSNmMvTE1lS3JvcE4zeWs0WQpVcTZzMjUydEdKTmEzUDJKWUw2L2RXQ1U4bDB0YkVrK3lVb00ycVVIRGYzS21kVzVrTWFFY256TU13PT0KLS0tLS1FTkQgUFVCTElDIEtFWS0tLS0t"},"content":"MEQCIGN/z6q/ErSW4+VgB1uRd5r2/cThm0eqW/4HM1pqUUD6AiBQCJ83loT4h7GmNOLWLgQQzDvQVags+/24mb/phD3rMQ=="}}}
RekorClient << Created hashedrekord entry in transparency log @ 'https://rekor.sigstore.dev/api/v1/log/entries/d24599fd16dd287a6dd2e7897a40df506956d4b62b42b84591f3e109acf2da30'
$12 ==> https://rekor.sigstore.dev/api/v1/log/entries/d24599fd16dd287a6dd2e7897a40df506956d4b62b42b84591f3e109acf2da30
```
