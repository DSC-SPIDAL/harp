---
title: Push and Pull
---

![push](/img/4-6-1.png)

`push` aims to collect all workers' partitions and store them in global. All workers should run it concurrently. The defination of the method is:
```java
boolean push(final String contextName, final String operationName, Table<P> localTable, Table<P> globalTable, PT partitioner, DataMap dataMap, Workers workers)
```

* `contextName` --- the name of the context
* `operationName` --- the name of the operation
* `localTable` --- the name of the local data table
* `globalTable` --- the name of the global data table
* `partitioner` --- the partitioner
* `dataMap` --- the data map
* `workers` --- the workers

## Example
```java
push(contextName, "local-global", localTable, globalTable, new Partitioner(workers.getNumWorkers()), dataMap, workers);
```

![pull](/img/4-6-2.png)

`pull` aims to distribute the partitions stored in global to all workers. All workers should run it concurrently. The defination of the method is:
```java
boolean pull(final String contextName, final String operationName, Table<P> localTable, Table<P> globalTable, boolean useBcast, DataMap dataMap, Workers workers)
```

* `contextName` --- the name of the context
* `operationName` --- the name of the operation
* `localTable` --- the name of the local data table
* `globalTable` --- the name of the global data table
* `useBcast` --- whether use broadcast or not
* `dataMap` --- the data map
* `workers` --- the workers

## Example
```java
pull(contextName, "global-local", localTable, globalTable, true, dataMap, workers);
```