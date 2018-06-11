/* file: HarpDAALNumericTableImpl.java */
/*******************************************************************************
* Copyright 2014-2018 Intel Corporation
* All Rights Reserved.
*
* If this  software was obtained  under the  Intel Simplified  Software License,
* the following terms apply:
*
* The source code,  information  and material  ("Material") contained  herein is
* owned by Intel Corporation or its  suppliers or licensors,  and  title to such
* Material remains with Intel  Corporation or its  suppliers or  licensors.  The
* Material  contains  proprietary  information  of  Intel or  its suppliers  and
* licensors.  The Material is protected by  worldwide copyright  laws and treaty
* provisions.  No part  of  the  Material   may  be  used,  copied,  reproduced,
* modified, published,  uploaded, posted, transmitted,  distributed or disclosed
* in any way without Intel's prior express written permission.  No license under
* any patent,  copyright or other  intellectual property rights  in the Material
* is granted to  or  conferred  upon  you,  either   expressly,  by implication,
* inducement,  estoppel  or  otherwise.  Any  license   under such  intellectual
* property rights must be express and approved by Intel in writing.
*
* Unless otherwise agreed by Intel in writing,  you may not remove or alter this
* notice or  any  other  notice   embedded  in  Materials  by  Intel  or Intel's
* suppliers or licensors in any way.
*
*
* If this  software  was obtained  under the  Apache License,  Version  2.0 (the
* "License"), the following terms apply:
*
* You may  not use this  file except  in compliance  with  the License.  You may
* obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
*
*
* Unless  required  by   applicable  law  or  agreed  to  in  writing,  software
* distributed under the License  is distributed  on an  "AS IS"  BASIS,  WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*
* See the   License  for the   specific  language   governing   permissions  and
* limitations under the License.
*******************************************************************************/

/**
 * @ingroup numeric_tables
 * @{
 */
package com.intel.daal.data_management.data;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.Vector;
import java.util.HashMap;
import java.util.Map;

import com.intel.daal.data_management.data.DataCollection;
import com.intel.daal.services.DaalContext;

/**
 *  <a name="DAAL-CLASS-DATA_MANAGEMENT__DATA__HARPDAALNUMERICTABLEIMPL"></a>
 *  @brief Class that provides methods to access a collection of numeric tables as if they are joined by columns
 */
public class HarpDAALNumericTableImpl extends NumericTableImpl {
    /** @private */
    static {
        System.loadLibrary("JavaAPI");
    }

    /**
     * Constructs empty HarpDAAL numeric table
     * @param context   Context to manage created HarpDAAL numeric table
     */
    public HarpDAALNumericTableImpl(DaalContext context) {
        super(context);
        dataAllocatedInJava = false;
        this.cObject = cNewHarpDAALNumericTable();
        dict = new DataDictionary(getContext(), (long)0, cGetCDataDictionary(cObject));
	_hashtable = new HashMap<>();
    }

    /**
     * Constructs HarpDAAL numeric table from C++ HarpDAAL numeric table
     *
     * @param context   Context to manage created HarpDAAL numeric table
     * @param cTable    Pointer to C++ numeric table
     */
    public HarpDAALNumericTableImpl(DaalContext context, long cTable) {
        super(context);
        dataAllocatedInJava = false;
        this.cObject = cTable;
        dict = new DataDictionary(getContext(), (long)0, cGetCDataDictionary(cObject));
	_hashtable = new HashMap<>();
    }

    /**
     * Constructs harpdaal table consisting of one partition
     *
     * @param context   Context to manage created merged numeric table
     * @param table     Pointer to the Numeric Table
     */
    public HarpDAALNumericTableImpl(DaalContext context, NumericTable table) {
        super(context);
        dataAllocatedInJava = false;
        this.cObject = cNewHarpDAALNumericTable();
        dict = new DataDictionary(getContext(), (long)0, cGetCDataDictionary(cObject));
	_hashtable = new HashMap<>();
	// initialize the first partition with 0 id
        addPartition(table, 0);
    }

    
    /**
     * @brief add a partition with key id to a harp_daal_numerictable
     *
     * @param table
     * @param key
     *
     * @return 
     */
    public void addPartition(NumericTable table, int key) {
        if ((table.getDataLayout().ordinal() & NumericTable.StorageLayout.csrArray.ordinal()) != 0) {
            throw new IllegalArgumentException("can not join numeric table in csr format to harpdaal table");
        } else {
            cAddPartition(this.cObject, table.getCObject(), key);
	   
	    _hashtable.put(key, table);

            int ncols = (int)(getNumberOfColumns());
            int cols = (int)(table.getNumberOfColumns());

            dict.setNumberOfFeatures(ncols);

            for (int i = 0; i < cols; i++) {
                DataFeature f = table.getDictionary().getFeature(i);
                dict.setFeature(f, ncols - cols + i);
            }
        }
    }

