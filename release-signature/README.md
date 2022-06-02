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

Pro: as simple as current PGP, we can do it right now

Con: for a large project release, this will create many 1,000's of Rekor entries, putting a high load on public Rekor instance: is it a good idea to start doing this?

### strategy #2: 1 signature for the whole release

To reduce the load on Rekor, we can instead create a release desciptor file that contains every file coordinates and their sha256: signing this unique descriptor will permit to have only 1 signature/Rekor entry to certify all release files at once

Pro: reduce drastically the load on Rekor: 1 unique entry, whatever the size of the project release

Cons:
- verifying only one file requires to get the full list of files from the release, which can be huge
- there is a question on at which coordinates to store the release descriptor
- no way to search in Rekor for a jar sha256 and find the entry
- Rekor is ready to accept that, but we need to define a descriptor convention accepted by all JVM ecosystem

### strategy #3: 1 signature per package

A descriptor file with filenames and sha256 can be created for each package (usually 4-5 files), and signed separately

Pro: easy check of each file from a package, even if it's part of a large project release

Cons:
- for large project releases, this can still create near 1,000 Rekor entries.
- no way to search in Rekor for a jar sha256 and find the entry
- Rekor is ready to accept that, but we need to define a descriptor convention accepted by all JVM ecosystem

### strategy #4: 1 descriptor per package, referenced in signed release descriptor

Creating a package descriptor like previous, then a release descriptor that lists the package descritpors and their sha256: that release descriptor is signed.

Notice: need to add in each package descriptor the coordinates of the release descriptor, to be able to find this release descriptor.

Pros:
- only 1 Rekor entry,
- quite efficient verification of a file

Con:
- 2 steps approach: more complex to specify, implement, run
- no way to search in Rekor for a jar sha256 and find the entry
- Rekor is ready to accept that, but we need to define 2 descriptors conventions accepted by all JVM ecosystem

### strategy #5: add some `multihashrekord` type to Rekord

With a new Rekor type that would accept multiple signatures in one payload, Rekor could crreate just one entry in the ledger that would 
contain many files signatures: tracked as [Rekor #845 issue](https://github.com/sigstore/rekor/issues/845).

Pros:
- only 1 Rekor entry,
- spec at Rekor type level, reusable for other needs

Con:
- Rekor is not ready to accept that

## Demo

Using Junit release 5.8.2 as an example of small project release: 20 packages, 78 files.

Run `prepare-junit-5.8.2.sh` script to see the data to sign on each strategy.
