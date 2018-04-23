# Uses python v 2.7

from __future__  import division
import pandas as pd
import sys, os

if len(sys.argv) != 4:
	print "Usage: python sampleData.py <inputFile> <outputFile> <datasetSizeRequired>"
	print "Example: python sampleData.py preprocessed_data.csv data_out.csv 1000"
	sys.exit(1)

# inputFile denotes 
inputFile = sys.argv[1]
outputFile = sys.argv[2]
datasetSizeRequired = int(sys.argv[3])

# reading into dataframe
df = pd.DataFrame(pd.read_csv(inputFile, sep=","))
total_in_file_rec = len(df)

# profiling the number of class 0 and class 1 records
num1 = df.CLASS.sum()
num0 = total_in_file_rec - num1
totalnum = num1+num0

# computing the number of class 0 and class 1 records to write
#numToWrite_Class1 = int((num1/totalnum)*datasetSizeRequired)
#numToWrite_Class0 = int((num0/totalnum)*datasetSizeRequired)

desiredRecs = int(datasetSizeRequired / 7)
day = 1
tmp_csv = pd.DataFrame()

daysSize = [desiredRecs for i in xrange(0,7)]
daysList = [df[df.DAY_OF_WEEK==day+1].DAY_OF_WEEK.count() for day in range(0,7)]
totalDeficit = 0
_days=[0 for i in xrange(0,7)]
for i in range(0,7):
    if daysList[i] < desiredRecs:
        totalDeficit += desiredRecs - daysList[i]
for i in range(0,7):
    if daysList[i] <= desiredRecs:
        _days[i] = daysList[i]
    else:
        _days[i] = desiredRecs
        delta = daysList[i] - desiredRecs
        if delta >= totalDeficit:
            _days[i]+=totalDeficit
            totalDeficit=0
        else:
            _days[i] += delta
            totalDeficit -= delta


for day in xrange(1,8):
	tmp_df = df[df.DAY_OF_WEEK == day]
	totalRec =_days[day-1]
	#print "For day ",day,": ",_days[day-1]
	class1 = tmp_df.CLASS.sum()
	class0 = len(tmp_df) - class1
	numToWrite_Class1 = int((class1/len(tmp_df))*totalRec)
	numToWrite_Class0 = int((class0/len(tmp_df))*totalRec)
	#print "Writing ",numToWrite_Class0,numToWrite_Class1," records"
	tmp_df_0 = tmp_df[tmp_df.CLASS==0]
	tmp_df_1 = tmp_df[tmp_df.CLASS==1]
	tmp_csv = tmp_csv.append(tmp_df_0[:numToWrite_Class0])
	tmp_csv = tmp_csv.append(tmp_df_1[:numToWrite_Class1])
	
#http://stackoverflow.com/questions/10840533/most-pythonic-way-to-delete-a-file-which-may-not-exist

try:
	os.remove(outputFile)
	tmp_csv.to_csv(outputFile,index=False)
	print "Written to ",outputFile,"."
except OSError:
	print "Unable to write."
	pass

