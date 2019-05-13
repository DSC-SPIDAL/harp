#!/bin/bash

## to generate the json database for YCM
#bear make ARCH=hsw COMPILER=gnu
compiledb -n make -f Makefile-db ARCH=hsw COMPILER=gnu
