sigstore Java PoC
=======

Simple Java PoC code to implement sigstore operations equivalent to `cosign sign-blob`, then be able to test it in any situation.

## Build

Require Java 11

```
mvn package
```

## Run

```
java -jar target/sigstore*.jar [optional file to sign: defaults to pom.xml]
```

will display:

```
Crypto       > generating keypair using EC with secp256r1 parameters
Crypto       > signing file content pom.xml
Sigstore     > Starting sigstore steps to record the signature
OidcClient   > getting OIDC token from https://oauth2.sigstore.dev/auth/token with auth https://oauth2.sigstore.dev/auth/auth
Please open the following address in your browser:
  https://oauth2.sigstore.dev/auth/auth?client_id=sigstore&code_challenge=OJYmpeku1AddJbLf_St5RosjBnSkbcKPFtrI-hdDVS0&code_challenge_method=S256&redirect_uri=http://localhost:43997/Callback&response_type=code&scope=openid%20email
```

you need to authenticate using your browser, then the sigstore work will finish:

```
OidcClient   > received token for email herve.boutemy@gmail.com
Crypto       > signing email address 'herve.boutemy@gmail.com' as proof of possession of private key
FulcioClient > requesting signing certificate from https://fulcio.sigstore.dev/api/v1/signingCert
FulcioClient > parsing signing certificate
RekorClient  > submitting to rekor: {"apiVersion":"0.0.1","kind":"hashedrekord","spec":{"data":{"hash":{"value":"2a080e38dd0214bc46ae9330e0a235589d113bda1e0f1f8032605668c27ed8fd","algorithm":"sha256"}},"signature":{"publicKey":{"content":"LS0tLS1CRUdJTiBQVUJMSUMgS0VZLS0tLS0KTUZrd0V3WUhLb1pJemowQ0FRWUlLb1pJemowREFRY0RRZ0FFYlV5eS93TDdkNjczVXZnY3dDSkp2WVVSRmU2Twp4amY4Zi8xNitJTHFNTUc1MWFrLzZwTERqMmh0MFQyU0IxZGJIOSthUlhFR1VFU05xOU1tcStrK2xnPT0KLS0tLS1FTkQgUFVCTElDIEtFWS0tLS0t"},"content":"MEUCIC0SVsPCThPdi56OJQ/oz0wSA9uw/IjkBwTh+w1mpRsCAiEAu/8Oas0coH7oQPGgrtwxkZvDG36a4yInPOf1/rZ5nNc="}}}
RekorClient  > Created hashedrekord entry in transparency log @ 'https://rekor.sigstore.dev/api/v1/log/entries/024908dcab6ac9602ce1c3da1adf1fdeb2b7de86b8e3c010bf4c4cdf3d7ec046'
```

You may also load the jar with `jshell` to do interactive test...
