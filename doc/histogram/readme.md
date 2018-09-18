Histogram
==============


XGBoost[1] provides an approximate framework that resembles the ideas proposed in past literature.
pGBRT[2] directly construct approximate histograms of gradient statistics based on the idea from the streaming decision tree paper[3].
In the community of database and data mining, the approximate answer of quantile queries is a thoroughly studied topic.  XGBoost extended GK algorithm [4][5] in its implementation. LightGBM[6] mentioned another classic work based on histogram[7, 8]. However, McRank[8] uses other variants of binning strategies instead of quantile.

The bin size of the histogram, B, was not discussed much in the context of GBT. pGBRT[2] set  B from 25, 50 to 100 for the learn to rank datasets. TencentBoost[9] and DimBoost[10] work on datasets with high dimensionality, they set B to 20 and 100 in the experiments.


### References

+ [1]T. Chen and C. Guestrin, “Xgboost: A scalable tree boosting system,” in Proceedings of the 22nd acm sigkdd international conference on knowledge discovery and data mining, 2016, pp. 785–794.
+ [2]S. Tyree, K. Q. Weinberger, K. Agrawal, and J. Paykin, “Parallel boosted regression trees for web search ranking,” in Proceedings of the 20th international conference on World wide web, 2011, pp. 387–396. [code: https://github.com/dophist/pGBRT ]
+ [3]Y. Ben-Haim and E. Tom-Tov, “A streaming parallel decision tree algorithm,” Journal of Machine Learning Research, vol. 11, no. Feb, pp. 849–872, 2010.
+ [4]M. Greenwald and S. Khanna, “Space-efficient online computation of quantile summaries,” in ACM SIGMOD Record, 2001, vol. 30, pp. 58–66.
+ [5]Q. Zhang and W. Wang, “A fast algorithm for approximate quantiles in high speed data streams,” in Scientific and Statistical Database Management, 2007. SSBDM’07. 19th International Conference on, 2007, pp. 29–29.
+ [6]G. Ke et al., “LightGBM: A Highly Efficient Gradient Boosting Decision Tree,” in Advances in Neural Information Processing Systems, 2017, pp. 3149–3157.
+ [7]R. Jin and G. Agrawal, “Communication and memory efficient parallel decision tree construction,” in Proceedings of the 2003 SIAM International Conference on Data Mining, 2003, pp. 119–129.
+ [8]P. Li, Q. Wu, and C. J. Burges, “Mcrank: Learning to rank using multiple classification and gradient boosting,” in Advances in neural information processing systems, 2008, pp. 897–904.
+ [9]J. Jiang, J. Jiang, B. Cui, and C. Zhang, “TencentBoost: A Gradient Boosting Tree System with Parameter Server,” in 2017 IEEE 33rd International Conference on Data Engineering (ICDE), 2017, pp. 281–284.
+ [10]J. Jiang, B. Cui, C. Zhang, and F. Fu, “DimBoost: Boosting Gradient Boosting Decision Tree to Higher Dimensions,” in Proceedings of the 2018 International Conference on Management of Data, 2018, pp. 1363–1376.
