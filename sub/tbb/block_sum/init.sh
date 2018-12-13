# .bashrc

# Source global definitions
if [ -f /etc/bashrc ]; then
	. /etc/bashrc
fi

source /opt/intel/compilers_and_libraries/linux/bin/compilervars.sh intel64
source /opt/intel/compilers_and_libraries/linux/mpi/intel64/bin/mpivars.sh
export TBB_ROOT=/opt/intel/compilers_and_libraries/linux/tbb/
source /opt/intel/compilers_and_libraries/linux/tbb/bin/tbbvars.sh intel64
source /opt/intel/vtune_amplifier/amplxe-vars.sh 1>>/dev/null

LD_LIBRARY_PATH=/N/u/pengb/anaconda2/lib/:$LD_LIBRARY_PATH
export LD_LIBRARY_PATH

