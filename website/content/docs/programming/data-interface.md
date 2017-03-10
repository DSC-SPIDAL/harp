---
title: Data Interfaces and Types
---   

![data-abstraction](/img/2-2-1.png)

Harp provides three levels of data structures: arrays and objects, partition, and table. Arrays and Serializable objects are the basic data structures, which include:

1. `ByteArray`: an array with byte-type elements

2. `ShortArray`: an array with short-type elements

3. `IntArray`: an array with int-type elements

4. `FloatArray`: an array with float-type elements

5. `LongArray`: an array with long-type elements

6. `DoubleArray`: an array with double-type elements

7. `Writable`: serializable object

`Partition` is a wraper of the data structures shown above. Every partition has an ID. In collective communication, partitions from different processors with the same ID will be merged. The merge operation is defined by `PartitionCombiner`.

`Table` is a container for partitions. It is a high-level data structure and the unit for collective communication.


# Table and Partitions
![table-partition](/img/3-1-2.png)

An example of how to construct a table is:

```java
Table<DoubleArray> table = new Table<>(0, new DoubleArrPlus());
for (int i = 0; i < numPartitions; i++) {
    DoubleArray array = DoubleArray.create(size, false);
    table.addPartition(new Partition<>(i, array));
}
```
In this example, it initiliazes a table which carries `DoubleArray` as the primitive data type. `DoubleArrPlus` is a `PartitionCombiner` used to define the merging operation of two partitions.

```java
public class DoubleArrPlus extends PartitionCombiner<DoubleArray> {
    public PartitionStatus combine(DoubleArray curPar, DoubleArray newPar) {
        double[] doubles1 = curPar.get(); int size1 = curPar.size();
        double[] doubles2 = newPar.get(); int size2 = newPar.size();
        if (size1 != size2) {            return PartitionStatus.COMBINE_FAILED;
        }
        for (int i = 0; i < size2; i++) {
            doubles1[i] = doubles1[i] + doubles2[i];
        }
        return PartitionStatus.COMBINED;
   }
}

```

# Data Abstraction

![data-types](/img/3-1-3.png)

The data abstraction is shown above. `Transferable` is ae higher interface compare to other data structures and `Simple` is the sub-interface for all primitive data structures. Here is an example of the primitive data strucutres.
```java
/*
 * Copyright 2013-2016 Indiana University
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.iu.harp.resource;

import java.io.DataOutput;
import java.io.IOException;

import edu.iu.harp.io.DataType;
import edu.iu.harp.resource.Array;

/*******************************************************
 * IntArray class for managing int[] data.
 ******************************************************/
public final class IntArray extends Array<int[]> {

    public IntArray(int[] arr, int start, int size) {
	super(arr, start, size);
    }

    /**
     * Get the number of Bytes of encoded data. One byte for storing DataType,
     * four bytes for storing the size, size*4 bytes for storing the data.
     */
    @Override
    public int getNumEnocdeBytes() {
	return this.size * 4 + 5;
    }

    /**
     * Encode the array as DataOutput
     */
    @Override
    public void encode(DataOutput out) throws IOException {
	out.writeByte(DataType.INT_ARRAY);
	int len = start + size;
	out.writeInt(size);
	for (int i = start; i < len; i++) {
	    out.writeInt(array[i]);
	}
    }

    /**
     * Create an array. Firstly try to get an array from ResourcePool; if
     * failed, new an array.
     * 
     * @param len
     * @param approximate
     * @return
     */
    public static IntArray create(int len, boolean approximate) {
	if (len > 0) {
	    int[] ints = ResourcePool.get().getIntsPool().getArray(len, approximate);
	    if (ints != null) {
		return new IntArray(ints, 0, len);
	    } else {
		return null;
	    }
	} else {
	    return null;
	}
    }

    /**
     * Release the array from the ResourcePool
     */
    @Override
    public void release() {
	ResourcePool.get().getIntsPool().releaseArray(array);
	this.reset();
    }

    /**
     * Free the array from the ResourcePool
     */
    @Override
    public void free() {
	ResourcePool.get().getIntsPool().freeArray(array);
	this.reset();

    }
}

```



