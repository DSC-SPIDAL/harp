# .bashrc

# Source global definitions
if [ -f /etc/bashrc ]; then
	. /etc/bashrc
fi

export use_compiler="intel"

source /opt/intel/compilers_and_libraries/linux/bin/compilervars.sh intel64
source /opt/intel/compilers_and_libraries/linux/mpi/intel64/bin/mpivars.sh
export TBB_ROOT=/opt/intel/compilers_and_libraries/linux/tbb/
source /opt/intel/compilers_and_libraries/linux/tbb/bin/tbbvars.sh intel64
source /opt/intel/vtune_amplifier/amplxe-vars.sh 1>>/dev/null

#export JAVA_HOME=$HOME/soft/jdk1.7.0_79
#export JAVA_HOME=$HOME/soft/jdk1.8.0_112
export JAVA_HOME=/opt/jdk1.8.0_101
export CPATH=/opt/jdk1.8.0_101/include:/opt/jdk1.8.0_101/include/linux:$CPATH
export PATH=$JAVA_HOME/bin:$JAVA_HOME/jre/bin:$PATH

