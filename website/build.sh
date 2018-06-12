#!/bin/sh

## build the website by hugo
gulp clean
gulp build
hugo
cp design/index.html public
hugo server --watch --ignoreCache
