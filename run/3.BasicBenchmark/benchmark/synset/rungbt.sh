cols=(50 100 200)
rows=(1000000 5000000 10000000)

for col in ${cols[*]}; do
for row in ${rows[*]}; do

	##python -m runner.xgb_benchmark --rows 1000000 --columns 50
	#rm -f dtrain.dm dtest.dm
	#ln -s train-${row}m-${col}.dm dtrain.dm
	#ln -s test-${row}m-${col}.dm dtest.dm

	#python -m runner.xgb_benchmark --rows $row --columns $col --iterations 300 --tree_method exact

	#daal
	rm -f train.csv test.csv
	ln -s train-${row}m-${col}.csv train.csv
	ln -s test-${row}m-${col}.csv test.csv
	./daalgbt train.csv test.csv 300 2 ${col} 6 0 0 

done
done

