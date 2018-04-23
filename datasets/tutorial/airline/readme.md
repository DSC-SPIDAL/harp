## airline dataset

[Airline](http://stat-computing.org/dataexpo/2009/) dataset consists of flight arrival and departure details for all commercial flights within the USA, from October 1987 to April 2008. 

Here is a subset extracted from the original dataset for input of binary classification tasks, such as random forest.

###Preprocessing

```bash
#Usage: python processData.py <inputFile> <outputFile>
python processData.py airline.csv airline-Processed.csv

#Sampling: Usage: python sampleData.py <inputFile> <outputFile> <datasetSizeRequired>
python sampleData.py airline-Processed.csv airline-final.csv 15000
```
