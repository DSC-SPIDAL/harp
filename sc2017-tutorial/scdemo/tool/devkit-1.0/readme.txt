=================================================
Introduction
=================================================

This is the documentation of the ILSVRC2010 Development Kit.

Please contact ilsvrc2010@image-net.org for questions, comments,
or bug reports.


=================================================
Data
=================================================

There are three types of image data for this competition: training
data from ImageNet (TRAINING), validation data specific to this
competition (VALIDATION), and test data specific to this competition
(TEST).  There is no overlap in the three sources of data: TRAINING,
VALIDATION, and TEST.  All three sets of data contain images of 1000
categories of objects.  The categories correspond 1-1 to a set of 1000
synsets (sets of synonymous nouns) in WordNet.  An image is in a
particular category X, where X is a noun synset, if the image contains
an X. See [1] for more details of the collection and
labeling strategy.

The 1000 synsets are selected such that there is no overlap between
synsets, for any sysnets i and j, i is not an ancestor of j in the
WordNet hierarchy. We call these synsets "low level synsets".

Those 1000 synsets are part of the larger ImageNet hierarchy and we
can consider the subset of ImageNet containing the 1000 low level
synsets and all of their ancestors. There are 676 such ancestor
synsets, which we refer to as "high level synsets". In this hierarchy,
all the low level synsets are "leaf" nodes and the high level synsets
are "internal" nodes.

Note that the low level synsets may have children in ImageNet, but for
ILSVRC 2010 we do not consider their child subcategories. The
hierarchy here can be thought of as a "trimmed" version of the
complete ImageNet hierarchy.

Also note that for this competition, all ground truth labels are low
level synsets and entries must predict labels corresponding to one of
the 1000 low level synsets.  Predicting high level synsets is not
considered. There are no additional training images for high level
synsets.

*** Meta Data

All information on the synsets is in the 'synsets' array in data/meta.mat.
To access it in Matlab, type

  load data/meta.mat;
  synsets

and you will see

   synsets =

   1676x1 struct array with fields:
       ILSVRC2010_ID
       WNID
       words
       gloss
       num_children
       children
       wordnet_height
       num_train_images

Each entry in the struct array corresponds to a synset,i, and contains
fields:

'ILSVRC2010_ID' is an integer ID assigned to each synset. All the low
level synsets are assigned to an ID between 1 and 1000. All the high
level synsets have ID higher than 1000. The synsets are sorted by
ILSVRC2010_ID in the 'synsets' array, i.e.  synsets(i).ILSVRC2010_ID
== i. For submission of prediction results, ILSVRC2010_ID is used as
the synset labels.

'WNID' is the WordNet ID of a synset. It is used to uniquely identify
a synset in ImageNet or WordNet.  The tar files for training images
are named using WNID. Also it is used in naming individual training
images.

'num_children' is the number of the children in this trimmed
hierarchy. It is zero for all low level synsets and non-zero for high
level synsets.

'children' is an vector listing the ILSVRC2010_IDs of child synsets.

'wordnet_height' is the length of the longest path to a leaf node in
the FULL ImageNet/WordNet hierarchy (leaf nodes in the FULL ImageNet
hierarchy have wordnet_height zero). It is used to measure the
semantic distance between two synsets and to define the hierarchical
misclassification cost.

The same file, data/meta.mat, contains information about the
hierarchical cost of misclassifying images of one category as coming
from another category.

For the hierarchical criteria, the cost of one misclassification,
d(x,y), is defined as the height of the lowest common ancestor of x
and y in the FULL ImageNet hierarchy.  The height of a node is the
length of the longest path to a leaf node (leaf nodes have height
zero).

The 'cost_matrix' matrix in data/meta.mat stores the precomputed
pairwise cost for all low level synsets. The columns are ordered by
ILSVRC2010_ID, i.e. the cost of classifying synset j as synset i is
cost_matrix(i,j), where i and j are the ILSVRC2010_IDs.

The 'cost_matrix' matrix is used in the evaluation routine for the
hierarchical criteria.

The ILSVRC2010_ID of the root of the hierarchy is 1001, the synset
"entity".


*** Training images

The training images are the same images in the ImageNet 2010 Spring Release. 
You can browse the images of a synset at
    http://www.image-net.org/synset?wnid=WNID
where WNID is the WordNet ID of the synset, as in the meta file.

There is a tar file for each synset, named by its WNID. The image files are named 
as x_y.JPEG, where x is WNID of the synset and y is an integer (not fixed width and not
necessarily consecutive). All images are in JPEG format. 

