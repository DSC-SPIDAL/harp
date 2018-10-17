cols=(50 100 200 500)
rows=(1000000 5000000 10000000)

for col in ${cols[*]}; do
for row in ${rows[*]}; do

	#python -m runner.xgb_benchmark --rows 1000000 --columns 50
	python -m runner.xgb_benchmark --rows $row --columns $col --iterations 1

	cp train.csv train-${row}m-${col}.csv
	cp test.csv test-${row}m-${col}.csv
	cp dtrain.dm train-${row}m-${col}.dm
	cp dtest.dm test-${row}m-${col}.dm

done
done

