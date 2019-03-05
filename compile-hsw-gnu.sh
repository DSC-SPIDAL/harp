#!/bin/bash

make distclean ARCH=hsw COMPILER=gnu
make ARCH=hsw COMPILER=gnu
make clean