There are a total of 1,261,406 images for training. The number of images for each 
synset ranges from 668 to 3047. 


*** Validation images

There are a total of 50,000 validation images. They are named as

      ILSVRC2010_val_00000001.JPEG
      ILSVRC2010_val_00000002.JPEG
      ...
      ILSVRC2010_val_00049999.JPEG
      ILSVRC2010_val_00050000.JPEG

There are 50 validation images for each synset.

The ground truth of the validation images is in 
    data/ILSVRC2010_validation_ground_truth.txt,
where each line contains one ILSVRC2010_ID for one image, in the ascending alphabetical 
order of the image file names.



*** Test images

There are a total of 150,000 test images, which will be released separately at a later
time. The test files are named as

      ILSVRC2010_test_00000001.JPEG
      ILSVRC2010_test_00000002.JPEG
      ...
      ILSVRC2010_test_00149999.JPEG
      ILSVRC2010_test_00150000.JPEG

There are 150 test images for each synset.

Ground truth of the test images will not be released during the competition.



*** Features

We have computed dense SIFT[2] features for all images, and they are available for 
download. 

Each image is re-sized to have a max side length of 300 pixel (smaller images are not
enlarged). SIFT descriptors are computed on 20x20 overlapping patches with 
a spacing of 10 pixels. Images are further downsized (to 1/2 the side length 
and then 1/4 of the side length) and more descriptors are computed. 
We use the VLFeat[3] implementation of dense SIFT (version 0.9.4.1).

We then perform k-means clustering of a random subset of 10 million SIFT 
descriptors to form a visual vocabulary of 1000 visual words. Each SIFT 
descriptor is quantized into a visual word using the nearest cluster center.

We provide both raw SIFT features (vldsift) and the visual codewords (sbow).
 Spatial coordinates of each descriptor/codeword are also included. 

For training images, there is one .mat file for each synset. For the raw
synset features, it is an structure array 'image_vldsift' with fields 'ID' and
'vldsift', where 'ID' is the image file name (excluding the format extension).
For example, if we load n03710721.vldsift.mat in Matlab,

>> image_vldsift(5)

ans =

         ID: 'n03710721_67'
    vldsift: [1x1 struct]


The 'vldsift' field is a struct with fields 'x', 'y, 'scale', 'norm' and 'desc'.
The 'scale' field indicates the scale at which the descriptor is computed. It is 
either 0 ( finest ), 1 ( 1/2 downsized ), or 2 ( 1/4 downsized ). The 'desc' field 
is a 128 dimensional L2 normalized float vector. For example, for

>> image_vldsift(5).vldsift

ans =

        x: [1x779 single]
        y: [1x779 single]
    scale: [1x779 int8]
     norm: [1x779 single]
     desc: [128x779 single]

Here the image 'n03710721_67' has 779 SIFT points. For the ith point,
its position is at (x(i),y(i)) and its descriptor is a L2 normalized 128 dimensional
vector desc(:,i). The 'scale' field indicates the scale at which the descriptor 
is computed. It is either 0 ( finest ), 1 ( 1/2 downsized ), or 2 ( 1/4 downsized ).

The quantized codewords are in similar format. For example, after loading 
n03710721.sbow.mat, 

>> image_sbow(5).sbow

ans =

     word: [1x779 int16]
        x: [1x779 single]
        y: [1x779 single]
    scale: [1x779 int8]
     norm: [1x779 single]

where the 'desc' field is replaced by the 'word' field, which is index of the codebook,
i.e. an integer between 0 and 999. 

For validation images, there is one Matlab file for every 1,000 images, named as follows:

    val.0001.sbow.mat // for images ILSVRC2010_val_00000001.JPEG to ILSVRC2010_val_00001000.JPEG
    val.0002.sbow.mat // for images ILSVRC2010_val_00001001.JPEG to ILSVRC2010_val_00002000.JPEG

    val.0050.sbow.mat // for images ILSVRC2010_val_00049001.JPEG to ILSVRC2010_val_00050000.JPEG

Features for testing images will be packaged similarly.


=================================================
Code for computing SIFT features ( LINUX )
=================================================

We provide the code used for computing the provided SIFT features. It uses
the VLFeat[3] library. 

It takes as input a text file listing the file names (one per line),
a text file listing the file IDs (the 'ID' field for each image in the Matlab
structure) and outputs a raw SIFT (vldsift) Matlab file and a quantized codeword (sbow)
Matlab file. 

