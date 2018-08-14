echo 'running boosting script'
echo 'command="python do_boost.py data/small.txt data/small.txt 700 4 10 0.1 1 > data/small-boost.preds"'
echo 'boost on data with 700 features, using trees with depth 4, for 10 iterations, with 0.1 learning rate, and 1 processor'
echo '---'
python do_boost.py data/small.txt data/small.txt 700 4 10 0.1 1 > data/small-boost.preds
echo '---'
echo 'predictions for data/small.txt stored in data/preds'
echo '---'
echo 'to boost using classification, call do_boost-class.py with the paramaters'
