/* file: DataCollection.java */
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
 * @defgroup data_model Data Model
 * @brief Contains classes that provide functionality of Collection container for objects derived from SerializableBase
 * @ingroup data_management
 * @{
 */
package com.intel.daal.data_management.data;

import com.intel.daal.services.DaalContext;

/**
 *  <a name="DAAL-CLASS-DATA_MANAGEMENT__DATA__DATACOLLECTION"></a>
 *  @brief Class that provides functionality of the Collection container for Serializable objects
 */
public class DataCollection extends SerializableBase {

    /** @private */
    static {
        System.loadLibrary("JavaAPI");
    }

    /**
     * Constructs the collection
     * @param context   Context to manage the collection
     */
    public DataCollection(DaalContext context) {
        super(context);
        this.cObject = cNewDataCollection();
    }

    public DataCollection(DaalContext context, long cDataCollection) {
        super(context);
        this.cObject = cDataCollection;
        this.serializedCObject = null;
    }

    public long size() {
        return cSize(this.cObject);
    }

    public SerializableBase get(long idx) {
        return Factory.instance().createObject(getContext(), cGetValue(this.cObject, idx));
    }

    public void set(SerializableBase value, long idx) {
        cSetValue(this.cObject, value.getCObject(), idx);
    }

    private native long cNewDataCollection();

    private native long cSize(long cDataCollectionAddr);

    private native long cGetValue(long cDataCollectionAddr, long idx);

    private native void cSetValue(long cDataCollectionAddr, long cValueAddr, long idx);
}
/** @} */
