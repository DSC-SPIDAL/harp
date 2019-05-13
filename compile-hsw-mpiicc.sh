#!/bin/bash

make distclean ARCH=hsw COMPILER=icc DISTRI=mpiicc
make ARCH=hsw COMPILER=icc DISTRI=mpiicc
