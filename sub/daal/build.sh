rm __work_gnu/lnx32e/kernel/gbt*
rm __work_gnu/lnx32e/kernel_dll/gbt*
#make daal -j 32 PLAT=lnx32e COMPILER=gnu
make daal -j 32 PLAT=lnx32e COMPILER=gnu
