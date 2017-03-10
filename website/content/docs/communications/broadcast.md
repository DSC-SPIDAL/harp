---
title: Broadcast
---
 

![broadcast](/img/3-1-1.png)

`broadcast` aims to share a table in one worker with others. All workers should run it concurrently. The defination of the method is:
```java
boolean broadcast(String contextName, String operationName, Table<P> table, int bcastWorkerID, boolean useMSTBcast, DataMap dataMap, Workers workers)
```

* `contextName` --- the name of the context
* `operationName` --- the name of the operation
* `table` --- the name of the data table
* `bcastWorkerID` --- the ID of the worker which broadcasts
* `useMSTBcast` --- whether use MST method or not
* `dataMap` --- the data map
* `workers` --- the workers

## Example
```java
broadcast(contextName, "chain-array-table-bcast-" + i, arrTable, workers.getMasterID(), false, dataMap, workers);
```