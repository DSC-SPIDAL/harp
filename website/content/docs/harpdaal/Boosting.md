---
title: Boosting 
---

Boosting is a group of algorithms aiming to construct a strong classifier from a set of weighted weak classifiers 
through iterative re-weighting based on accuracy measurement for weak classifiers. A weak classifier typically has only slightly better 
performance than random guessing, which are very simple, fast, and focusing on specific feature classification. 

Harp-DAAL supports batch modes of the following three boosting algorithms.

## AdaBoost Classifier

AdaBoost algorithm performs well on a variety of data sets except some noisy data [^fn1]. 
More details are from [Intel DAAL Documentation](https://software.intel.com/en-us/daal-programming-guide-details-29)

AdaBoost in Harp-DAAL is a binary classifier.
 
## BrownBoost Classifier 

BrownBoost is a boosting classification algorithm. It is more robust to noisy data sets than other boosting classification algorithms.
More details are from [Intel DAAL Documentation](https://software.intel.com/en-us/daal-programming-guide-details-30)

BrownBoost in Harp-DAAL is a binary classifier.

## LogitBoost Classifier

LogitBoost and AdaBoost are close to each other in the sense that both perform an additive logistic regression. The difference is that AdaBoost minimizes the exponential loss, whereas LogitBoost minimizes the logistic loss.

LogitBoost in Harp-DAAL is a multi-class classifier. 

More details are from [Intel DAAL Documentation](https://software.intel.com/en-us/daal-programming-guide-details-31)

[^fn1]: Yoav Freund, Robert E. Schapire. Additive Logistic regression: a statistical view of boosting. Journal of Japanese Society for Artificial Intelligence (14(5)), 771-780, 1999.
