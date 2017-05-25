/* file: HomogenBMNumericTableImpl.java */
/*******************************************************************************
* Copyright 2014-2016 Intel Corporation
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
*******************************************************************************/

package com.intel.daal.data_management.data;

import com.intel.daal.services.DaalContext;

/**
 * <a name="DAAL-CLASS-DATA__HomogenBMNumericTableImpl__HomogenBMNumericTableImpl"></a>
 * @brief A derivative class of the NumericTableImpl class, that provides common interfaces for
 *        different implementations of a homogen numeric table
 */
abstract class HomogenBMNumericTableImpl extends NumericTableImpl {
    protected Class<? extends Number> type;

    /** @private */
    static {
        System.loadLibrary("JavaAPI");
    }

    public HomogenBMNumericTableImpl(DaalContext context) {
        super(context);
    }

    abstract public void assign(long constValue);

    abstract public void assign(int constValue);

    abstract public void assign(double constValue);

    abstract public void assign(float constValue);

    abstract public double[] getDoubleArray();

    abstract public float[] getFloatArray();

    abstract public long[] getLongArray();

    abstract public Object getDataObject();

    abstract public Class<? extends Number> getNumericType();

    abstract public void set(long row, long column, double value);

    abstract public void set(long row, long column, float value);

    abstract public void set(long row, long column, long value);

    abstract public void set(long row, long column, int value);

    abstract public double getDouble(long row, long column);

    abstract public float getFloat(long row, long column);

    abstract public long getLong(long row, long column);

    abstract public int getInt(long row, long column);

    abstract public void releaseBlockOfRowsByte(long vectorIndex, long vectorNum, double[] data); 

    abstract public void releaseBlockOfRowsByte(long vectorIndex, long vectorNum, float[] data); 

    abstract public void releaseBlockOfRowsByte(long vectorIndex, long vectorNum, int[] data); 

    abstract public void getBlockOfRowsByte(long vectorIndex, long vectorNum, double[] data); 

    abstract public void getBlockOfRowsByte(long vectorIndex, long vectorNum, float[] data); 

    abstract public void getBlockOfRowsByte(long vectorIndex, long vectorNum, int[] data); 

    abstract public void getBlockOfColumnValuesByte(long featureIndex, long vectorIndex, long vectorNum, double[] data); 

    abstract public void getBlockOfColumnValuesByte(long featureIndex, long vectorIndex, long vectorNum, float[] data); 

    abstract public void getBlockOfColumnValuesByte(long featureIndex, long vectorIndex, long vectorNum, int[] data); 

    abstract public void releaseBlockOfColumnValuesByte(long featureIndex, long vectorIndex, long vectorNum, double[] data); 

    abstract public void releaseBlockOfColumnValuesByte(long featureIndex, long vectorIndex, long vectorNum, float[] data); 

    abstract public void releaseBlockOfColumnValuesByte(long featureIndex, long vectorIndex, long vectorNum, int[] data); 

}
