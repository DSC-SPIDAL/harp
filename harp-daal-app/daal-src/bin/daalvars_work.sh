#!/bin/bash

# a script for sourcing the work directory without release

export DAALROOT=/N/u/lc37/Lib/DAAL2017/daal
export CPATH=$DAALROOT/include${CPATH+:${CPATH}}
export LIBRARY_PATH=$DAALROOT/../__work/lnx32e/daal/lib/:$TBBROOT/lib/intel64_lin/gcc4.4${LIBRARY_PATH+:${LIBRARY_PATH}}
export LD_LIBRARY_PATH=$DAALROOT/../__work/lnx32e/daal/lib/:$TBBROOT/lib/intel64_lin/gcc4.4${LD_LIBRARY_PATH+:${LD_LIBRARY_PATH}}
export CLASSPATH=$DAALROOT/../__work/lnx32e/daal/lib/daal.jar${CLASSPATH+:${CLASSPATH}}
