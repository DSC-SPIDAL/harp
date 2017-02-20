---
title: Allreduce
---   



![allreduce](/img/3-4-1.png)

`allreduce` aims to first combine tables from other workers and then broadcast the accumulated table. All workers should run it concurrently. The defination of the method is:
```java
boolean allreduce(final String contextName, final String operationName, final Table<P> table, final DataMap dataMap, final Workers workers)
```

* `contextName` --- the name of the context
* `operationName` --- the name of the operation
* `table` --- the name of the data table
* `dataMap` --- the data map
* `workers` --- the workers

## Example
```java
allreduce(contextName, "allreduce", table, dataMap, workers);
```