
tagname=$1
if [ -z $tagname ] ; then
    tagname=-byte-blockdense-longcube
fi

echo "make $tagname"

 make clean_all
 make -j 24 USE_COMPACT=1 USE_OMP_BUILDHIST=1 USE_BINID=1 USE_COMPACT_BINID=1 TAGNAME=$tagname-release USE_HALFTRICK=1
 make clean_all
 make -j 24 USE_COMPACT=1 USE_OMP_BUILDHIST=1 USE_BINID=1 USE_COMPACT_BINID=1 TAGNAME=$tagname-release
# make clean_all
# make -j 24 USE_COMPACT=1 USE_OMP_BUILDHIST=1 USE_BINID=1 USE_COMPACT_BINID=1 TAGNAME=$tagname-debug USE_DEBUG=1
# make clean_all
# make -j 24 USE_COMPACT=1 USE_OMP_BUILDHIST=1 USE_BINID=1 USE_COMPACT_BINID=1 TAGNAME=$tagname-debug USE_HALFTRICK=1 USE_DEBUG=1

#cp *longcube-debug ~/tmp/optgbt/trainers/
