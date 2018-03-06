#!/usr/bin/env bash

cwd="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
examples=`ls ${cwd}/run_harp_daal_*.py`

export PYTHONPATH=${cwd}/../../

for pyrun in ${examples}
do
    echo -e "\033[0;34m${pyrun}\033[0m"
    python ${pyrun}
done