To try an example,
       (1) Compile VLFeat:
		   cd feature/3rd-party/vlfeat
		   make
       (2) In the 'feature' directory, type
                   make
                   ./extract_sbow.sh demo.input demo.input demo.output

It will extract SIFT features for img1.jpg and img2.jpg, and output
demo.output.sbow.mat and demo.output.vldsift.mat. 

You might need to modify the linking path in VL_LFLAGS in ./feature/Makefile, 
depending on your platform. Also you might need to set MATLABPATH in 
./feature/3rd-party/vlfeat/Makefile.


=================================================
Demo system
=================================================
We provide a demo system using the precomputed SIFT features. 

We train one-against-all classifiers with LIBLINEAR[4] on bag of words
histograms, using 100 images per synset. We then test on validation 
data, reporting the top 5 classifications for each image.

To try the demo system, 

(1)Download the visual words features (sbow) for training and validation.
Create folders ./data/features/train and ./data/features/val, and
untar the mat files to ./data/features/train and ./data/features/val 
respectively. 

(2)Compile LIBLINEAR:
      cd demo/3rd-party/liblinear-1.5/matlab
      make

You might need to change the MATLABDIR variable in demo/3rd-party/liblinear-1.5/matlab/Makefile
to your actual Matlab installation path.
	   
(3)Start Matlab in the 'demo' folder, type
      demo;

The demo system should take less than 1 hour to finish. It will output the error
scores on the validation data at the end:

    # guesses  vs flat error
	1.0000    0.9101
	2.0000    0.8716
	3.0000    0.8422
	4.0000    0.8191
	5.0000    0.8003

    # guesses vs hierarchical error
	1.0000   10.3468
	2.0000    8.7489
	3.0000    7.8510
	4.0000    7.2221
	5.0000    6.7695


=================================================
Submission and evaluation
=================================================

**** Submission format:

The 150,000 test images will be in the same format, i.e. from 
ILSVRC2010_test_00000001.JPEG to ILSVRC2010_test_0150000.JPEG. 

Submission of results on test data will consist of a text file 
with one line per image, in the alphabetical order of the image 
file names, i.e. from ILSVRC2010_test_00000001.JPEG to 
ILSVRC2010_test_0150000.JPEG. Each line contains the predicted 
labels, i.e. the ILSVRC2010_IDs ( an integer between 1 and 1000 ) 
of the predicted categories, sorted by confidence in descending 
order. The number of labels per line can vary but must be no more 
than 5.

For this competition, you will submit two result files, one for 
each criteria (flat and hierarchical). You can also submit the 
same file for both.

An example file on the validation data is 

        ./evaluation/demo.val.pred.txt

which is the result produced by our demo system.



**** Evaluation routines

The Matlab routines for evaluating the submission are 

./evaluation/eval_flat.m  and 
./evaluation/eval_hie.m,

for flat cost and hierarchical cost respectively. 

To see an example of using the routines, start Matlab
in the 'evaluation' folder and type
       demo_eval;

and you will see the following output:

    pred_file =

    demo.val.pred.txt


    ground_truth_file =

    ../data/ILSVRC2010_validation_ground_truth.txt

    # guesses  vs flat error
	1.0000    0.9101
	2.0000    0.8716
	3.0000    0.8422
	4.0000    0.8191
	5.0000    0.8003

    # guesses vs hierarchical error
	1.0000   10.3468
	2.0000    8.7489
	3.0000    7.8510
	4.0000    7.2221
	5.0000    6.7695

In this demo, we take top i ( i=1...5) predictions (and ignore the 
rest) from your result file and plot the error as a function 
of the number of guesses.

Note that only the error with 5 guesses will be used to determine the 
winner of this competition for each criteria. 


=================================================
References
=================================================

[1] J. Deng, W. Dong, R. Socher, L.-J. Li, K. Li and L. Fei-Fei, ImageNet: 
A Large-Scale Hierarchical Image Database. IEEE Computer Vision and Pattern 
Recognition (CVPR), 2009. 

[2] David G. Lowe, Distinctive Image Features from Scale-Invariant Keypoints. 
International Journal of Computer Vision, 2004. 

[3] A. Vedaldi and B. Fulkerson. VLFeat: An Open and Portable Library 
of Computer Vision Algorithms. 2008. http://www.vlfeat.org

[4] R.-E. Fan, K.-W. Chang, C.-J. Hsieh, X.-R. Wang, and C.-J. Lin. LIBLINEAR: 
A library for large linear classification. Journal of Machine Learning Research 
9(2008), 1871-1874.

