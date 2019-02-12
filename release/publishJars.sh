#!/bin/sh -e

printUsage() {
   echo "Usage : "
   echo "./publishJars.sh JAMES_SOURCES_PATH JAMES_DIST_PATH RELEASE SHA1 KEY"
   echo "    JAMES_SOURCES_PATH: path of the James code"
   echo "    JAMES_DIST_PATH: path of the James dist svn repo"
   echo "    RELEASE: release version ex.: 3.3.0"
   echo "    SHA1: sha1 corresponding of the git commit of the release"
   echo "    KEY: key footprint to use for signing"
   exit 1
}

addAndSign() {
    sha256sum "$1" > "$1.sha256"
    gpg -u $KEY --output "$1.asc" --detach-sig "$1"

    svn add "$1"
    svn propset svn:mime-type application/octet-stream "$1"
    svn add "$1.sha256"
    svn add "$1.asc"
    svn propset svn:mime-type application/octet-stream "$1.asc"
}

guiceProduct() {
    mkdir -p "$1/target/$2-$RELEASE"
    cp "$1/target/$2.jar" "$1/target/$2-$RELEASE"
    cp -r "$1/target/$2.lib" "$1/target/$2-$RELEASE"
    cp "$JAMES_SOURCES_PATH/server/container/cli/target/james-server-cli.jar" "$1/target/$2-$RELEASE"
    cp -r "$JAMES_SOURCES_PATH/server/container/cli/target/james-server-cli.lib" "$1/target/$2-$RELEASE"
    cp -r "$3" "$1/target/$2-$RELEASE"
    cp "$1/README.md" "$1/target/$2-$RELEASE"
    cd "$1/target"
    zip -r "$2-$RELEASE.zip" "$2-$RELEASE"
    sha256sum "$2-$RELEASE.zip" > "$2-$RELEASE.zip.sha256"
    gpg -u $KEY --output "$2-$RELEASE.zip.asc" --detach-sig "$2-$RELEASE.zip"
    cp  "$2-$RELEASE.zip" "$2-$RELEASE.zip.asc" "$2-$RELEASE.zip.sha256" "$JAMES_DIST_PATH/server/$RELEASE"
    svn add "$JAMES_DIST_PATH/server/$RELEASE/$2-$RELEASE.zip"
    svn propset svn:mime-type application/octet-stream "$JAMES_DIST_PATH/server/$RELEASE/$2-$RELEASE.zip"
    svn add "$JAMES_DIST_PATH/server/$RELEASE/$2-$RELEASE.zip.sha256"
    svn add "$JAMES_DIST_PATH/server/$RELEASE/$2-$RELEASE.zip.asc"
    svn propset svn:mime-type application/octet-stream "$JAMES_DIST_PATH/server/$RELEASE/$2-$RELEASE.zip.asc"
}

if [ "$#" -ne 5 ]; then
    printUsage
fi

JAMES_SOURCES_PATH=$1
JAMES_DIST_PATH=$2
RELEASE=$3
SHA1=$4
KEY=$5


# Build
cd "$JAMES_SOURCES_PATH"
git checkout $SHA1
mvn clean package -DskipTests

# Mailets
mkdir -p "$JAMES_DIST_PATH/mailets/$RELEASE"
svn add "$JAMES_DIST_PATH/mailets/$RELEASE"

cd "$JAMES_DIST_PATH/mailets/$RELEASE"
cp "$JAMES_SOURCES_PATH/mailet/api/target/apache-mailet-api-$RELEASE.jar" .
addAndSign apache-mailet-api-$RELEASE.jar
cp "$JAMES_SOURCES_PATH/mailet/api/target/apache-mailet-api-$RELEASE-sources.jar" .
addAndSign apache-mailet-api-$RELEASE-sources.jar
cp "$JAMES_SOURCES_PATH/mailet/base/target/apache-mailet-base-$RELEASE.jar" .
addAndSign apache-mailet-base-$RELEASE.jar
cp "$JAMES_SOURCES_PATH/mailet/base/target/apache-mailet-base-$RELEASE-sources.jar" .
addAndSign apache-mailet-base-$RELEASE-sources.jar
cp "$JAMES_SOURCES_PATH/mailet/crypto/target/apache-mailet-crypto-$RELEASE.jar" .
addAndSign apache-mailet-crypto-$RELEASE.jar
cp "$JAMES_SOURCES_PATH/mailet/crypto/target/apache-mailet-crypto-$RELEASE-sources.jar" .
addAndSign apache-mailet-crypto-$RELEASE-sources.jar
cp "$JAMES_SOURCES_PATH/mailet/standard/target/apache-mailet-standard-$RELEASE.jar" .
addAndSign apache-mailet-standard-$RELEASE.jar
cp "$JAMES_SOURCES_PATH/mailet/standard/target/apache-mailet-standard-$RELEASE-sources.jar" .
addAndSign apache-mailet-standard-$RELEASE-sources.jar

