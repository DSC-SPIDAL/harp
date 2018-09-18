mpirun -np 2 ../bin/pgbrt train.dat 70 700 4 100 0.1 -V valid.dat -v 30 -m > out.log
cat out.log | python ../scripts/crossval.py 5 -r | python ../scripts/compiletest.py test
cat test.dat | ./test > test.pred
python ../scripts/evaluate.py test.dat test.pred