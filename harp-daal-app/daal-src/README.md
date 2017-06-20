# Harp-DAAL-Local 

Harp-DAAL-Local is the implementation of local computation codes of Harp-DAAL-APP. The current codes are forked from DAAL2017 version released by Intel. 
at their github repository: https://github.com/01org/daal DAAL2017 is licensed under Apache License 2.0.
Harp-DAAL-Local has six applications as follows

* K-means
* MF-SGD
* MF-ALS
* SVD
* PCA
* Neural Network

## Installation

The current version has only been tested on Linux X64 platform. Please follow the steps to compile and install Harp-DAAL-Local

### Enter directory and execute the makefile file 

```bash
cd harp/harp-daal-app/daal-src
```
Compile and populate the release directory
```bash
make _release PLAT=lnx32e 
```
Users may use other make objects, such as *clean*, *_daal*, please refer to the content of makefile file. 
The compilation will generate two directories. 

*release_dir* stores the compiled lib files and example files. The name and path of release directory is specified by the variable *RELEASEDIR* in the makefile.
*work_dir* stores the temporary files generated in the compilation. The name and path of work directory is specified by the variable *WORKDIR* in the makefile

### Set environment variables

After the compilation, users shall source the daal.var script in *release_dir* to set up the environment variables such as *DAALROOT*
```bash
source /path-to-release_dir/daal/bin/daalvars.sh
echo $DAALROOT
```

## Development

To add applications to Harp-DAAL-Local, users shall use the APIs provided by DAAL team. A complete application consists of three parts

1. C++ codes in directory daal-src/algorithms/kernel/app-name, which is the implementation of the application
2. Java codes in directory daal-src/lang_interface/java/com/intel/daal/algorithms, which is the Java API of this application. 
3. JNI codes in directory daal-src/lang_service/java/com/intel/daal/algorithms, which combines the C++ codes with the Java API codes

Before compile the newly added application codes, please add the application folder name into *makefile.lst* file. 
If the new application has implemented some serializable objects, please add the correspondent lines for the objects to 
the file daal-src/algorithms/kernel/daal_factory_impl.cpp

```c++
#include "new_app.h"
registerObject(new Creator<algorithms::new_app::Result>());
registerObject(new Creator<algorithms::new_app::PartialResult<algorithms::new_app::method> >());
```

These lines will register *Result* and *PartialResult* objects within *new_app* algorithm.

