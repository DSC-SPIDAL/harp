#!/bin/sh

## sync to the futuresystem account
dataPath=/N/u/lc37/WorkSpace/cpuFascia
ssh $LCJULIET "mkdir -p ${dataPath}"
rsync -r -a -v -e ssh --exclude ".git" --delete . $LCJULIET:${dataPath}