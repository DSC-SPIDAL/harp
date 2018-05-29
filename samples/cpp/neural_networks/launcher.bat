@@echo off
rem ============================================================================
rem Copyright 2017-2018 Intel Corporation
rem All Rights Reserved.
rem
rem If this software  was obtained under the Intel  Simplified Software License,
rem the following terms apply:
rem
rem The source code,  information and material ("Material")  contained herein is
rem owned by Intel Corporation or its suppliers or licensors,  and title to such
rem Material remains with Intel Corporation or its suppliers  or licensors.  The
rem Material contains  proprietary  information  of Intel  or its  suppliers and
rem licensors.  The Material is protected by worldwide copyright laws and treaty
rem provisions.  No part  of the  Material   may be  used,  copied,  reproduced,
rem modified,  published,   uploaded,   posted,   transmitted,   distributed  or
rem disclosed in any way without Intel's prior  express written  permission.  No
rem license under any patent,  copyright  or other intellectual  property rights
rem in the Material is granted to or conferred upon you,  either  expressly,  by
rem implication,  inducement,  estoppel  or otherwise.  Any  license  under such
rem intellectual  property  rights must  be  express and  approved  by  Intel in
rem writing.
rem
rem Unless otherwise  agreed by  Intel in writing,  you may not  remove or alter
rem this notice or  any other notice  embedded in Materials by  Intel or Intel's
rem suppliers or licensors in any way.
rem
rem
rem If this software was obtained  under the Apache  License,  Version  2.0 (the
rem "License"), the following terms apply:
rem
rem You may not  use this file  except in compliance  with the License.  You may
rem obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
rem
rem
rem Unless  required  by  applicable  law  or  agreed  to in  writing,  software
rem distributed under the License is distributed  on an "AS IS"  BASIS,  WITHOUT
rem WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
rem
rem See the  License  for  the  specific  language  governing   permissions  and
rem limitations under the License.
rem ============================================================================

::  Content:
::     Intel(R) Data Analytics Acceleration Library samples creation and run
::******************************************************************************

set ARCH=%1
set RMODE=%2

set errorcode=0

if "%1"=="help" (
    goto :Usage
)

if "%DATA_PATH%"=="" (
    set DATA_PATH=./data
)

if not "%ARCH%"=="ia32" if not "%ARCH%"=="intel64" (
    echo Bad first argument, must be ia32 or intel64
    set errorcode=1
    goto :Usage
)

if not "%RMODE%"=="build" if not "%RMODE%"=="run" if not "%RMODE%"=="" (
    echo Bad second argument, must be build or run
    set errorcode=1
    goto :Usage
)

goto :CorrectArgs

:Usage
echo Usage: launcher.bat ^{arch^|help^} [rmode]
echo arch  - can be ia32 or intel64
echo rmode - optional parameter, can be build (for building samples only) or
echo         run (for running samples only).
echo         If not specified build and run are performed.
echo help  - print this message
exit /b errorcode

:CorrectArgs

set RESULT_DIR=_results\%ARCH%

if not exist %RESULT_DIR% md %RESULT_DIR%

echo %RESULT_DIR%

set CFLAGS=-nologo -w -DDAAL_CHECK_PARAMETER
set LFLAGS=-nologo
set LIB_DAAL=daal_core.lib daal_thread.lib
set LIB_DAAL_DLL=daal_core_dll.lib
set LFLAGS_DAAL=%LIB_DAAL% tbb.lib tbbmalloc.lib
set NEURAL_NETWORKS_LOGFILE=.\%RESULT_DIR%\build_NEURAL_NETWORKS.log
if not "%RMODE%"=="run" (
    if exist %NEURAL_NETWORKS_LOGFILE% del /Q /F %NEURAL_NETWORKS_LOGFILE%
)
set NEURAL_NETWORKS_CPP_PATH=sources
if not defined NEURAL_NETWORKS_SAMPLE_LIST (
    call .\daal.lst.bat
)

setlocal enabledelayedexpansion enableextensions

for %%T in (%NEURAL_NETWORKS_SAMPLE_LIST%) do (
    if not "%RMODE%"=="run" (
        echo call icl -c %CFLAGS% %NEURAL_NETWORKS_CPP_PATH%\%%T.cpp -Fo%RESULT_DIR%\%%T.obj 2>&1 >> %NEURAL_NETWORKS_LOGFILE%
        call      icl -c %CFLAGS% %NEURAL_NETWORKS_CPP_PATH%\%%T.cpp -Fo%RESULT_DIR%\%%T.obj 2>&1 >> %NEURAL_NETWORKS_LOGFILE%
        echo call icl %LFLAGS% %RESULT_DIR%\%%T.obj %LIB_DAAL%     %NEURAL_NETWORKS_LIB% ws2_32.lib -Fe%RESULT_DIR%\%%T.exe     2>&1 >> %NEURAL_NETWORKS_LOGFILE%
        call      icl %LFLAGS% %RESULT_DIR%\%%T.obj %LIB_DAAL%     %NEURAL_NETWORKS_LIB% ws2_32.lib -Fe%RESULT_DIR%\%%T.exe     2>&1 >> %NEURAL_NETWORKS_LOGFILE%
        echo call icl %LFLAGS% %RESULT_DIR%\%%T.obj %LIB_DAAL_DLL% %NEURAL_NETWORKS_LIB% ws2_32.lib -Fe%RESULT_DIR%\%%T_dll.exe 2>&1 >> %NEURAL_NETWORKS_LOGFILE%
        call      icl %LFLAGS% %RESULT_DIR%\%%T.obj %LIB_DAAL_DLL% %NEURAL_NETWORKS_LIB% ws2_32.lib -Fe%RESULT_DIR%\%%T_dll.exe 2>&1 >> %NEURAL_NETWORKS_LOGFILE%
    )
    if not "%RMODE%"=="build" (
        for %%U in (%%T %%T_dll) do (
            if exist .\%RESULT_DIR%\%%U.exe (
                .\%RESULT_DIR%\%%U.exe %DATA_PATH% 1>.\%RESULT_DIR%\%%U.res 2>&1
                if "!errorlevel!" == "0" (
                    echo %time% PASSED %%U
                ) else (
                    echo %time% FAILED %%U with errno !errorlevel!
                )
            ) else (
                echo %time% BUILD FAILED %%U
            )
        )
    )
)

endlocal

exit /B %ERRORLEVEL%

:out