/* file: TransformInput.java */
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
 * @ingroup pca_transform
 * @{
 */
/**
 * @brief Contains classes for computing the PCA transformation
 */
package com.intel.daal.algorithms.pca.transform;

import com.intel.daal.algorithms.Precision;
import com.intel.daal.data_management.data.Factory;
import com.intel.daal.data_management.data.NumericTable;
import com.intel.daal.data_management.data.KeyValueDataCollection;
import com.intel.daal.services.DaalContext;

/**
 * <a name="DAAL-CLASS-ALGORITHMS__PCA__TRANSFORM__INPUT"></a>
 * @brief %Input objects for the PCA transformation algorithm
 */
public final class TransformInput extends com.intel.daal.algorithms.Input {
    /** @private */
    static {
        System.loadLibrary("JavaAPI");
    }

    public TransformInput(DaalContext context, long cObject) {
        super(context, cObject);
    }

    /**
     * Sets the input object of the PCA transformation algorithm
     * @param id    Identifier of the input object
     * @param val   Value of the input object
     */
    public void set(TransformInputId id, NumericTable val) {
        if (id == TransformInputId.data || id == TransformInputId.eigenvectors) {
            cSetInputTable(cObject, id.getValue(), val.getCObject());
        }
        else {
            throw new IllegalArgumentException("id unsupported");
        }
    }

    /**
     * Returns the input object of the PCA transformation algorithm
     * @param id Identifier of the input object
     * @return   Input object that corresponds to the given identifier
     */
    public NumericTable get(TransformInputId id) {
        if (id == TransformInputId.data || id == TransformInputId.eigenvectors) {
            return (NumericTable)Factory.instance().createObject(getContext(), cGetInputTable(cObject, id.getValue()));
        }
        else {
            throw new IllegalArgumentException("id unsupported");
        }
    }

    /**
     * Sets the input object of the PCA transformation algorithm
     * @param id    Identifier of the input object
     * @param val   Value of the input object
     */
    public void set(TransformDataInputId id, KeyValueDataCollection val) {
        if (id == TransformDataInputId.dataForTransform) {
            cSetInputTransformData(cObject, id.getValue(), val.getCObject());
        }
        else {
            throw new IllegalArgumentException("id unsupported");
        }
    }

    /**
     * Returns the input object of the PCA transformation algorithm
     * @param id Identifier of the input object
     * @return   Input object that corresponds to the given identifier
     */
    public KeyValueDataCollection get(TransformDataInputId id) {
        if (id == TransformDataInputId.dataForTransform) {
            return (KeyValueDataCollection)Factory.instance().createObject(getContext(), cGetInputTransformData(cObject, id.getValue()));
        }
        else {
            throw new IllegalArgumentException("id unsupported");
        }
    }

    /**
     * Sets the input object of the PCA transformation algorithm
     * @param wid   Identifier of the transform input object
     * @param id    Identifier of the component
     * @param val   Value of the input object
     */
    public void set(TransformDataInputId wid, TransformComponentId id, NumericTable val) {
        if (wid == TransformDataInputId.dataForTransform &&
           (id == TransformComponentId.eigenvalue || id == TransformComponentId.mean || id == TransformComponentId.variance)) {
                cSetInputTransformComponent(cObject, wid.getValue(), id.getValue(), val.getCObject());
        }
        else {
            throw new IllegalArgumentException("id unsupported");
        }
    }

    /**
     * Returns the input object of the PCA transformation algorithm
     * @param wid   Identifier of the transform input object
     * @param id    Identifier of the component
     * @return      Input object that corresponds to the given identifier
     */
    public NumericTable get(TransformDataInputId wid, TransformComponentId id) {
        if (wid == TransformDataInputId.dataForTransform &&
           (id == TransformComponentId.eigenvalue || id == TransformComponentId.mean || id == TransformComponentId.variance)) {
            return (NumericTable)Factory.instance().createObject(getContext(), cGetInputTransformComponent(cObject, wid.getValue(), id.getValue()));
        }
        else {
            throw new IllegalArgumentException("id unsupported");
        }
    }


    private native void cSetInputTable(long cObject, int id, long ntAddr);
    private native long cGetInputTable(long cObject, int id);
    private native void cSetInputTransformData(long cObject, int id, long ntAddr);
    private native long cGetInputTransformData(long cObject, int id);
    private native void cSetInputTransformComponent(long cObject, int wid, int id, long ntAddr);
    private native long cGetInputTransformComponent(long cObject, int wid, int id);

}
/** @} */
