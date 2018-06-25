---
title: Communication Module API 
---

Harp-DAAL provides users a module of interprocess (mappers) communication operations with high-level APIs.
Meanwhile, all of the low-level API of Harp operations are still compatible with DAAL implementations, however, 
users must handle the data serialization and data type conversion by themselves. 

To use the communication module:

```java
import edu.iu.data_comm.*;
```

To create a communication module

```java
HarpDAALComm comm = new HarpDAALComm(self_id, master_id, num_mappers, context, mapper);
```

where the arguments are defined as follows

* int self_id: the ID of current mapper
* int master_id: the ID of master mapper
* int num_workers: the total number of mapper
* DaalContext context: the DAAL context handle to initialize DAAL data structures
* CollectiveMapper mapper: the handle of mapper object that invokes this function

There are three types of communication operations wrapped by Harp-DAAL Communication Module 

* harpdaal_broadcast
* harpdaal_gather
* harpdaal_allgather

## Broadcast

To use broadcast *input* objects from master mapper 

```java
SerializableBase output = comm.harpdaal_braodcast(input, contextName, operationName, useSpanTree);
```

where, *input* and *output* object must be java classes extended from Intel DAAL *SerializableBase* class. The *contextName*, 
*operationName* and *useSpanTree* are strings passed to the harp *broadcast* operation. If *useSpanTree* is true, harp will 
select a minimum spanning tree algorithm as its internal routing algorithm. The master mapper shall provide a valid *input*
object and each worker mapper shall obtain its broadcast copy from the output object. Users may also specify a root mapper ID
that is different from the master mapper ID 

```java
SerializableBase output = comm.harpdaal_braodcast(input, root_id, contextName, operationName, useSpanTree);
```

and the *input* object is broadcast from *mapper root_id* to all the other mappers. 

## Gather

To gather *input* object from all the mappers to the master mapper: 

```java
SerializableBase[] outputs = comm.harpdaal_gather(input, contextName, operationName);
```

where, the *outputs* is an array of *SerializableBase* from all the mappers. Similarly, the users may provide the *root_id* other than master mapper ID.

```java
SerializableBase[] outputs = comm.harpdaal_gather(input, root_id, contextName, operationName);
```

## Allgather

*Allgather* operation first gathers the data from all mappers to the master mapper, and then it broadcasts the output from the master mapper back to all the 
worker mappers.

```java
SerializableBase[] outputs = comm.harpdaal_allgather(input, contextName, operationName);
```















