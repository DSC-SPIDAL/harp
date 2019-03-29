#!/bin/sh

## sync to the futuresystem account
dataPath=/N/u/lc37/WorkSpace/gpuPGBSC
ssh $LCJULIET "mkdir -p ${dataPath}"
rsync -r -a -v -e ssh --exclude ".git" --delete . $LCJULIET:${dataPath}

# tesbed of dell pascal p1000 GPU
dataPath=/home/lc37/Workspace/Codebase/gpuPGBSC
ssh $LCDELL "mkdir -p ${dataPath}"
rsync -r -a -v -e ssh --exclude ".git" --delete . $LCDELL:${dataPath}
