Scaling Test
================

Test strong scaling in multi-threading env for different implemnetations

### tools:

    xgboost     tree_method=hist
    lightgbm    tree_learner=data, feature
    
    xgb-fnb     ; block-based trainer with block <0,1,0>  (rows, feature, bin)
    xgb-bnf     ; block-based trainer with block <0,0,K>  (rows, feature, bin) , K=bin_block_size

### dataset:

    data dir: /share/jproject/fg474/share/optgbt/vtune/

    higgs       ; higgs/higgs_train.libsvm
    synset128   ; synset2/synset_train_10000000x128.libsvm
    synset1024  ; synset2/synset_train_1000000x1024.libsvm




