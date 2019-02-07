methods=(feature data)
ids=(0 1 2)

#depths=(8 12 16)
#dbs=(higgs synset)

depths=(8 12 16)
dbs=(higgs)
for dataset in ${dbs[*]}; do
	for tree_method in ${methods[*]}; do
		for depth in ${depths[*]}; do
			for id in ${ids[*]}; do
			#./lightgbm-speedup.sh ./lightgbm higgs 10 $depth feature 32 ${id} 
			../bin/lightgbm-speedup.sh ./lightgbm ${dataset} 10 $depth ${tree_method} 32 ${id} 
			done
		done
	done
done

depths=(8 12 13 14)
dbs=(synset)
for dataset in ${dbs[*]}; do
	for tree_method in ${methods[*]}; do
		for depth in ${depths[*]}; do
			for id in ${ids[*]}; do
			#./lightgbm-speedup.sh ./lightgbm higgs 10 $depth feature 32 ${id} 
			../bin/lightgbm-speedup.sh ./lightgbm ${dataset} 10 $depth ${tree_method} 32 ${id} 
			done
		done
	done
done

