
#
# depthwise d8
#
../bin/xgboost-g++-omp-dense-halftrick-short-splitonnode-fitmem-release yfcc_metaeval.conf num_round=10 nthread=32 tree_method=lossguide max_depth=8 bin_block_size=0 ft_block_size=32 row_block_size=500000 node_block_size=65536 loadmeta=yfccmeta

#
# depthwise d12
#
  ../bin/xgboost-g++-omp-dense-halftrick-short-splitonnode-fitmem-release yfcc_metaeval.conf num_round=10 nthread=32 tree_method=lossguide max_depth=12 bin_block_size=0 ft_block_size=32 row_block_size=500000 node_block_size=65536 loadmeta=yfccmeta

#
# lossguide d8
#
  ../bin/xgboost-g++-omp-dense-halftrick-short-splitonnode-fitmem-release yfcc_metaeval.conf num_round=10 nthread=32 tree_method=lossguide max_depth=0 bin_block_size=0 ft_block_size=32 row_block_size=500000 node_block_size=8 loadmeta=yfccmeta grow_policy=lossguide max_leaves=256
