echo 'performing random forests'
echo 'with 4 trees, 1 processor, and randomly picking 80 of the 700 features for each tree'
echo 'command="python do_forest.py data/small.txt data/small.txt 700 80 4 1 > data/small-forest.preds"'
echo '---'
python do_forest.py data/small.txt data/small.txt 700 80 4 1 > data/small-forest.preds
echo '---'
echo 'saved predictions to data/small-forest.preds'
echo '---'
echo 'to do random forests with classification, call do_forest-class.py with the same parameters'