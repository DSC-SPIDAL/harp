#!/bin/sh

## build the website by hugo
gulp clean
gulp build
hugo
hugo server --watch --ignoreCache
