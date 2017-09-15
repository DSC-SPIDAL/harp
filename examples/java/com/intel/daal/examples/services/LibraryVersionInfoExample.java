/* file: LibraryVersionInfoExample.java */
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

/*
//  Content:
//  Intel(R) DAAL version information
////////////////////////////////////////////////////////////////////////////////
*/

/**
 * <a name="DAAL-EXAMPLE-JAVA-LIBRARYVERSIONINFOEXAMPLE">
 * @example LibraryVersionInfoExample.java
 */

package com.intel.daal.examples.services;

import com.intel.daal.services.CpuTypeEnable;
import com.intel.daal.services.Environment;
import com.intel.daal.services.LibraryVersionInfo;

class LibraryVersionInfoExample {
    public static void main(String[] args) throws java.io.FileNotFoundException, java.io.IOException {
        LibraryVersionInfo vi = new LibraryVersionInfo();

        System.out.println("Major Version:  " + vi.getMajorVersion());
        System.out.println("Minor Version:  " + vi.getMinorVersion());
        System.out.println("Update Version: " + vi.getUpdateVersion());
        System.out.println("Product:        " + vi.getProductStatus());
        System.out.println("Build:          " + vi.getBuild());
        System.out.println("Build: revision " + vi.getBuildRev());
        System.out.println("Name:           " + vi.getName());
        System.out.println("Processor:      " + vi.getProcessor());
    }
}
