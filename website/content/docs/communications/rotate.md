---
title: Rotate
---

![rotate](/img/4-7-1.png)

`rotate` aims to swap tables among workers. If `rotateMap` isn't specified, the `i`th worker will get `(i + n - 1) % n`th worker's table and send its table to `(i + 1) % n`th worker. All workers should run it concurrently. The defination of the method is:
```java
boolean rotate(final String contextName, final String operationName, Table<P> globalTable, Int2IntMap rotateMap, DataMap dataMap, Workers workers)
```

* `contextName` --- the name of the context
* `operationName` --- the name of the operation
* `globalTable` --- the name of the global data table
* `rotateMap` --- the map indicating the order of rotation
* `dataMap` --- the data map
* `workers` --- the workers

## Example
```java
rotate(contextName, "max-rotate-" + i + "-" + j, cenTable, null, dataMap, workers);
```