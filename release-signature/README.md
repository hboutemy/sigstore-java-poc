Signing A Project Release
========

## Project, Release, Package, File

A project release (`version`) consists in 1 or more packages (`groupId:artifactId`), with each package having 1 or more files (`.pom`, eventual `.jar`, `-sources.jar`, `-javadoc.jar`, `.module` or other).

Small project have only 1 package, many projects have 10 to 50 packages, some large project have up to 1,000 packages (example: [NiFi](https://github.com/jvm-repo-rebuild/reproducible-central/blob/master/content/org/apache/nifi/README.md), with 650 packages).

Each package has usually 4 or 5 files.

## Signature Granularity

With existing PGP, each file is signed: each detached signature is stored in a `.asc` file.

With sigstore, each signature has to be certified by a Rekor entry.

### strategy #1: 1 signature per file

Default signature strategy is to sign each file separately, then have as many Rekor entries as there are files.

Pro: as simple as current PGP

Con: for a large project release, this will create many 1,000's of Rekor entries, putting a high load on public Rekor instance

### strategy #2: 1 signature for the whole release

To reduce the load on Rekor, we can instead create a release desciptor file that contains every file coordinates and their sha256: signing this unique descriptor will permit to have only 1 signature/Rekor entry to certify all release files at once

Pro: reduce drastically the load on Rekor: 1 unique entry, whatever the size of the project release

Con: verifying only one file requires to get the full list of files from the release, which can be huge. And there is a question on at which coordinates to store the release descriptor.
No way to search in Rekor for a jar sha256 and find the entry

### strategy #3: 1 signature per package

A descriptor file with filenames and sha256 can be created for each package (usually 4-5 files), and signed separately

Pro: easy check of each file from a package, even if it's part of a large project release

Con: for large project releases, this can still create near 1,000 Rekor entries.
No way to search in Rekor for a jar sha256 and find the entry

### strategy #4: 1 descriptor per package, referenced in signed release descriptor

Creating a package descriptor like previous, then a release descriptor that lists the package descritpors and their sha256: that release descriptor is signed.

Notice: need to add in each package descriptor the coordinates of the release descriptor, to be able to find this release descriptor.

Pro: only 1 Rekor entry, and quite efficient verification of a file

Con: 2 steps approach. No way to search in Rekor for a jar sha256 and find the entry


## Demo

Using Junit release 5.8.2 as an example of small project release: 20 packages, 78 files.

Run `prepare-junit-5.8.2.sh` script to see the data to sign on each strategy.