    public NumericTable getPartition(int key) 
    {
	return _hashtable.get(key);
    }

    public int getNumberOfPartitions()
    {
	return _hashtable.size();
    }

    /** @copydoc NumericTable::getNumberOfColumns() */
    @Override
    public long getNumberOfColumns() {
        return cGetNumberOfColumns(this.cObject);
    }

    /** @copydoc NumericTable::getBlockOfRows(long,long,DoubleBuffer) */
    @Override
    public DoubleBuffer getBlockOfRows(long vectorIndex, long vectorNum, DoubleBuffer buf) {
        int nColumns = (int) (getNumberOfColumns());
        int bufferSize = (int) (vectorNum * nColumns);
        // Gets data from C++ NumericTable object
        ByteBuffer byteBuf = ByteBuffer.allocateDirect(bufferSize * 8 /* sizeof(double) */);
        byteBuf.order(ByteOrder.LITTLE_ENDIAN);
        byteBuf = getDoubleBlockBuffer(this.cObject, vectorIndex, vectorNum, byteBuf);
        return byteBuf.asDoubleBuffer();
    }

    /** @copydoc NumericTable::getBlockOfRows(long,long,FloatBuffer) */
    @Override
    public FloatBuffer getBlockOfRows(long vectorIndex, long vectorNum, FloatBuffer buf) {
        int nColumns = (int) (getNumberOfColumns());
        int bufferSize = (int) (vectorNum * nColumns);
        // Gets data from C++ NumericTable object
        ByteBuffer byteBuf = ByteBuffer.allocateDirect(bufferSize * 4 /* sizeof(float) */);
        byteBuf.order(ByteOrder.LITTLE_ENDIAN);
        byteBuf = getFloatBlockBuffer(this.cObject, vectorIndex, vectorNum, byteBuf);
        return byteBuf.asFloatBuffer();
    }

    /** @copydoc NumericTable::getBlockOfRows(long,long,IntBuffer) */
    @Override
    public IntBuffer getBlockOfRows(long vectorIndex, long vectorNum, IntBuffer buf) {
        int nColumns = (int) (getNumberOfColumns());
        int bufferSize = (int) (vectorNum * nColumns);
        // Gets data from C++ NumericTable object
        ByteBuffer byteBuf = ByteBuffer.allocateDirect(bufferSize * 4 /* sizeof(int) */);
        byteBuf.order(ByteOrder.LITTLE_ENDIAN);
        byteBuf = getIntBlockBuffer(this.cObject, vectorIndex, vectorNum, byteBuf);
        return byteBuf.asIntBuffer();
    }

    /** @copydoc NumericTable::getBlockOfColumnValues(long,long,long,DoubleBuffer) */
    @Override
    public DoubleBuffer getBlockOfColumnValues(long featureIndex, long vectorIndex, long vectorNum, DoubleBuffer buf) {
        int nColumns = (int) getNumberOfColumns();
        int bufferSize = (int) vectorNum;
        // Gets data from C++ NumericTable object
        ByteBuffer byteBuf = ByteBuffer.allocateDirect(bufferSize * 8 /* sizeof(double) */);
        byteBuf.order(ByteOrder.LITTLE_ENDIAN);
        byteBuf = getDoubleColumnBuffer(this.cObject, featureIndex, vectorIndex, vectorNum, byteBuf);
        return byteBuf.asDoubleBuffer();
    }

