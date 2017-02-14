#!/usr/bin/env bash
set -e

JAVADOC=javadoc
FLAGS="-quiet"

HARP_ROOT_DIR=$(git rev-parse --show-toplevel)
# for display on GitHub website
JAVADOC_OUTPUT_DIR=$HARP_ROOT_DIR/website/public/api
# for display on local Hugo server
JAVADOC_OUTPUT_LOCAL_DIR=$HARP_ROOT_DIR/website/static/api

# The path of the customized landing page for the Javadocs
OVERVIEW_HTML_FILE=$HARP_ROOT_DIR/website/scripts/javadocs-overview.html

HARP_SRC_FILES=`find $HARP_ROOT_DIR -path "*/edu/iu/harp/*" -name "*.java"`
APACHE_SRC_FILES=`find $HARP_ROOT_DIR -path "*/org/apache/hadoop/*" -name "*.java"`

rm -rf $JAVADOC_OUTPUT_DIR
mkdir -p $JAVADOC_OUTPUT_DIR


$JAVADOC $FLAGS \
  -windowtitle "Harp Java API" \
  -doctitle "The Harp Java API" \
  -overview $OVERVIEW_HTML_FILE \
  -d $JAVADOC_OUTPUT_DIR $HARP_SRC_FILES $APACHE_SRC_FILES

# Generated Java API doc needs to be copied to $JAVADOC_OUTPUT_LOCAL_DIR
# for the following two reasons:
# 1. When one is developing website locally using Hugo server, he should
#    be able to click into API doc link and view API doc to
#    check if the correct API link is given.
# 2. ``wget`` needs to verify if links to Java API doc are valid when Hugo is
#    serving the website locally. This means that Hugo should be able to display
#    Java API doc properly.
cp -r $JAVADOC_OUTPUT_DIR $JAVADOC_OUTPUT_LOCAL_DIR

echo "Javdocs generated at $JAVADOC_OUTPUT_DIR"
exit 0
