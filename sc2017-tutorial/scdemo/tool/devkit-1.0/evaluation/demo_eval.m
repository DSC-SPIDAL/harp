%this script demos the usage of evaluation routines
% the result file 'demo.val.pred.txt' on validation data is evaluated
% against the ground truth

load('../data/meta.mat');

pred_file='demo.val.pred.txt'
ground_truth_file='../data/ILSVRC2010_validation_ground_truth.txt'

num_predictions_per_image=5;

error_flat=zeros(num_predictions_per_image,1);
error_hie=zeros(num_predictions_per_image,1);

for i=1:num_predictions_per_image
    error_flat(i) = eval_flat(pred_file,ground_truth_file, i);
    error_hie(i) = eval_hie(pred_file,ground_truth_file,cost_matrix, i);
end

disp('# guesses  vs flat error');
disp([(1:num_predictions_per_image)',error_flat]);

disp('# guesses vs hierarchical error');
disp([(1:num_predictions_per_image)',error_hie]);

