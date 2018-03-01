% Demo system for ILSVRC2010
% Train one-against-all linear SVMs on precomputed bag of words features
% and test on validation data

%---------------------------------------------------------------------------------------
% Settings
%---------------------------------------------------------------------------------------
train_feat_path='../data/features/train/';
val_feat_path = '../data/features/val/';

if ispc
    path(path,'./3rd-party/liblinear-1.5/windows');
else 
    path(path,'./3rd-party/liblinear-1.5/matlab');
end

path(path,'../evaluation');

load '../data/meta.mat';

codebook_size=1000;

%number of classes
K=1000;

max_num_train_per_synset = 100;  

num_predictions_per_image = 5;


%---------------------------------------------------------------------------------------
% Prepare training data
%---------------------------------------------------------------------------------------

train_sizes = [synsets.num_train_images];
train_sizes = train_sizes(1:K);

train_sizes(train_sizes > max_num_train_per_synset) = max_num_train_per_synset;

m = sum(train_sizes);
training_label_vector = zeros(m, 1);
training_instance_matrix = zeros(m, codebook_size);

if 1
    disp('loading training data...this can take 10min');
    tic
    start=1;
    for i=1:K
        mysize = train_sizes(i);

        training_label_vector(start:start+mysize-1) = i;

        S = load([train_feat_path,synsets(i).WNID,'.sbow.mat']);
        assert(mysize<=numel(S.image_sbow));
        for j=1:mysize
            %convert to bag of words histogram
            x = histc(S.image_sbow(j).sbow.word,0:codebook_size-1);
            x = x / norm(x);
            training_instance_matrix(start+j-1,:) = x;
        end
        start = start + mysize;
        clear S;
    end
    training_instance_matrix = sparse(training_instance_matrix);
    save('demo.train.mat','training_label_vector', ...
         'training_instance_matrix');
    toc
else
    load 'demo.train.mat';
end

%---------------------------------------------------------------------------------------
% Train using LIBLINEAR
%---------------------------------------------------------------------------------------

if 1
    disp('training...this can take 20min');
    tic
    model = train(training_label_vector, training_instance_matrix);
    save('demo.model.mat','model');
    toc
else
    load 'demo.model.mat';
end


%---------------------------------------------------------------------------------------
% Test on validation data
%---------------------------------------------------------------------------------------

validation_size=50000;
testing_label_vector=zeros(validation_size,1);
testing_instance_matrix=zeros(validation_size,codebook_size);

if 1
    disp('loading validation data...');
    tic
    start=1;
    mysize=1000;
    for i=1:50
        S = load([val_feat_path,'val.',num2str(i,'%.4d'),'.sbow.mat']);
        assert(numel(S.image_sbow)==mysize);
        for j=1:mysize
            %convert to bag of words histogram
            x = histc(S.image_sbow(j).sbow.word,0:codebook_size-1);
            x = x / norm(x);
            testing_instance_matrix(start+j-1,:) = x;
        end
        start = start + mysize;
        clear S;
    end
    testing_instance_matrix = sparse(testing_instance_matrix);
    save('demo.val.mat','testing_label_vector', ...
         'testing_instance_matrix');
    toc
else
    load 'demo.val.mat';
end

if 1
    disp('testing...');
    tic
    [pred_ignore, accuracy_ignore, dec_values] = predict(testing_label_vector, ...
                                 testing_instance_matrix, ...
                                 model);
    toc
end

% make sure the order of the decision values from liblinear is the same as
% the order of the classes
for i=1:K
    assert(model.Label(i)==i);
end

% predict the top labels
[scores,pred]=sort(dec_values,2,'descend');
pred = pred(:,1:num_predictions_per_image);
scores = pred(:,1:num_predictions_per_image);

% write out predicted labels
dlmwrite('demo.val.pred.txt',pred,'delimiter',' ');

%evaluation
error_flat=zeros(num_predictions_per_image,1);
error_hie=zeros(num_predictions_per_image,1);

for i=1:num_predictions_per_image
    error_flat(i) = eval_flat('demo.val.pred.txt','../data/ILSVRC2010_validation_ground_truth.txt', i);
    error_hie(i) = eval_hie('demo.val.pred.txt','../data/ILSVRC2010_validation_ground_truth.txt',...
         cost_matrix, i);
end

disp('# guesses  vs flat error');
disp([(1:num_predictions_per_image)',error_flat]);

disp('# guesses vs hierarchical error');
disp([(1:num_predictions_per_image)',error_hie]);

