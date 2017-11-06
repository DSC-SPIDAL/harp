function image_feats = vldsift_a2mat(txt_file, id_file)

[dummy, result] = system( ['wc -l ', txt_file] );
numlines = sscanf( result,'%d',1 );

if ~isempty(id_file)
    f = fopen(id_file);
    C = textscan(f, '%s');
    ids = C{1};
    fclose(f);
    assert(numel(ids)==numlines);
end

feat = cell(numlines,1);

fid = fopen(txt_file);

fields = {'x','y','scale','norm','desc'};
i = 1;
while 1
    line = fgetl(fid);
    if ~ischar(line)
        break;
    end
    s = sscanf(line,'%f',[132, inf]);
    
    feat{i} = cell2struct(mat2cell(s,[1 1 1 1 128],size(s,2)), fields, 1);
    i = i +1;
end

assert(numel(feat)==numlines)

fclose(fid);

image_feats = struct('ID',ids, 'vldsift', feat);

image_feats = compress_vldsift(image_feats);
