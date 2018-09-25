#!/usr/bin/env bash
# run the miRna pipeline
#
. /etc/profile
APPNAME=miRnaPipeline

APPDIR=/home/rgddata/pipelines/$APPNAME
cd $APPDIR

DB_OPTS="-Dspring.config=$APPDIR/../properties/default_db.xml"
LOG4J_OPTS="-Dlog4j.configuration=file://$APPDIR/properties/log4j.properties"
declare -x "MI_RNA_PIPELINE_OPTS=$DB_OPTS $LOG4J_OPTS"
bin/$APPNAME "$@"