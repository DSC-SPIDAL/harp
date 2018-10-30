#
# biuld dataset from fourclass
#

wget https://www.csie.ntu.edu.tw/~cjlin/libsvmtools/datasets/binary/fourclass -O fourclass.raw

sed 's/-1/0/' fourclass.raw > fourclass.libsvm 
head -50 fourclass.libsvm >fourclass_train.libsvm
tail -797 fourclass.libsvm >fourclass_test.libsvm
head -1 fourclass.libsvm >fourclass_valid.libsvm
sed 's/ 1:/ /' fourclass_test.libsvm |sed 's/ 2:/ /' | sed 's/+//' |gawk '{printf("%s,%s,%s,\n",$2,$3,$1)}' >fourclass_test.csv
