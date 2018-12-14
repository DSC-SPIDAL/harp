
save()
{
ret=`tail -7 $1.log |grep -Po "[0-9]*\.[0-9]*" | gawk '{printf("%s,",$1)}'`

echo $1,$ret >> $output

echo >> $output
echo >> $output
}


output=xgb-synset-treemethod-cmp.csv


logfile=synset_bnf_16x16
../bin/xgboost-g++-omp-halftrick-noprefetch-bnf-resort synset_block.conf bin_block_size=16 num_round=10 nthread=16 2>&1 | tee ${logfile}.log

save $logfile


logfile=synset_hist_16
../bin/xgboost-g++-omp-halftrick-noprefetch-bnf-resort synset_block.conf bin_block_size=16 num_round=10 nthread=16 tree_method=hist 2>&1 | tee ${logfile}.log

save $logfile


logfile=synset_fnb_16
../bin/xgboost-g++-omp-nohalftrick-noprefetch-fnb synset_block.conf bin_block_size=16 num_round=10 nthread=16 2>&1 | tee ${logfile}.log

save $logfile

