#!/bin/bash

if [[ -z $1 ]]
then
    echo "must specify input"
    exit 1
fi
I=$1
step=10

tmp=`mktemp` || exit 1
tmpim=`mktemp` || exit 1

b=-b

convert $I[0] -resize 300x300\> -depth 8 pgm:- > $tmpim || exit 1
./vldsift -l 0 $b -s $step - < $tmpim >> $tmp || exit 1
convert $tmpim -resize 50% -depth 8 pgm:- | ./vldsift -l 1 $b -s $step - >> $tmp || exit 1
convert $tmpim -resize 25% -depth 8 pgm:- | ./vldsift -l 2 $b -s $step - >> $tmp || exit 1

cat $tmp

rm -f $tmp $tmpim
