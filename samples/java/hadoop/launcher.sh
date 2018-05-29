#!/bin/bash
#===============================================================================
# Copyright 2017-2018 Intel Corporation
# All Rights Reserved.
#
# If this  software was obtained  under the  Intel Simplified  Software License,
# the following terms apply:
#
# The source code,  information  and material  ("Material") contained  herein is
# owned by Intel Corporation or its  suppliers or licensors,  and  title to such
# Material remains with Intel  Corporation or its  suppliers or  licensors.  The
# Material  contains  proprietary  information  of  Intel or  its suppliers  and
# licensors.  The Material is protected by  worldwide copyright  laws and treaty
# provisions.  No part  of  the  Material   may  be  used,  copied,  reproduced,
# modified, published,  uploaded, posted, transmitted,  distributed or disclosed
# in any way without Intel's prior express written permission.  No license under
# any patent,  copyright or other  intellectual property rights  in the Material
# is granted to  or  conferred  upon  you,  either   expressly,  by implication,
# inducement,  estoppel  or  otherwise.  Any  license   under such  intellectual
# property rights must be express and approved by Intel in writing.
#
# Unless otherwise agreed by Intel in writing,  you may not remove or alter this
# notice or  any  other  notice   embedded  in  Materials  by  Intel  or Intel's
# suppliers or licensors in any way.
#
#
# If this  software  was obtained  under the  Apache License,  Version  2.0 (the
# "License"), the following terms apply:
#
# You may  not use this  file except  in compliance  with  the License.  You may
# obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
#
#
# Unless  required  by   applicable  law  or  agreed  to  in  writing,  software
# distributed under the License  is distributed  on an  "AS IS"  BASIS,  WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#
# See the   License  for the   specific  language   governing   permissions  and
# limitations under the License.
#===============================================================================

##  Content:
##     Intel(R) Data Analytics Acceleration Library samples
##******************************************************************************

help_message() {
    echo "Usage: launcher.sh {arch|help}"
    echo "arch          - can be ia32 or intel64, optional for building examples."
    echo "help          - print this message"
    echo "Example: launcher.sh ia32 or launcher.sh intel64"
}

daal_ia=
first_arg=$1

while [ "$1" != "" ]; do
    case $1 in
        ia32|intel64) daal_ia=$1
                      ;;
        help)         help_message
                      exit 0
                      ;;
        *)            break
                      ;;
    esac
    shift
done

if [ "${daal_ia}" != "ia32" -a "${daal_ia}" != "intel64" ]; then
    echo Bad argument arch = ${first_arg} , must be ia32 or intel64
    help_message
    exit 1
fi

# Setting CLASSPATH to build jar
export CLASSPATH=${DAALROOT}/lib/daal.jar:${SCALA_JARS}:$CLASSPATH

# Creating _results folder
result_folder=$(command -p cd $(dirname -- "${BASH_SOURCE}"); pwd)/_results/
if [ -d ${result_folder} ]; then rm -rf ${result_folder}; fi
mkdir -p ${result_folder}

hdfs dfs -mkdir -p /Hadoop/Libraries                                                        >  ${result_folder}/hdfs.log 2>&1

# Comma-separated list of shared libs
os_name=`uname`
if [ "${os_name}" == "Linux" ]; then
    export LIBJAVAAPI=libJavaAPI.so
    export LIBTBB=libtbb.so.2
    export LIBTBBMALLOC=libtbbmalloc.so.2
    hdfs dfs -put -f ${DAALROOT}/lib/${daal_ia}_lin/${LIBJAVAAPI} ${DAALROOT}/../tbb/lib/${daal_ia}_lin/gcc4.4/${LIBTBB} ${DAALROOT}/../tbb/lib/${daal_ia}_lin/gcc4.4/${LIBTBBMALLOC} /Hadoop/Libraries/   >> ${result_folder}/hdfs.log 2>&1
elif [ "${os_name}" == "Darwin" ]; then
    export LIBJAVAAPI=libJavaAPI.dylib
    export LIBTBB=libtbb.dylib
    export LIBTBBMALLOC=libtbbmalloc.dylib
    hdfs dfs -put -f ${DAALROOT}/lib/${LIBJAVAAPI} ${DAALROOT}/../tbb/lib/${LIBTBB} ${DAALROOT}/../tbb/lib/${LIBTBBMALLOC} /Hadoop/Libraries/ >> ${result_folder}/hdfs.log 2>&1
fi

# Setting envs
export LIBJARS=${DAALROOT}/lib/daal.jar
export CLASSPATH=${LIBJARS}:${CLASSPATH}
export HADOOP_CLASSPATH=${LIBJARS}

# Setting list of Spark samples to process
if [ -z ${Hadoop_samples_list} ]; then
    source ./daal.lst
fi

for sample in ${Hadoop_samples_list[@]}; do

    results=${result_folder}/${sample}/
    mkdir ${results}

    # Delete output folder if it exists
    hdfs dfs -rm -r -f /Hadoop/${sample}       >> ${results}/${sample}.log 2>&1

    # Create required folders on HDFS
    hdfs dfs -mkdir -p /Hadoop/${sample}/input >> ${results}/${sample}.log 2>&1
    hdfs dfs -mkdir -p /Hadoop/${sample}/data  >> ${results}/${sample}.log 2>&1

    # Copy datasets to HDFS
    hdfs dfs -put ./data/${sample}*.csv /Hadoop/${sample}/data/ >> ${results}/${sample}.log 2>&1

    cd sources

    # Copy file with the dataset names to the input folder
    hdfs dfs -put ${sample}_filelist.txt /Hadoop/${sample}/input/ >> ${results}/${sample}.log 2>&1

    # Building the sample
    mkdir -p ../build
    javac -d ./../build/ -sourcepath ./ ./${sample}*.java ./WriteableData.java >> ${results}/${sample}.log 2>&1

    # Running the sample
    cd ../build/
    jar -cvfe ${sample}.jar DAAL.${sample} ./DAAL/${sample}* ./DAAL/WriteableData.class >> ${results}/${sample}.log 2>&1

    cmd="hadoop jar ${sample}.jar -libjars ${LIBJARS} /Hadoop/${sample}/input /Hadoop/${sample}/Results"
    echo $cmd > ${sample}.log
    `${cmd} >> ${results}/${sample}.log 2>&1`

    hdfs dfs -ls /Hadoop/${sample}/Results >> ${results}/${sample}.log 2>&1

    # Checking run status
    grepsuccess=`grep '/Results/_SUCCESS' ${results}/${sample}.log 2>/dev/null`
    greperror=`grep 'Error:' ${results}/${sample}.log 2>/dev/null`
    if [ "$grepsuccess" == "" ] || [ "$greperror" != "" ]; then
        echo -e "`date +'%H:%M:%S'` FAILED\t\t${sample}"
    else
        echo -e "`date +'%H:%M:%S'` PASSED\t\t${sample}"
    fi

    hdfs dfs -get /Hadoop/${sample}/Results/* ${results} >> ${results}/${sample}.log 2>&1

    cd ../
done