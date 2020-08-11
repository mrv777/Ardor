#!/bin/bash

PUBLIC_VERSION=$(grep nxt.version ./conf/nxt-default.properties | cut -d'=' -f2)

if [ "$#" -lt 1 ]; then
  ./release-package.sh $PUBLIC_VERSION || :
  BUILD_FILES="ardor-client-$PUBLIC_VERSION.sh ardor-client-$PUBLIC_VERSION.zip"
  echo $BUILD_FILES
else
  BUILD_FILES=${@:1}
fi

COMMIT_HASH=$(git log -n1 --format="%h")
DATE_FORMATTED=$(date '+%Y%m%d-%H%M%S')

PACKAGE_NAME=ardor-beta-${PUBLIC_VERSION}-${DATE_FORMATTED}-${COMMIT_HASH}.zip
echo PACKAGE_NAME="${PACKAGE_NAME}"

zip -q -X ${PACKAGE_NAME} changelog-full.txt $BUILD_FILES
