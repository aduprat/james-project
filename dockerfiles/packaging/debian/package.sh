#!/bin/sh -e

printUsage() {
   echo "Usage : "
   echo "./package.sh ITERATION"
   echo "    ITERATION: The iteration to give to the package."
   exit 1
}

if [ "$#" -ne 1 ]; then
    printUsage
fi

ITERATION=$1
cp /jars/james-server.jar /debian/package/usr/share/james/
cp -r /jars/james-server-cassandra-guice.lib /debian/package/usr/share/james/
cp /jars/james-cli.jar /debian/package/usr/share/james/

fpm -s dir -t deb \
 -n james \
 -v 3.0-beta6 \
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