    /** @copydoc NumericTable::getBlockOfColumnValues(long,long,long,FloatBuffer) */
    @Override
    public FloatBuffer getBlockOfColumnValues(long featureIndex, long vectorIndex, long vectorNum, FloatBuffer buf) {
        int nColumns = (int) getNumberOfColumns();
        int bufferSize = (int) vectorNum;
        // Gets data from C++ NumericTable object
        ByteBuffer byteBuf = ByteBuffer.allocateDirect(bufferSize * 4 /* sizeof(float) */);
        byteBuf.order(ByteOrder.LITTLE_ENDIAN);
        byteBuf = getFloatColumnBuffer(this.cObject, featureIndex, vectorIndex, vectorNum, byteBuf);
        return byteBuf.asFloatBuffer();
    }

    /** @copydoc NumericTable::getBlockOfColumnValues(long,long,long,IntBuffer) */
    @Override
    public IntBuffer getBlockOfColumnValues(long featureIndex, long vectorIndex, long vectorNum, IntBuffer buf) {
        int nColumns = (int) getNumberOfColumns();
        int bufferSize = (int) vectorNum;
        // Gets data from C++ NumericTable object
        ByteBuffer byteBuf = ByteBuffer.allocateDirect(bufferSize * 4 /* sizeof(int) */);
        byteBuf.order(ByteOrder.LITTLE_ENDIAN);
        byteBuf = getIntColumnBuffer(this.cObject, featureIndex, vectorIndex, vectorNum, byteBuf);
        return byteBuf.asIntBuffer();
    }

    /** @copydoc NumericTable::releaseBlockOfRows(long,long,FloatBuffer) */
    @Override
    public void releaseBlockOfRows(long vectorIndex, long vectorNum, FloatBuffer buf) {
        int nColumns = (int) (getNumberOfColumns());
        int bufferSize = (int) (vectorNum * nColumns);

        float[] data = new float[buf.capacity()];
        buf.position(0);
        buf.get(data);
        // Gets data from C++ NumericTable object
        ByteBuffer byteBuf = ByteBuffer.allocateDirect(bufferSize * 4 /* sizeof(float) */);
        byteBuf.order(ByteOrder.LITTLE_ENDIAN);
        byteBuf.asFloatBuffer().put(data);
        releaseFloatBlockBuffer(this.cObject, vectorIndex, vectorNum, byteBuf);
    }

    /** @copydoc NumericTable::releaseBlockOfRows(long,long,DoubleBuffer) */
    @Override
    public void releaseBlockOfRows(long vectorIndex, long vectorNum, DoubleBuffer buf) {
        int nColumns = (int) (getNumberOfColumns());
        int bufferSize = (int) (vectorNum * nColumns);

        double[] data = new double[buf.capacity()];
        buf.position(0);
        buf.get(data);
        // Gets data from C++ NumericTable object
        ByteBuffer byteBuf = ByteBuffer.allocateDirect(bufferSize * 8 /* sizeof(double) */);
        byteBuf.order(ByteOrder.LITTLE_ENDIAN);
        byteBuf.asDoubleBuffer().put(data);
        releaseDoubleBlockBuffer(this.cObject, vectorIndex, vectorNum, byteBuf);
    }

    /** @copydoc NumericTable::releaseBlockOfRows(long,long,IntBuffer) */
    @Override
    public void releaseBlockOfRows(long vectorIndex, long vectorNum, IntBuffer buf) {
        int nColumns = (int) (getNumberOfColumns());
        int bufferSize = (int) (vectorNum * nColumns);

        int[] data = new int[buf.capacity()];
        buf.position(0);
        buf.get(data);
        // Gets data from C++ NumericTable object
        ByteBuffer byteBuf = ByteBuffer.allocateDirect(bufferSize * 4 /* sizeof(int) */);
        byteBuf.order(ByteOrder.LITTLE_ENDIAN);
        byteBuf.asIntBuffer().put(data);
        releaseIntBlockBuffer(this.cObject, vectorIndex, vectorNum, byteBuf);
    }

