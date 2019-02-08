LargeModel
===============

to run large model by reduce memory footprint.

Because the codebase is based on xgboost, in which is very hard to release the data matrix loaded from the input file,
two parameters now are added to save and load the inialized data matrix to/from file.
Then the unnecessary memory usages are now freed.

YFCC-SUB now can run d12 on juliet node~


