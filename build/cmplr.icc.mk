#================================================== -*- makefile -*- vim:ft=make
# Copyright 2012-2018 Intel Corporation
# All Rights Reserved.
#
# If this  software was obtained  under the  Intel Simplified  Software License,
# the following terms apply:
#
# The source code,  information  and material  ("Material") contained  herein is
# owned by Intel Corporation or its  suppliers or licensors,  and  title to such
# Material remains with Intel  Corporation or its  suppliers or  licensors.  The
# Material  contains  proprietary  information  of  Intel or  its suppliers  and
# licensors.  The Material is protected by  worldwide copyright  laws and treaty
# provisions.  No part  of  the  Material   may  be  used,  copied,  reproduced,
# modified, published,  uploaded, posted, transmitted,  distributed or disclosed
# in any way without Intel's prior express written permission.  No license under
# any patent,  copyright or other  intellectual property rights  in the Material
# is granted to  or  conferred  upon  you,  either   expressly,  by implication,
# inducement,  estoppel  or  otherwise.  Any  license   under such  intellectual
# property rights must be express and approved by Intel in writing.
#
# Unless otherwise agreed by Intel in writing,  you may not remove or alter this
# notice or  any  other  notice   embedded  in  Materials  by  Intel  or Intel's
# suppliers or licensors in any way.
#
#
# If this  software  was obtained  under the  Apache License,  Version  2.0 (the
# "License"), the following terms apply:
#
# You may  not use this  file except  in compliance  with  the License.  You may
# obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
#
#
# Unless  required  by   applicable  law  or  agreed  to  in  writing,  software
# distributed under the License  is distributed  on an  "AS IS"  BASIS,  WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#
# See the   License  for the   specific  language   governing   permissions  and
# limitations under the License.
#===============================================================================

#++
#  Intel compiler defenitions for makefile
#--

PLATs.icc = lnx32e lnx32 win32e win32 mac32 mac32e

CMPLRDIRSUFF.icc =

CORE.SERV.COMPILER.icc = generic
-DEBC.icc = $(if $(OS_is_win),-debug:all -Z7,-g)

-Zl.icc = $(if $(OS_is_win),-Zl,) -mGLOB_freestanding=TRUE -mCG_no_libirc=TRUE
-Qopt = $(if $(OS_is_win),-Qopt-,-qopt-)

COMPILER.lnx.icc  = $(if $(COVFILE),cov01 -1; covc -i )icc -Werror -qopenmp-simd -Wreturn-type
COMPILER.lnx.icc += $(if $(COVFILE), $(if $(IA_is_ia32), $(-Q)m32, $(-Q)m64))

# add by harpdaal
COMPILER.lnx.icc += -I$(HDFSDIR.include) -I$(MEMKINDDIR.include) -O3 -wn2

COMPILER.win.icc = icl -nologo -WX -Qopenmp-simd
COMPILER.mac.icc = icc -Werror -stdlib=libstdc++ -mmacosx-version-min=10.11 -Wreturn-type

# icc 16 does not support -qopenmp-simd option on macOS*
ifeq ($(if $(OS_is_mac),$(shell icc --version | grep "icc (ICC) 16"),),)
    COMPILER.mac.icc += -qopenmp-simd
endif

link.dynamic.lnx.icc = icc -no-cilk
link.dynamic.mac.icc = icc

daaldep.lnx32e.rt.icc = -static-intel
daaldep.lnx32.rt.icc  = -static-intel

p4_OPT.icc   = $(-Q)$(if $(OS_is_mac),$(if $(IA_is_ia32),xSSE3,xSSSE3),xSSE2)
mc_OPT.icc   = $(-Q)$(if $(PLAT_is_mac32),xSSE3,xSSSE3)
mc3_OPT.icc  = $(-Q)xSSE4.2
avx_OPT.icc  = $(-Q)xAVX
avx2_OPT.icc = $(-Q)xCORE-AVX2
knl_OPT.icc  = $(if $(OS_is_mac),$(-Q)xCORE-AVX2,$(-Q)xMIC-AVX512)
skx_OPT.icc  = $(-Q)xCORE-AVX512 $(-Qopt)zmm-usage=high
#TODO add march opts in GCC style
