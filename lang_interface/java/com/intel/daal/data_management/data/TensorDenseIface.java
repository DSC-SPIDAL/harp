/* file: TensorDenseIface.java */
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
 * @ingroup tensor
 * @{
 */
package com.intel.daal.data_management.data;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

interface TensorDenseIface {

    /**
     * Reads subtensor the tensor and returns it to
     *        java.nio.DoubleBuffer. This method needs to be defined by user
     *        in the subclass of this class.
     *
     * @param  vectorIndex Index of the first row to include into the block
     * @param  vectorNum   Number of rows in the block
     * @param buf         Buffer to store results
     *
     * @return Subtensor packed into DoubleBuffer
     */
    abstract public DoubleBuffer getSubtensor(long[] fixedDims, long rangeDimIdx, long rangeDimNum, DoubleBuffer buf);

    /**
     * Reads subtensor the tensor and returns it to
     *        java.nio.FloatBuffer. This method needs to be defined by user in
     *        the subclass of this class.
     *
     * @param  vectorIndex Index of the first row to include into the block
     * @param  vectorNum   Number of rows in the block
     * @param buf         Buffer to store results
     *
     * @return Subtensor packed into FloatBuffer
     */
    abstract public FloatBuffer getSubtensor(long[] fixedDims, long rangeDimIdx, long rangeDimNum, FloatBuffer buf);

    /**
     * Reads subtensor the tensor and returns it to
     *        java.nio.IntBuffer. This method needs to be defined by user in
     *        the subclass of this class.
     *
     * @param  vectorIndex Index of the first row to include into the block
     * @param  vectorNum   Number of rows in the block
     * @param buf         Buffer to store results
     *
     * @return Subtensor packed into IntBuffer
     */
    abstract public IntBuffer getSubtensor(long[] fixedDims, long rangeDimIdx, long rangeDimNum, IntBuffer buf);

    /**
     * Transfers the data from the input DoubleBuffer subtensor of the tensor
     * This function needs to be defined by user in the subclass of
     * this class.
     *
     * @param  vectorIndex Index of the first row to include into the block
     * @param  vectorNum   Number of rows in the block
     * @param buf         Input DoubleBuffer with the subtensor data
     */
    abstract public void releaseSubtensor(long[] fixedDims, long rangeDimIdx, long rangeDimNum, DoubleBuffer buf);

    /**
     * Transfers the data from the input FloatBuffer subtensor of the tensor
     * This function needs to be defined by user in the subclass of
     * this class.
     *
     * @param  vectorIndex Index of the first row to include into the block
     * @param  vectorNum   Number of rows in the block
     * @param buf         Input FloatBuffer with the subtensor data
     */
    abstract public void releaseSubtensor(long[] fixedDims, long rangeDimIdx, long rangeDimNum, FloatBuffer buf);

    /**
     * Transfers the data from the input IntBuffer subtensor of the tensor
     * This function needs to be defined by user in the subclass of
     * this class.
     *
     * @param vectorIndex Index of the first row to include into the block
     * @param vectorNum   Number of rows in the block
     * @param buf         Input IntBuffer with the subtensor data
     */
    abstract public void releaseSubtensor(long[] fixedDims, long rangeDimIdx, long rangeDimNum, IntBuffer buf);
}
/** @} */
