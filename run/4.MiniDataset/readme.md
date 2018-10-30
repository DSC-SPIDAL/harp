minidataset
===============

@goal: build a mini dataset to validate the correctness in debug mode, e.g, print all the trees 

@10302018

### references results

```
[pengb@j-030 mini]$ ./runfourclass.sh xgboost-orig-vtune num_round=10 max_depth=2
2018-10-30 12:00:00,243 : INFO : auc = 0.720623

[pengb@j-030 mini]$ ./runfourclass.sh xgboost-orig-vtune num_round=10 max_depth=1
2018-10-30 12:00:28,430 : INFO : auc = 0.669566

[pengb@j-030 mini]$ ./runfourclass.sh xgboost-orig-vtune num_round=50 max_depth=1
2018-10-30 11:53:24,654 : INFO : auc = 0.762129
```
