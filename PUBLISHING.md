## Publishing

### Credentials

The library is published to Maven Central with android team's sonatype account

### GPG Key

You will need to create a private GPG keyring on your machine, if you don't have one do the
following steps:

1. Run `gpg --full-generate-key`
1. Choose `RSA and RSA` for the key type
1. Use `4096` for the key size
1. Use `0` for the expiration (never)
1. Use any name, email address, and password

This creates your key in `~/.gnupg/openpgp-revocs.d/` with `.rev` format. The **last 8 characters**
before the `.rev` extension are your **Key ID**.

To export the key, run:

```
gpg --export-secret-keys -o $HOME/sonatype.gpg
```

Finally upload your key to the keyserver:

```
gpg --keyserver keys.openpgp.org --send-keys <YOUR KEY ID>
```

### Local Properties

Open your `komposable-architecture/local.properties` file and fill in the values:

```gradle
signing.keyId=<KEY ID> // 8 chars
signing.password=<PASSWORD YOU CHOSE>
signing.secretKeyRingFile=<PATH TO YOUR GPG FILE> // ../keyname.gpg
ossrhUsername=<SONATYPE USERNAME>
ossrhPassword=<SONATYPE PASSWORD>
sonatypeStagingProfileId=<PROFILE ID>
```

PROFILE ID: This value is an ID that Sonatype assigns to you, which the plugin uses to make sure all the artifacts end up in the right place during the upload.

Go to https://s01.oss.sonatype.org/ and log in. In the menu on the left, select Staging profiles, select your profile, and then look for the ID in the URL.

![mavencentral_sonatype_staging_profile (1)](https://user-images.githubusercontent.com/535613/206739359-94885338-0f0d-4493-80e8-228dbd3a3875.png)


### Publish

To publish, run:

```
./gradlew publishReleasePublicationToSonatypeRepository
```

### Release

Follow [the instructions here](https://central.sonatype.org/pages/releasing-the-deployment.html):

1. Navigate to https://s01.oss.sonatype.org/ and **Log In**
2. On the left side click **Build Promotion** and look for the `com.toggl` repo
3. Click **Close** ... wait a few minutes (you can check status with **Refresh**)
4. Click **Release**

Another useful [tutorial here](https://getstream.io/blog/publishing-libraries-to-mavencentral-2021/) 

