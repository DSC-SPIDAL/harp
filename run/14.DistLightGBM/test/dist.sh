ids=(b c d e f g h)
ips=(101 102 110 111 112 113 126)

id=0
for ip in ${ips[*]}; do
ssh 10.16.0.${ip} "cd `pwd`; ln -s split/higgs_train_${ids[id]} higgs_train.libsvm"
((id++))

done

