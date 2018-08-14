echo 'calling random forest with classification, saving class predictions to data/fc.cpred[1234]'
echo 'command="python do_forest-class.py data/small.txt data/small.txt 700 80 5 1 data/fc.cpred > data/small-forest-class.pred"'
echo '---'
python do_forest-class.py data/small.txt data/small.txt 700 80 5 1 data/fc.cpred > data/small-forest-class.pred
echo '---'
echo 'boost from the classification results'
echo 'save predictions to data/small-igbrt-class.pred'
echo 'command="python do_boost-class.py data/small.txt data/small.txt 700 4 10 0.1 1 data/fc.cpred > data/small-igbrt-class.pred"'
echo '---'
python do_boost-class.py data/small.txt data/small.txt 700 4 10 0.1 1 data/fc.cpred > data/small-igbrt-class.pred
echo '---'