    /** @copydoc NumericTable::releaseBlockOfColumnValues(long,long,long,DoubleBuffer) */
    @Override
    public void releaseBlockOfColumnValues(long featureIndex, long vectorIndex, long vectorNum, DoubleBuffer buf) {
        int nColumns = (int) (getNumberOfColumns());
        int bufferSize = (int) (vectorNum);

        double[] data = new double[buf.capacity()];
        buf.position(0);
        buf.get(data);
        // Gets data from C++ NumericTable object
        ByteBuffer byteBuf = ByteBuffer.allocateDirect(bufferSize * 8 /* sizeof(double) */);
        byteBuf.order(ByteOrder.LITTLE_ENDIAN);
        byteBuf.asDoubleBuffer().put(data);
        releaseDoubleColumnBuffer(this.cObject, featureIndex, vectorIndex, vectorNum, byteBuf);
    }

    /** @copydoc NumericTable::releaseBlockOfColumnValues(long,long,long,FloatBuffer) */
    @Override
    public void releaseBlockOfColumnValues(long featureIndex, long vectorIndex, long vectorNum, FloatBuffer buf) {
        int nColumns = (int) (getNumberOfColumns());
        int bufferSize = (int) (vectorNum);

        float[] data = new float[buf.capacity()];
        buf.position(0);
        buf.get(data);
        // Gets data from C++ NumericTable object
        ByteBuffer byteBuf = ByteBuffer.allocateDirect(bufferSize * 4 /* sizeof(float) */);
        byteBuf.order(ByteOrder.LITTLE_ENDIAN);
        byteBuf.asFloatBuffer().put(data);
        releaseFloatColumnBuffer(this.cObject, featureIndex, vectorIndex, vectorNum, byteBuf);
    }

    /** @copydoc NumericTable::releaseBlockOfColumnValues(long,long,long,IntBuffer) */
    @Override
    public void releaseBlockOfColumnValues(long featureIndex, long vectorIndex, long vectorNum, IntBuffer buf) {
        int nColumns = (int) (getNumberOfColumns());
        int bufferSize = (int) (vectorNum);

        int[] data = new int[buf.capacity()];
        buf.position(0);
        buf.get(data);
        // Gets data from C++ NumericTable object
        ByteBuffer byteBuf = ByteBuffer.allocateDirect(bufferSize * 4 /* sizeof(int) */);
        byteBuf.order(ByteOrder.LITTLE_ENDIAN);
        byteBuf.asIntBuffer().put(data);
        releaseIntColumnBuffer(this.cObject, featureIndex, vectorIndex, vectorNum, byteBuf);
    }

    /* Gets NIO buffer containing data of the C++ table */

    private native ByteBuffer getDoubleBlockBuffer(long cObject, long vectorIndex, long vectorNum, ByteBuffer buffer);

    private native ByteBuffer getFloatBlockBuffer(long cObject, long vectorIndex, long vectorNum, ByteBuffer buffer);

    private native ByteBuffer getIntBlockBuffer(long cObject, long vectorIndex, long vectorNum, ByteBuffer buffer);

    private native void releaseDoubleBlockBuffer(long cObject, long vectorIndex, long vectorNum, ByteBuffer buffer);

    private native void releaseFloatBlockBuffer(long cObject, long vectorIndex, long vectorNum, ByteBuffer buffer);

    private native void releaseIntBlockBuffer(long cObject, long vectorIndex, long vectorNum, ByteBuffer buffer);

    private native ByteBuffer getDoubleColumnBuffer(long cObject, long featureIndex, long vectorIndex, long vectorNum, ByteBuffer buffer);
    private native ByteBuffer getFloatColumnBuffer (long cObject, long featureIndex, long vectorIndex, long vectorNum, ByteBuffer buffer);
    private native ByteBuffer getIntColumnBuffer   (long cObject, long featureIndex, long vectorIndex, long vectorNum, ByteBuffer buffer);

    private native void releaseDoubleColumnBuffer(long cObject, long featureIndex, long vectorIndex, long vectorNum, ByteBuffer buffer);
    private native void releaseFloatColumnBuffer (long cObject, long featureIndex, long vectorIndex, long vectorNum, ByteBuffer buffer);
    private native void releaseIntColumnBuffer   (long cObject, long featureIndex, long vectorIndex, long vectorNum, ByteBuffer buffer);

    private native long cNewHarpDAALNumericTable();
    private native long cGetNumberOfColumns(long cObject);
    private native void cAddPartition(long cObject, long numericTableAddr, int key);

    private HashMap<Integer, NumericTable> _hashtable;
}
/** @} */
