
save()
{
#[LightGBM] [Info] 50.857357 seconds elapsed, finished iteration 100
	ret=`grep "finished iteration 100" $1.log |grep -Po "[0-9]*\.[0-9]*" | gawk '{printf("%s,",$1)}'`

	echo $1,$ret >> $output

	#echo >> $output
	#echo >> $output

}

output=lightgbm_result.csv


runit()
{
#
#feature, thread
#
logfile=lgbm_"$1"-"$2"
if [ ! -f $logfile.log ]; then
	./lightgbm config=higgs_lightgbm.conf num_trees=100 tree_learner=$1 nthread=$2 2>&1 |tee $logfile.log
fi

save $logfile 
}


runit feature 28
runit feature 14
runit feature 7
runit data 28
runit data 14
runit data 7



