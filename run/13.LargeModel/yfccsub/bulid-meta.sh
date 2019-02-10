#
# first, save the meta
# it runs on knl node, need about 134GB memory
#
../bin/xgboost-g++-omp-dense-halftrick-short-splitonnode-fitmem-release yfcc.conf num_round=10 nthread=32 tree_method=lossguide max_depth=6 bin_block_size=0 ft_block_size=32 row_block_size=500000 node_block_size=65536 savemeta=yfccmeta

#
# second, make the pseudo input file
#
python -m runner.makepseudodb --input yfcc_sub_train.libsvm --output yfcc_sub_pseudo.libsvm

#
# now, you get the meta image files
# 
# -rw-r--r--. 1 pengb users 8192004176 Feb  8 15:15 yfccmeta.blkmat
# -rw-r--r--. 1 pengb users 4096114768 Feb  8 15:15 yfccmeta.hmat
# -rw-r--r--. 1 pengb users    8213488 Feb  8 15:15 yfccmeta.meta
#
#
../bin/xgboost-g++-omp-dense-halftrick-short-splitonnode-fitmem-release yfcc_metaeval.conf num_round=10 nthread=32 tree_method=lossguide max_depth=8 bin_block_size=0 ft_block_size=32 row_block_size=500000 node_block_size=65536 loadmeta=yfccmeta


