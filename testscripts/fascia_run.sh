#!/bin/bash

app_dir=/N/u/lc37/Project/sc-vec
work_dir=./
log_dir=$work_dir/log
mkdir -p ${log_dir}
# if [ ! -d ./fascia-vec-knl ];then
#     ln -s ${outdir} ./ 
# fi

graph_loc=/dev/shm/lc37/fascia-data/graphs
template_loc=/dev/shm/lc37/fascia-data/templates

## ----------------------------------- default parameters -----------------------------------
# threads per mpi procs
tpproc=48
# omp affinity compact/scatter
affinity_type=scatter
# affinity_type=compact
# bindopt=socket
bindopt=none
# hosts list
# hosts=$work_dir/test_scripts/hosts-single
# iteration
Itr=1

export OMP_PLACES=cores 
#
if [ "$affinity_type" == "compact" ];then
    export OMP_PROC_BIND=close
else
    export OMP_PROC_BIND=spread
fi

# -------------------- experiment version --------------------
# version of experiment
# version=comparenec
# version=binarydata

# version=vertex-pruned
# algoMode=0
#
# version=vertex-fascia-pruned
# algoMode=1
version=debug-vertex-fascia-original-pruned
# version=debug-vertex-binarydata

# version=vertex-fascia
# algoMode=2

writeBinary=0

# version=AdaptiveSmall # from small templates to large template 
# version=NodeScalefascia
## -------------- test on the collected data --------------
## real datasets
for graph_file in miami.graph
# for graph_file in orkut.graph
# for graph_file in nyc.graph
# for graph_file in RMAT-Data-sk-3-nV-1000000-nE-100000000.fascia
# for graph_file in RMAT-Data-sk-3-nV-4000000-nE-100000000.fascia RMAT-Data-sk-3-nV-4000000-nE-200000000.fascia RMAT-Data-sk-3-nV-4000000-nE-300000000.fascia
# for graph_file in RMAT-Data-sk-5-nV-4000000-nE-100000000.fascia RMAT-Data-sk-5-nV-4000000-nE-200000000.fascia RMAT-Data-sk-5-nV-4000000-nE-300000000.fascia
# for graph_file in RMAT-Data-sk-8-nV-4000000-nE-100000000.fascia RMAT-Data-sk-8-nV-4000000-nE-200000000.fascia RMAT-Data-sk-8-nV-4000000-nE-300000000.fascia
do
    # template files
	# for template_file in u3-1.fascia
	# for template_file in u10-2.fascia
	# for template_file in u12-2.fascia
	# for template_file in u13.fascia
	# for template_file in u14.fascia
	# for template_file in u15-1.fascia
	# for template_file in u15-2.fascia
	# for template_file in u16-1.fascia
	# for template_file in u17-1.fascia
	# for template_file in u12-2.fascia u13.fascia u14.fascia u15-1.fascia u15-2.fascia
    do
        # num of threads (per node)	
        for tpproc in 48
        do
            export OMP_NUM_THREAD=${tpproc}

            graph_name=$(echo "${graph_file}" | cut -d'.' -f1 )

                        ### read in text or binary file
                        graph_type=$(echo "${graph_file}" | cut -d'.' -f2)
                        if [ "$graph_type" == "data" ];then
                            read_binary=1
                            echo "read in binary file"
                        else
                            read_binary=0
                            echo "read in text file"
                        fi

                        template_name=$(echo "${template_file}" | cut -d'.' -f1 )

                        ## use the icc compiled binary for skl
                        for app in sc-skl-icc.bin 
                        do
                            ## 1 is the pfascia 
                            ## 2 is the fascia 
                            # for algoMode in 1 2
                            # for algoMode in 1
                            for algoMode in 2
                            do
                                logName=FASCIA-${version}-${graph_name}-${template_name}-Thd-${tpproc}-Mode-${algoMode}.log
                                ${app_dir}/$app ${graph_loc}/${graph_file} ${template_loc}/${template_file} ${Itr} ${tpproc} ${read_binary} ${writeBinary} ${algoMode} 2>&1 | tee ${log_dir}/${logName}
                            done
                        done
                    done # num of threads

                done # template files

                if [ "$writeBinary" == "1" ];then
                    mv graph.data ${graph_name}-binary.data 
                fi

            done # input graph

