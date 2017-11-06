function error = eval_hie( predict_file, gtruth_file, cost_matrix, max_num_pred_per_image )
% Evaluate hierarchical cost
% predict_file: each line is the predicted labels ( must be positive
% integers, seperated by white spaces ) for one image, sorted by
% confidence in descending order. The number of labels per line can vary,
% but not more than max_num_pred_per_image ( extra labels are ignored ).
% gtruth_file: each line is the ground truth labels, in the same format.

pred = dlmread(predict_file);
gt = dlmread(gtruth_file);

if size(pred,2) > max_num_pred_per_image
    pred = pred(:,1:max_num_pred_per_image);
end

assert(size(pred,1)==size(gt,1));

K = size(cost_matrix,1);
N = size(pred,1);

assert(max(pred(1:end))<= K);
assert(max(gt(1:end))<= K);

big = max(cost_matrix(1:end));

%add a dummy class K+1
cost_matrix = [cost_matrix, ones(K,1)*big; ones(1,K)*big, 0];

%add dummy predictions for each image.
pred = [pred, zeros(N,1)];
pred(pred==0)=K+1;

gt(gt==0)=K+1;

c = zeros(N,1);

for j=1:size(gt,2) %for each ground truth label
    x = gt(:,j) * ones(1,size(pred,2));
    ind = sub2ind(size(cost_matrix),pred,x);
    c = c + min(cost_matrix(ind),[],2);
end

n = sum(gt~=K+1,2);

error = sum(c./n)/N;

