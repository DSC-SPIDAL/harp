/* file: CompressionParameter.java */
/*******************************************************************************
* Copyright 2014-2017 Intel Corporation
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
 * @ingroup data_compression
 * @{
 */
package com.intel.daal.data_management.compression;

import com.intel.daal.algorithms.Parameter;
import com.intel.daal.services.DaalContext;

/**
 * <a name="DAAL-CLASS-DATA_MANAGEMENT__COMPRESSION__COMPRESSIONPARAMETER"></a>
 * @brief Parameters for the compression and decompression
 *
 * @par References
 *      - @ref CompressionLevel - Compression levels
 */
public class CompressionParameter extends Parameter {
    /** @private */
    static {
        System.loadLibrary("JavaAPI");
    }

    /**
    * CompressionParameter constructor
    */
    public CompressionParameter(DaalContext context, long cParameter) {
        super(context, cParameter);
    }

    /**
     * Sets the compression level
     * @param level   Compression level
     */
    public void setCompressionLevel(CompressionLevel level) {
        cSetCompressionLevel(this.cObject, level.getValue());
    }

    /**
     * Returns the compression level
     * @return Compression level
     */
    public CompressionLevel getCompressionLevel() {
        CompressionLevel cLevel = new CompressionLevel(cGetCompressionLevel(this.cObject));
        return cLevel;
    }

    private native void cSetCompressionLevel(long parAddr, int cLevel);

    private native int cGetCompressionLevel(long parAddr);
}
/** @} */
