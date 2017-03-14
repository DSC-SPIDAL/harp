---
title: Allgather
---   

![allgather](/img/3-3-1.png)

`allgather` aims to first collect tables from other workers and then broadcast the collection. All workers should run it concurrently. The defination of the method is:
```java
boolean allgather(final String contextName, final String operationName, final Table<P> table, final DataMap dataMap, final Workers workers)
```

* `contextName` --- the name of the context
* `operationName` --- the name of the operation
* `table` --- the name of the data table
* `dataMap` --- the data map
* `workers` --- the workers

## Example
```java
allgather(contextName, "allgather", table, dataMap, workers);
```