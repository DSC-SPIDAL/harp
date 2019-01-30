depths=(8 12 16)

for depth in ${depths[*]}; do

../bin/lightgbm-scaling.sh ./lightgbm higgs 10 $depth feature 
#./lightgbm-scaling.sh ./lightgbm synset 10 $depth feature 32 0 

done

