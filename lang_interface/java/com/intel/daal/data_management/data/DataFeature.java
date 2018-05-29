/* file: DataFeature.java */
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
 * @ingroup data_dictionary
 * @{
 */
package com.intel.daal.data_management.data;

import com.intel.daal.services.DaalContext;
import java.nio.ByteBuffer;

/**
 * <a name="DAAL-CLASS-DATA_MANAGEMENT__DATA__DATAFEATURE"></a>
 * @brief Class used to describe a feature. The structure is used in the
 *        com.intel.daal.data.DataDictionary class.
 */
public class DataFeature extends SerializableBase {
    /** @private */
    static {
        System.loadLibrary("JavaAPI");
    }

    /**
     * Constructs the data feature
     * @param context   Context to manage the data feature
     */
    public DataFeature(DaalContext context) {
        super(context);
        this.cObject = init();
    }

    /**
     * Sets PMML data type of the feature
     * @param pmmlType      PMML data type of the feature
     */
    public void setPMMLNumType(DataFeatureUtils.PMMLNumType pmmlType) {
        cSetPMMLNumType(this.cObject, pmmlType.getType());
    }

    /**
     * Sets  type of the feature(continuous, ordinal or categorical)
     * @param featureType   Type of the feature
     */
    public void setFeatureType(DataFeatureUtils.FeatureType featureType) {
        cSetFeatureType(this.cObject, featureType.getType());
    }

    /**
     * Sets number of category levels
     * @param categoryNumber    Number of category levels
     */
    public void setCategoryNumber(int categoryNumber) {
        cSetCategoryNumber(this.cObject, categoryNumber);
    }

    /**
     * @private
     */
    void setType(Class<?> cls) {
        if (Double.class == cls || double.class == cls) {
            type = Double.class;
            cSetDoubleType(this.cObject);
        } else if (Float.class == cls || float.class == cls) {
            type = Float.class;
            cSetFloatType(this.cObject);
        } else if (Long.class == cls || long.class == cls) {
            type = Long.class;
            cSetLongType(this.cObject);
        } else if (Integer.class == cls || int.class == cls) {
            type = Integer.class;
            cSetIntType(this.cObject);
        } else {
            throw new IllegalArgumentException("type unsupported");
        }
    }

    /**
     * Gets PMML data type of the feature
     *
     * @return PMML data type of the feature
     */
    public native DataFeatureUtils.PMMLNumType getPMMLNumType();

    /**
     * Gets  type of the feature(continuous, ordinal or categorical)
     *
     * @return Type of the feature
     */
    public native DataFeatureUtils.FeatureType getFeatureType();

    /**
     * Gets number of category levels
     *
     * @return Number of category levels
     */
    public native int getCategoryNumber();

    /* Constructs C++ data feature object */
    private native long init();

    private native void cSetInternalNumType(long cObject, int intType);

    private native void cSetPMMLNumType(long cObject, int pmmlType);

    private native void cSetFeatureType(long cObject, int featureType);

    private native void cSetCategoryNumber(long cObject, int categoryNumber);

    private native void cSetName(long cObject, String name);

    private native void cSetDoubleType(long cObject);

    private native void cSetFloatType(long cObject);

    private native void cSetLongType(long cObject);

    private native void cSetIntType(long cObject);

    private native ByteBuffer cSerializeCObject(long cObject);
    private native long cDeserializeCObject(ByteBuffer buffer, long size);

    /** @private */
    Class<?> type;
}
/** @} */
