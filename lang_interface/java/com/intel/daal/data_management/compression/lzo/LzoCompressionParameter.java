/* file: LzoCompressionParameter.java */
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
 * @ingroup data_compression
 * @{
 */
package com.intel.daal.data_management.compression.lzo;

import com.intel.daal.data_management.compression.CompressionParameter;
import com.intel.daal.services.DaalContext;

/**
 * <a name="DAAL-CLASS-DATA_MANAGEMENT__COMPRESSION__LZO__LZOCOMPRESSIONPARAMETER"></a>
 *
 * @brief Parameter for the LZO compression and decompression
 * LZO compressed block header consists of four sections: 1) optional, 2) uncompressed data size(4 bytes),
 * 3) compressed data size(4 bytes), 4) optional.
 *
 * @par Enumerations
 *      - @ref CompressionLevel - %Compression levels enumeration
 */
public class LzoCompressionParameter extends CompressionParameter {
    /** @private */
    static {
        System.loadLibrary("JavaAPI");
    }

    public LzoCompressionParameter(DaalContext context, long cParameter) {
        super(context, cParameter);
    }

    /**
     *  Sets size of section 1) of LZO compressed block header in bytes
     *  @param preHeadBytes Size of section 1) of LZO compressed block header in bytes
     */
    public void setPreHeadBytes(long preHeadBytes) {
        cSetPreHeadBytes(this.cObject, preHeadBytes);
    }

    /**
     *  Returns size of section 1) of LZO compressed block header in bytes
     *  @return Size of section 1) of LZO compressed block header in bytes
     */
    public long getPreHeadBytes() {
        return cGetPreHeadBytes(this.cObject);
    }

    /**
     *  Sets size of section 4) of LZO compressed block header in bytes
     *  @param postHeadBytes Size of section 4) of LZO compressed block header in bytes
     */
    public void setPostHeadBytes(long postHeadBytes) {
        cSetPostHeadBytes(this.cObject, postHeadBytes);
    }

    /**
     *  Returns size of section 4) of LZO compressed block header in bytes
     *  @return Size of section 4) of LZO compressed block header in bytes
     */
    public long getPostHeadBytes() {
        return cGetPostHeadBytes(this.cObject);
    }

    private native void cSetPreHeadBytes(long parAddr, long preHeadBytes);

    private native long cGetPreHeadBytes(long parAddr);

    private native void cSetPostHeadBytes(long parAddr, long postHeadBytes);

    private native long cGetPostHeadBytes(long parAddr);
}
/** @} */
