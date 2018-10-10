#!/bin/sh -e

printUsage() {
   echo "Usage : "
   echo "./merge.sh SHA1 RESULTING_BRANCH"
   echo "    BRANCH: the original branch used for merge"
   echo "    SHA1: SHA1 to merge with the branch"
   echo "    RESULTING_BRANCH : Resulting branch of the merge"
   exit 1
}

if [ "$#" -ne 3 ]; then
    printUsage
fi

BRANCH=$1
SHA1=$2
RESULTING_BRANCH=$3

APACHE_REPO=`git remote show | grep apache || true`
if [ -z "$APACHE_REPO" ]; then
    git remote add apache https://github.com/apache/james-project.git
fi 
# Getting BRANCH from apache repo
git fetch apache
git checkout apache/$BRANCH -B $BRANCH

# Getting the branch to be merged from /origin
git fetch origin
git checkout $SHA1
git checkout -b SHA1_BRANCH

# Merging the branch to be merged in the original BRANCH
git checkout $BRANCH
git checkout -b $RESULTING_BRANCH
git merge --no-edit SHA1_BRANCH
