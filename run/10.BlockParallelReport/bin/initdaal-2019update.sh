#LD_LIBRARY_PATH=/N/u/pengb/anaconda2/lib/:$LD_LIBRARY_PATH
#export LD_LIBRARY_PATH

CPLUSLIB=/N/u/pengb/anaconda2/lib/
#CPLUSLIB=/opt/intel/vtune_amplifier/lib64/
LD_LIBRARY_PATH=$CPLUSLIB:$LD_LIBRARY_PATH
export LD_LIBRARY_PATH


root=/scratch_hdd/hpda/optgbt/daal2019-update1.1/
echo "root=$root"
source $root/daal/__release_lnx/daal/bin/daalvars.sh intel64
