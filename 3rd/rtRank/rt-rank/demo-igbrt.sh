echo 'running random forests, save results'
python do_forest.py data/small.txt data/small.txt 700 80 4 1 > data/small-rf.pred
echo 'run boosting from RF predictions'
python do_boost.py data/small.txt data/small.txt 700 4 10 0.1 1 data/small-rf.pred > data/small-igbrt.pred