# Uses python v 2.7

import pandas as pd
import sys,os
import numpy as np

if len(sys.argv) != 3:
	print "Usage: python processData.py <inputFile> <outputFile>"
	print "Example: python processData.py unprocessed_data.csv preprocessed_data.csv"
	sys.exit(1)

# inputFile variable holds the path to the training dataset file.
# outputFile variable points to the path of the process output file.

inputFile = sys.argv[1]
outputFile = sys.argv[2]

# print(filepath)
df = pd.DataFrame(pd.read_csv(inputFile, sep=","))
uni_car = list(set(df.UNIQUE_CARRIER))
state_abbr = list(set(df.ORIGIN_STATE_ABR))
#print(uni_car, state_abbr)

# converting the string literals into numeric form
df['UNIQUE_CARRIER_ID'] = [uni_car.index(v) for v in df['UNIQUE_CARRIER']]
df['ORIGIN_STATE_ABR_ID'] = [state_abbr.index(v) for v in df['ORIGIN_STATE_ABR']]
df['DEST_STATE_ABR_ID'] = [state_abbr.index(v) for v in df['DEST_STATE_ABR']]
desiredColumns = ['DAY_OF_WEEK','DEP_DELAY','DEP_DELAY_NEW','DEP_DELAY_GROUP','ARR_DELAY','ARR_DELAY_NEW','ARR_DELAY_GROUP','UNIQUE_CARRIER_ID','ORIGIN_STATE_ABR_ID','DEST_STATE_ABR_ID']

col = df.columns.values
#print col
# dropping the columns which are not necessary
for c in col:
    if c not in desiredColumns:
        df.drop(c,axis=1,inplace = True)


col = df.columns.values
col = col[:len(col)]

# filtering rows with null or empty values.
for c in col:
    df[c].replace('', np.nan, inplace=True)
    df.dropna(subset=[c], inplace=True)
	
# assigning class column
df['CLASS'] = [0]*len(df)

# assigning value to the class column
for i,r in df.iterrows():
	# the records with ARR_DELAY_GROUP greater than equal to 1 are considered
	# to be the positive samples i.e. they are assigned class label 1.
    if r['ARR_DELAY_GROUP'] >= 1:
        #print(".",i)
        df.set_value(i,'CLASS',1)
#print("Done")
df.drop('ARR_DELAY_GROUP', axis=1, inplace=True)
#http://stackoverflow.com/questions/10840533/most-pythonic-way-to-delete-a-file-which-may-not-exist

try:
	os.remove(outputFile)
	df.to_csv(outputFile, sep=',',index=False)
	print "Written to ",outputFile,"."
except OSError:
	print "Unable to write."
	pass
