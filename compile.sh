#!/bin/bash

## have a fresh build
mkdir -p build
rm -rf build/*

cd build/

export BOOST_ROOT=/N/u/lc37/Lib/boost_1_69_0
export BOOST_LIBRARYDIR=/N/u/lc37/Lib/boost_1_69_0/stage/lib

cmake ..
echo $(gcc --version)

make