# James products
mkdir -p "$JAMES_DIST_PATH/server/$RELEASE"
svn add "$JAMES_DIST_PATH/server/$RELEASE"

cd "$JAMES_DIST_PATH/server/$RELEASE"
cp "$JAMES_SOURCES_PATH/server/app/target/james-server-app-$RELEASE-app.zip" .
addAndSign james-server-app-$RELEASE-app.zip"
guiceProduct "$JAMES_SOURCES_PATH/server/container/guice/cassandra-guice" james-server-cassandra-guice "$JAMES_SOURCES_PATH/dockerfiles/run/guice/cassandra/destination/conf"
guiceProduct "$JAMES_SOURCES_PATH/server/container/guice/cassandra-ldap-guice" james-server-cassandra-ldap-guice "$JAMES_SOURCES_PATH/dockerfiles/run/guice/cassandra-ldap/destination/conf"
guiceProduct "$JAMES_SOURCES_PATH/server/container/guice/cassandra-rabbitmq-guice" james-server-cassandra-rabbitmq-guice "$JAMES_SOURCES_PATH/dockerfiles/run/guice/cassandra-rabbitmq/destination/conf"
guiceProduct "$JAMES_SOURCES_PATH/server/container/guice/cassandra-rabbitmq-ldap-guice" james-server-cassandra-rabbitmq-ldap-guice "$JAMES_SOURCES_PATH/dockerfiles/run/guice/cassandra-rabbitmq-ldap/destination/conf"
guiceProduct "$JAMES_SOURCES_PATH/server/container/guice/jpa-guice" james-server-jpa-guice "$JAMES_SOURCES_PATH/dockerfiles/run/guice/jpa/destination/conf"
guiceProduct "$JAMES_SOURCES_PATH/server/container/guice/jpa-smtp" james-server-jpa-smtp-guice "$JAMES_SOURCES_PATH/dockerfiles/run/guice/jpa-smtp/destination/conf"

# James sources
cd "$JAMES_SOURCES_PATH"
mvn assembly:single -Ppackage-sources
cp "$JAMES_SOURCES_PATH/target/james-project-$RELEASE-src.zip" "$JAMES_DIST_PATH/server/$RELEASE"
svn add "$JAMES_DIST_PATH/server/$RELEASE/james-project-$RELEASE-src.zip"
svn propset svn:mime-type application/octet-stream "$JAMES_DIST_PATH/server/$RELEASE/james-project-$RELEASE-src.zip"
sha256sum "$JAMES_DIST_PATH/server/$RELEASE/james-project-$RELEASE-src.zip" > "$JAMES_DIST_PATH/server/$RELEASE/james-project-$RELEASE-src.zip.sha256"
gpg -u $KEY --output "$JAMES_DIST_PATH/server/$RELEASE/james-project-$RELEASE-src.zip.asc" --detach-sig "$JAMES_DIST_PATH/server/$RELEASE/james-project-$RELEASE-src.zip"
svn add "$JAMES_DIST_PATH/server/$RELEASE/james-project-$RELEASE-src.zip.sha256"
svn add "$JAMES_DIST_PATH/server/$RELEASE/james-project-$RELEASE-src.zip.asc"
svn propset svn:mime-type application/octet-stream "$JAMES_DIST_PATH/server/$RELEASE/james-project-$RELEASE-src.zip.asc"
