mkdir airline
cd airline/
mkdir test
mkdir bin
cd test
cp /share/jproject/fg474/share/optgbt/singlenode/airline_test/aireline*.libsvm .
cp /share/jproject/fg474/share/optgbt/singlenode/airline_test/aireline*.conf .

cp /share/jproject/fg474/share/optgbt/singlenode/criteo-j114/test/* .
cp /share/jproject/fg474/share/optgbt/singlenode/criteo-j114/bin/* ../bin

python -m runner.makepseudodb --input airline_train.libsvm --output airline_pseudo.libsvm
../bin/xgboost-g++-omp-dense-halftrick-byte-splitonnode-unity-release airline_eval.conf num_round=10 nthread=32 tree_method=lossguide max_depth=0 bin_block_size=0 row_block_size=312500 node_block_size=16 grow_policy=lossguide max_leaves=4096 ft_block_size=0 data_parallelism=1 group_parallel_cnt=32 topk=32 async_mixmode=1 savemeta=airlinemeta

rename criteometa airlinemeta criteome*

sed -i 's/criteometa/airlinemeta/g' run*.sh
sed -i 's/1562500/312500/g' run*.sh
sed -i 's/33/8/g' run*.sh

ls run*.sh >runallblock_airline_optimal.sh
