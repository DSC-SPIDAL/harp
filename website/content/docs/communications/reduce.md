---
title: Reduce
---

![reduce](/img/4-2-1.png)

`reduce` aims to combine tables from other workers to this worker. All workers should run it concurrently. The defination of the method is:
```java
boolean reduce(final String contextName, final String operationName, final Table<P> table, final int reduceWorkerID, final DataMap dataMap, final Workers workers)
```

* `contextName` --- the name of the context
* `operationName` --- the name of the operation
* `table` --- the name of the data table
* `reduceWorkerID` --- the ID of the worker whose table combines others
* `dataMap` --- the data map
* `workers` --- the workers

## Example
```java
reduce(contextName, "reduce", table, workers.getMasterID(), dataMap, workers);
```