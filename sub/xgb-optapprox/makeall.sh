
tagname=$1
if [ -z $tagname ] ; then
    tagname=blockdense
fi

echo "make $tagname"

 make clean_all
 make -j 24 TAGNAME=$tagname USE_HALFTRICK=0 USE_SPLIT_PARALLELONNODE=1 USE_VTUNE=0 USE_DEBUG=0 DEBUG=0
 make -j 24 TAGNAME=$tagname USE_HALFTRICK=1 USE_SPLIT_PARALLELONNODE=1 USE_VTUNE=0 USE_DEBUG=0 DEBUG=0
 make -j 24 TAGNAME=$tagname USE_HALFTRICK=0 USE_SPLIT_PARALLELONNODE=1 USE_VTUNE=0 USE_DEBUG=1 DEBUG=1
 make -j 24 TAGNAME=$tagname USE_HALFTRICK=1 USE_SPLIT_PARALLELONNODE=1 USE_VTUNE=0 USE_DEBUG=1 DEBUG=1

#cp *longcube-debug ~/tmp/optgbt/trainers/
