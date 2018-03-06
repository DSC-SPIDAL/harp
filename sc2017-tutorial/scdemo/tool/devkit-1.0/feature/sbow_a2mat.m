function image_feats = sbow_a2mat(txt_file, id_file)

[dummy, result] = system( ['wc -l ', txt_file] );
numlines = sscanf( result,'%d',1 );

ids = {};
if ~isempty(id_file)
    f = fopen(id_file);
    C = textscan(f, '%s');
    ids = C{1};
    fclose(f);
    assert(numel(ids)==numlines);
end

feat = cell(numlines,1);

fid = fopen(txt_file);

fields = {'word','x','y','scale','norm'};
i = 1;
while 1
    line = fgetl(fid);
    if ~ischar(line)
        break;
    end
    s = sscanf(line,'%d %f %f %d %f',[5, inf]);

    feat{i} = cell2struct(mat2cell(s,ones(1,5),size(s,2)), fields, 1);
    i = i +1;
end

fclose(fid);

assert(numel(feat)==numlines)

image_feats = struct('ID',ids, 'sbow', feat);

image_feats = compress_sbow(image_feats);