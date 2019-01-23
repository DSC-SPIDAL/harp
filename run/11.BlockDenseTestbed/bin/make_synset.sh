#~/bin/bash

python -m runner.gensynset --rows 1000000 --columns 1024
mv synset_train_1000000x1024_0.000000.libsvm synset_train.libsvm

head -1 synset_train.libsvm >synset_valid.libsvm


