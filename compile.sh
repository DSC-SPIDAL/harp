#!/bin/bash

## if setup the DAALROOT for experimental apps 
# export DAALROOT=/N/u/lc37/Lib/DAAL2018_Beta/__release_subgraph_hsw_lnx/daal
export DAALROOT=/N/u/lc37/IntelDAAL/daal/__release_lnx/daal

mvn clean package
