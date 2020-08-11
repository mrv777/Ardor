#!/bin/sh
PATHSEP=":"
if [ "$OSTYPE" = "cygwin" ] ; then
PATHSEP=";"
fi

CP=conf/${PATHSEP}classes/${PATHSEP}lib/*${PATHSEP}testlib/*${PATHSEP}addons/lib/*
SP=src/java/${PATHSEP}test/java/

if [ "$1" = "--verbose" ]; then
  shift
else
  TEST_SYSTEM_PROPERTIES="${TEST_SYSTEM_PROPERTIES} -Dnxt.logging.properties.file.name.prefix=unit-tests-"
fi

if [ $# -eq 0 ]; then
TESTS="nxt.FullAutoSuite"
else
TESTS=$@
fi

/bin/rm -f nxt.jar
/bin/rm -rf classes
/bin/mkdir -p classes/

find src/java/ addons/src/java test/java/ -path src/java/nxtdesktop -prune -o -name "*.java" -print > sources.tmp
javac -encoding utf8 -sourcepath ${SP} -classpath ${CP} -d classes/ @sources.tmp || exit 1
rm -f sources.tmp

cp test/java/unit-tests-logging.properties  classes/unit-tests-logging.properties

for TEST in ${TESTS} ; do
java ${TEST_SYSTEM_PROPERTIES} -classpath ${CP} nxt.JUnitCoreWithListeners ${TEST} ;
done



