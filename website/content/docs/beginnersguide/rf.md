---
title: Random Forests Tutorial for Beginners
---

Random forests or random decision forests are an ensemble learning method for classification, regression and other tasks, that operate by constructing a multitude of decision trees at training time and outputting the class that is the mode of the classes (classification) or mean prediction (regression) of the individual trees.  Ensembles are a divide-and-conquer approach used to improve performance. The main principle behind ensemble methods is that a group of “weak learners” can come together to form a “strong learner”. Each classifier, individually, is a “weak learner,” while all the classifiers taken together are a “strong learner”. Random decision forests correct for decision trees' habit of overfitting to their training set. Random forests can be used to rank the importance of variables in a regression or classification problem in a natural way.




# **The Dataset**

The data consists of flight arrival and departure details for all commercial flights within the USA, from October 1987 to April 2008. This is a large dataset: there are nearly 120 million records in total, and takes up 1.6 gigabytes of space compressed and 12 gigabytes when uncompressed. To make sure that you're not overwhelmed by the size of the data, we've provide two brief introductions to some useful tools: linux command line tools and sqlite, a simple sql database. The aim of the data expo is to provide a **graphical** summary of important features of the data set. This is intentionally vague in order to allow different entries to focus on different aspects of the data, but here are a few ideas to get you started. 


## **The Categories**

This dataset consists of various types of data that is related to flight information, such as dates of the flight, flight numbers, destination, distances, and weather information, etc. 

![](https://d2mxuefqeaa7sj.cloudfront.net/s_1B5FA81D2B13AFC508D477B8AF2E409F57CCD4688678CA213431080E95BD3A54_1529939505818_Screen+Shot+2018-06-25+at+11.10.57+AM.png)


**Airports**
[airports.csv](http://stat-computing.org/dataexpo/2009/airports.csv) describes the locations of US airports, with the fields:

- iata: the international airport abbreviation code
- name of the airport
- city and country in which airport is located.
- lat and long: the latitude and longitude of the airport

This majority of this data comes from the [FAA](http://www.faa.gov/airports_airtraffic/airports/planning_capacity/npias/), but a few extra airports (mainly military bases and US protectorates) were collected from other web sources by Ryan Hafen and Hadley Wickham.

**Carrier codes**
Listing of carrier codes with full names: [carriers.csv](http://stat-computing.org/dataexpo/2009/carriers.csv)

**Planes**
You can find out information about individual planes by googling the tail number or by looking it up in the [FAA database](http://registry.faa.gov/aircraftinquiry/NNum_inquiry.asp).  [plane-data.csv](http://stat-computing.org/dataexpo/2009/plane-data.csv) is a csv file produced from the (some of) the data on that page.

**Weather**
Meteorological data is available from (among many others) [NOAA](http://www.ncdc.noaa.gov/oa/mpp/freedata.html) and [weather underground](http://www.wunderground.com/history). The call sign for airport weather stations can be constructed by adding K to the airport code, e.g. KORD, KLAX, KSEA.

The format for the data should be a list of <DAY_OF_WEEK DEP_DELAY DEP_DELAY_NEW DEP_DELAY_GROUP ARR_DELAY ARR_DELAY_NEW UNIQUE_CARRIER_ID ORIGIN_STATE_ABR_ID DEST_STATE_ABR_ID CLASS>. For example:
```bash
1 14 14 0 10 10 15 10 20 0
1 -3 0 -1 -24 0 15 10 20 0
```
Please refer to [Airline on-time performance](http://stat-computing.org/dataexpo/2009/) for more details.



# Run Example

The dataset used can be found [here] ( http://stat-computing.org/dataexpo/2009/the-data.html)

To be able to compile and run, you have to install [Harp and Hadoop](https://dsc-spidal.github.io/harp/docs/getting-started/):

Put data on hdfs
```bash
hdfs dfs -mkdir -p /data/airline
rm -rf data
mkdir -p data
cd data
split -l 74850 $HARP_ROOT_DIR/datasets/tutorial/airline/train.csv
cd ..
hdfs dfs -put data /data/airline
hdfs dfs -put $HARP_ROOT_DIR/datasets/tutorial/airline/test.csv /data/airline/
```

## **Compile**

Select the profile related to your hadoop version. For ex: hadoop-2.6.0. Supported hadoop versions are 2.6.0, 2.7.5 and 2.9.0
```bash
cd $HARP_ROOT_DIR
mvn clean package -Phadoop-2.6.0
cd $HARP_ROOT_DIR/contrib/target
cp contrib-0.1.0.jar $HADOOP_HOME
cp $HARP_ROOT_DIR/third_party/javaml-0.1.7.jar $HADOOP_HOME/share/hadoop/mapreduce
cp $HARP_ROOT_DIR/third_party/ajt-2.11.jar $HADOOP_HOME/share/hadoop/mapreduce
cd $HADOOP_HOME
```

## **Run**
```bash
hadoop jar contrib-0.1.0.jar edu.iu.rf.RFMapCollective 
Usage: edu.iu.rf.RFMapCollective <numTrees> <numMapTasks> <numThreads> <trainPath> <testPath> <outputPath>
```

## **Example**
```bash
hadoop jar contrib-0.1.0.jar edu.iu.rf.RFMapCollective 32 2 16 /data/airline/data/ /data/airline/test.csv /out
```

Fetch the result
```bash
hadoop fs -ls /out
hadoop fs -cat /out/output
```

