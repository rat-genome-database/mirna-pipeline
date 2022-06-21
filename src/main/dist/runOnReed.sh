#!/usr/bin/env bash
# run the miRna pipeline on pipelines prod database: REED
#
. /etc/profile
APPNAME=miRnaPipeline

APPDIR=/home/rgddata/pipelines/${APPNAME}_on_reed
cd $APPDIR

java -Dspring.config=$APPDIR/../properties/reed2.xml \
    -Dlog4j.configurationFile=file://$APPDIR/properties/log4j2.xml \
    -jar lib/$APPNAME.jar -Xmx100G --load --stats "$@"
