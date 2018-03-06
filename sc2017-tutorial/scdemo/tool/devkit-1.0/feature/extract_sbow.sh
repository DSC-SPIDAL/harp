#!/bin/bash

VLFEAT_LIB=3rd-party/vlfeat/bin/g64 #you might need to change it depending on your platform
export LD_LIBRARY_PATH=${LD_LIBRARY_PATH}:$VLFEAT_LIB

input=$1
id_list=$2
output=$3

keep_raw=1

matlab="matlab -nosplash -nodesktop -nojvm -nodisplay"

sbow_txt=$output.sbow.txt
sbow_mat=$output.sbow.mat

vldsift_bin=$output.vldsift.bin
vldsift_txt=$output.vldsift.txt
vldsift_mat=$output.vldsift.mat

./vldsift_ms_kv.sh < $input > $vldsift_bin

rm -f $vldsift_mat
rm -f $sbow_mat

if [[ $keep_raw = 1 ]]; then
    ./feat_b2a vldsift < $vldsift_bin > $vldsift_txt
    echo "image_vldsift = vldsift_a2mat('$vldsift_txt','$id_list'); save('$vldsift_mat','image_vldsift')"  | $matlab
    rm $vldsift_txt
fi

./vq -codebook codebook.txt < $vldsift_bin | ./feat_b2a sbow > $sbow_txt
echo "image_sbow = sbow_a2mat('$sbow_txt','$id_list'); save('$sbow_mat','image_sbow')"  | $matlab

rm $vldsift_bin
rm $sbow_txt

