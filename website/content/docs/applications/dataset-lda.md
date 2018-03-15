# datasets for lda

**nytimes,pubmed:**

standard text dataset in the UCI repository.

https://archive.ics.uci.edu/ml/datasets/bag+of+words


**enwiki,bigram:**

Articles in English in the 18-May-2015 dump.
First, download the dump of all Wikipedia articles from http://download.wikimedia.org/enwiki/ (you want a file like enwiki-latest-pages-articles.xml.bz2). This file is about 8GB in size and contains (a compressed version of) all articles from the English Wikipedia.

Preprocess the wikipedia documents by gensim toolkits, and calculate the tf-idf value for each documen-word pair and create this dataset useable for mf problems.

Bigram version as bigram.


**clueweb**

http://lemurproject.org/clueweb09.php/

http://boston.lti.cs.cmu.edu/clueweb09/wiki/tiki-index.php?page=Dataset+Information

The ClueWeb09 dataset was created to support research on information retrieval and related human language technologies. It consists of about 1 billion web pages in ten languages that were collected in January and February 2009. The dataset is used by several tracks of the TREC conference.

We select sbuset of english webpages from the collection, and preprocess to convert it into a tf-idf document-word matrix.
There are different versions, one is clueweb10b with 10b training examples, the other one is clueweb30b.



