#!/bin/sh -e

printUsage() {
   echo "Usage : "
   echo "./importFromModules.sh ORIGIN_GIT_REPO DESTINATION_GIT_PATH COMMITS..."
   echo "    ORIGIN_GIT_REPO: path to the origin git repo"
   echo "    DESTINATION_FOLDER: folder in the destination git repo"
   echo "    COMMITS: list of SHA1 to import"
   exit 1
}

if [ "$#" -lt 3 ]; then
    printUsage
fi

ORIGIN_PATH=$1
DESTINATION_FOLDER=$2
CURRENT_PATH=$PWD

shift 2
for sha1 in "$@"
do
    echo "Processing patch" $sha1
    cd $ORIGIN_PATH
    patchFile=`git format-patch -p -o $CURRENT_PATH -1 $sha1 --src-prefix=a/$DESTINATION_FOLDER/ --dst-prefix=b/$DESTINATION_FOLDER/`

    cd $CURRENT_PATH
    git apply --stat $patchFile
    git am --ignore-space-change < $patchFile
    rm $patchFile
done
