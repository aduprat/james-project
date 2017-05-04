#!/bin/sh -e

printUsage() {
   echo "Usage : "
   echo "./package.sh RELEASE ITERATION"
   echo "    RELEASE  : The release to be generated."
   echo "    ITERATION: The iteration to give to the package."
   exit 1
}

if [ "$#" -ne 2 ]; then
    printUsage
fi

RELEASE=$1
ITERATION=$2
cp /jars/james-server.jar /debian/package/usr/share/james/
cp -r /jars/james-server-cassandra-guice.lib /debian/package/usr/share/james/
cp /jars/james-cli.jar /debian/package/usr/share/james/

fpm -s dir -t deb \
 -n james \
 -v $RELEASE \
 -a x86_64 \
 -d openjdk-8-jre \
 -C package \
 --deb-systemd james.service \
 --after-install james.postinst \
 --before-remove james.prerm \
 --provides mail-transport-agent \
 --provides default-mta \
 --iteration $ITERATION \
 .

cp /debian/james*.deb /result/
