---
title: Regroup
---

![regroup](/img/3-5-1.png)

`regroup` aims to combine tables among a group of workers. All workers should run it concurrently. The defination of the method is:
```java
boolean regroupCombine(final String contextName, String operationName, Table<P> table, Partitioner partitioner, DataMap dataMap, Workers workers)
```

* `contextName` --- the name of the context
* `operationName` --- the name of the operation
* `table` --- the name of the data table
* `partitioner` --- the partitioner
* `dataMap` --- the data map
* `workers` --- the workers

## Example
```java
regroupCombine(contextName, "regroup", table, new Partitioner(workers.getNumWorkers()), dataMap, workers);
```