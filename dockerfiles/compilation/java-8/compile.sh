#!/bin/sh -e
#

printUsage() {
   echo "Usage : "
   echo "./compile.sh [-s | --skipTests] SHA1"
   echo "    -s: Skip test"
   echo "    SHA1: SHA1 to build (optional)"
   exit 1
}

ORIGIN=/origin
DESTINATION=/destination

for arg in "$@"
do
   case $arg in
      -s|--skipTests)
         SKIPTESTS="skipTests"
         ;;
      -*)
         echo "Invalid option: -$OPTARG"
         printUsage
         ;;
      *)
         if ! [ -z "$1" ]; then
            SHA1=$1
         fi
         ;;
   esac
   if [ "0" -lt "$#" ]; then
      shift
   fi
done

if [ -z "$SHA1" ]; then
   SHA1=master
fi

# Sources retrieval
git clone $ORIGIN/.
git checkout $SHA1

# Compilation

if [ "$SKIPTESTS" = "skipTests" ]; then
   mvn package -DskipTests -Pcassandra,exclude-lucene,with-assembly,with-jetm
else
   mvn package -Pcassandra,exclude-lucene,with-assembly,with-jetm
fi

mvn sonar:sonar -Dsonar.host.url=http://62.210.101.42:9000 -Dsonar.github.oauth=671ba766764f5b4e1fa3323fbfae49a0c1e7a408 -Dsonar.github.repository=linagora/james-project -Dsonar.github.pullRequest=188 -Dsonar.analysis.mode=preview

# Retrieve result

if [ $? -eq 0 ]; then
   cp server/app/target/james-server-app-*-app.zip $DESTINATION
   cp server/container/cassandra-guice/target/james-server-cassandra-guice-*-SNAPSHOT.jar $DESTINATION
   cp -r server/container/cassandra-guice/target/james-server-cassandra-guice-*-SNAPSHOT.lib $DESTINATION
   cp server/container/cli/target/james-server-cli-*.jar $DESTINATION
   cp -r server/container/cli/target/james-server-cli-*.lib $DESTINATION
fi
