#!/usr/bin/env bash
# run the miRna pipeline
#
. /etc/profile
APPNAME=miRnaPipeline

APPDIR=/home/rgddata/pipelines/$APPNAME
cd $APPDIR

java -jar -Dspring.config=$APPDIR/../properties/default_db.xml \
    -Dlog4j.configuration=file://$APPDIR/properties/log4j.properties \
    -jar lib/$APPNAME.jar "$@"
    