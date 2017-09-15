/* file: WriteableData.java */
/*******************************************************************************
* Copyright 2017 Intel Corporation
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

package DAAL;

import org.apache.hadoop.io.*;
import java.io.*;
import com.intel.daal.services.*;
import com.intel.daal.data_management.data.*;

/**
 * @brief Model is the base class for classes that represent models, such as
 * a linear regression or support vector machine (SVM) classifier.
 */
public class WriteableData implements Writable {

    protected int id;
    protected SerializableBase value;

    public WriteableData() {
        this.id    = 0;
        this.value = null;
    }

    public WriteableData( SerializableBase val ) {
        this.id    = 0;
        this.value = val;
        val.pack();
    }

    public WriteableData( int id, SerializableBase val ) {
        this.id    = id;
        this.value = val;
        val.pack();
    }

    public int getId() {
        return this.id;
    }

    public ContextClient getObject(DaalContext context) {
        this.value.unpack(context);
        return this.value;
    }

    public void write(DataOutput out) throws IOException {
        ByteArrayOutputStream outputByteStream = new ByteArrayOutputStream();
        ObjectOutputStream outputStream = new ObjectOutputStream(outputByteStream);

        outputStream.writeObject(value);

        byte[] buffer = outputByteStream.toByteArray();

        out.writeInt(buffer.length);
        out.write(buffer);

        out.writeInt(id);
    }

    public void readFields(DataInput in) throws IOException {
        int length = in.readInt();

        byte[] buffer = new byte[length];
        in.readFully(buffer);

        ByteArrayInputStream inputByteStream = new ByteArrayInputStream(buffer);
        ObjectInputStream inputStream = new ObjectInputStream(inputByteStream);

        try {
            this.value = (SerializableBase)inputStream.readObject();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            this.value = null;
        }

        id = in.readInt();
    }
}
