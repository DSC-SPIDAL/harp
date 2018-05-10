---
title: MLR Tutorial for Beginners
---

Multiclass logistic regression is an algorithm that uses logistic regression to apply to multi-class problems when classifying. Typically, in MLR, there are two discrete outcomes, which means that it is a model that is used to predict the probabilities of the different possible outcomes of a categorically distributed dependent variable, given a set of independent variables, which may be real-valued, binary-valued, categorical-valued, etc. Each possible result is of a categorically distributed dependent variable. Multiclass logistic regression is used when the dependent variable in problem is a classification and for which there are more than two categories. The best values of the parameters for a given problem are usually determined from some training data.

To solve multiclass problem, One-versus-Rest (OvR), also known as one-vs-all, consists in fitting one classifier per class. For each classifier, the class is fitted against all the other classes. In addition to its computational efficiency (only n_classes classifiers are needed), one advantage of this approach is its interpretability. Since each class is represented by one and one classifier only, it is possible to gain knowledge about the class by inspecting its corresponding classifier. This is the most commonly used strategy for multiclass classification and is a fair default choice.
In this tutorial, one-versus-rest is used in our code, in which 103(the total number of categories/codes) independent binary classifiers are trained.

## What kinds of Data is needed?
MLR needs a dependent variable that is a categorical data, for example, race, sex, age group, and educational level, as well as a set of independent variables, which are used to predict the dependent variable. Multinomial logistic regression is a particular solution to the classification problem that assumes that a linear combination of the observed features and some problem-specific parameters can be used to determine the probability of each particular outcome of the dependent variable.




# **Dataset Description**

## **The Documents**
Reuters is the largest international text and television news agency. Its editorial division produces some 11,000 stories a day in 23 languages. Stories are both distributed in real time and made available via online databases and other archival products.
RCV1 is drawn from one of those online databases. It was intended to consist of all and only English language stories produced by Reuters journalists between August 20, 1996, and August 19, 1997. The stories cover the range of content typical of a large English language international newswire. They vary from a few hundred to several thousand words in length. Figure 1 shows an example story (with some simplification of the markup for brevity).


![](https://d2mxuefqeaa7sj.cloudfront.net/s_80744BEE5C676152B1FCF01CB78E2B069F35AB9167E06F4ED3D6F8DCBD252FD4_1524252439284_Screen+Shot+2018-04-20+at+3.26.55+PM.png)


## **The Categories**
To aid retrieval from database products such as Reuters Business Briefing (RBB), category codes, such as, “C15, USA, I34420, CCAT” (Figure 1) from three sets (Topics, Industries, and Regions) were assigned to stories. The code sets were originally designed to meet customer requirements for access to corporate/business information, with the main focus on company coding and associated topics. With the introduction of the RBB product the focus broadened to the end user in large corporations, banks, financial services, consultancy, marketing, advertising and PR firms.


The RCV1 text collection (Figure 1) is first transformed into a dataset of feature vectors(shown below) through preprocessing steps, such as stop word removal, tokenization, punctuation removal, stemming and term weighting. Please refer to [RCV1-v2/LYRL2004: The LYRL2004 Distribution of the RCV1-v2 Text Categorization Test Collection](http://www.ai.mit.edu/projects/jmlr/papers/volume5/lewis04a/lyrl2004_rcv1v2_README.htm) for more details.

Here’s what the [dataset](https://github.com/DSC-SPIDAL/harp/tree/master/datasets/tutorial/rcv1) should look like:
```bash
2286  864:0.0497399253756197 1523:0.044664135988103 ... 46545:0.0146733593899118 46694:0.0409422676712255
```
Format:
*\<did\> [\<tid\>:\<weight\>]+*

- *\<did\>* : Reuters-assigned document id.  (Example: 2286)
- *\<tid\>*: A positive integer term id. Term ids are between 1 and 47,236. (Example: 864)
- *\<weight\>* : The numeric feature value, i.e. within document weight, assigned to this term for this document. (Example: 0.0497399253756197)





# Compile
<mark>To be able to compile and run, you have to [install Harp and Hadoop](https://dsc-spidal.github.io/harp/docs/getting-started/)</mark>:


Select the profile related to your hadoop version. For ex: hadoop-2.6.0. Supported hadoop versions are 2.6.0, 2.7.5 and 2.9.0
```bash
cd $HARP_ROOT_DIR
mvn clean package -Phadoop-2.6.0

cd $HARP_ROOT_DIR/contrib/target
cp contrib-0.1.0.jar $HADOOP_HOME
cd $HADOOP_HOME
```

Put data on hdfs
```bash
hdfs dfs -mkdir /rcv1v2
rm -rf data
mkdir -p data
cd data
split -l 1000 $HARP_ROOT_DIR/datasets/tutorial/rcv1/lyrl2004_vectors_train.dat
cd ..
hdfs dfs -put data /rcv1v2
hdfs dfs -put $HARP_ROOT_DIR/datasets/tutorial/rcv1/rcv1* /rcv1v2
```
# Run
```bash
hadoop jar contrib-0.1.0.jar edu.iu.mlr.MLRMapCollective [alpha] [number of iteration] [number of features] [number of workers] [number of threads] [topic file path] [qrel file path] [input path in HDFS] [output path in HDFS]
```
For this dataset:
```bash
hadoop jar contrib-0.1.0.jar edu.iu.mlr.MLRMapCollective 1.0 100 47236 2 4 /rcv1v2/rcv1.topics.txt /rcv1v2/rcv1-v2.topics.qrels /rcv1v2 /output
```

# Result

The output should be the weight matrix `W` in the weight file. To view the result:
```bash
cd $HADOOP_HOME
cd output/
vim weights
```
To get to the analysis and accuracy of the result :
```bash
cd $HADOOP_HOME
cd output/
vim evaluation
```
evaluation:
```bash
cat    tp      fn      fp      tn      f1
C11:   655.0   19.0    13.0    22462.0 0.9761549925477078
G156:  0.0     2.0     0.0     23147.0 0.0
...
M141:  1501.0  7.0     3.0     21638.0 0.9966799468788191
CCAT:  10740.0 46.0    39.0    12324.0 0.9960584280083007
microF1 : 0.9922454324036446
macroF1 : 0.9545153419381115
```
- cat: categories
- tp: True positives
- fn: False negatives
- fp: False positives
- tn: True negatives
- F1: F-score = 2TP/(2TP + FP + FN)

MicroF1 and MacroF1 provide the accuracy of the algorithm on this data set.

- Microaverage measures are dominated by high frequency categories.
- Macroaverage gives equal weight to each category, and thus is dominated by effectiveness on low frequency categories.
