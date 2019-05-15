#!/bin/sh

runPath=/N/u/lc37/WorkSpace/PGBSCTest
ssh $LCJULIET "mkdir -p ${runPath}"
#scp ./hsw-run-pgbsc-mpi.sh $LCJULIET:${runPath}/
#scp ./machinehosts $LCJULIET:${runPath}/
#scp ./check-nodes.sh $LCJULIET:${runPath}/
scp ./DataCopy-juliet.sh $LCJULIET:${runPath}/
