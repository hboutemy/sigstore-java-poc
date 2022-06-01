#!/bin/bash

packages="org/junit/junit-bom
org/junit/platform/junit-platform-engine
org/junit/platform/junit-platform-suite-engine
org/junit/platform/junit-platform-testkit
org/junit/platform/junit-platform-suite-api
org/junit/platform/junit-platform-suite-commons
org/junit/platform/junit-platform-commons
org/junit/platform/junit-platform-reporting
org/junit/platform/junit-platform-runner
org/junit/platform/junit-platform-jfr
org/junit/platform/junit-platform-suite
org/junit/platform/junit-platform-console
org/junit/platform/junit-platform-launcher
org/junit/platform/junit-platform-console-standalone
org/junit/vintage/junit-vintage-engine
org/junit/jupiter/junit-jupiter-api
org/junit/jupiter/junit-jupiter-migrationsupport
org/junit/jupiter/junit-jupiter-params
org/junit/jupiter/junit-jupiter
org/junit/jupiter/junit-jupiter-engine"

suffixes=".pom .module" # only for the first package
repo="1-per-file"
pack="3-per-package"
for p in ${packages}
do
  a="$(basename $p)"
  if [[ "$p" =~ org/junit/platform* ]]
  then
    v=1.8.2
  else
    v=5.8.2
  fi
  [ -d $repo/$p/$v ] || mkdir -p $repo/$p/$v
  for s in $suffixes
  do
    file="$p/$v/$a-$v$s"
    curl -s https://repo.maven.apache.org/maven2/$file --output $repo/$file
  done
  suffixes=".pom .module .jar -sources.jar -javadoc.jar" # starting with second package

  # create package descriptor
  [ -d $pack/$p/$v ] || mkdir -p $pack/$p/$v
  ( cd $repo ; sha256sum $p/$v/* ) | tee $pack/$p/$v/$a-$v.package
done
# create release descriptor listing files
( cd $repo ; sha256sum `find org -type f`) > 2-per-release/$v.release
# create release descriptor listing packages descriptors
( cd $pack ; sha256sum `find org -type f`) > 4-per-package-release/$v.release

echo
echo "strategy 1: sign/Rekor per file"
find $repo -type f | wc -l

echo
echo "strategy 2: 1 unique release descriptor sign/Rekor"
du -h 2-*/$v.release

echo
echo "strategy 3: per package descriptor sign/Rekor"
find $pack -type f -print | wc -l
find $pack -type f -print

echo
echo "strategy 4: per-package descriptor with 1 unique release descriptor sign/Rekor"
du -h 4-*/$v.release
