#!/bin/sh

if [ -x jdk/bin/java ]; then
    JAVA=./jdk/bin/java
else
    JAVA=java
fi

DB_DIR=nxt_db
if [ "$1" != "" ];
then
	DB_DIR="$1"
fi

echo "Connecting to ${DB_DIR}"

${JAVA} -cp lib/h2*.jar org.h2.tools.Shell -url jdbc:h2:./${DB_DIR}/nxt -user sa -password sa
