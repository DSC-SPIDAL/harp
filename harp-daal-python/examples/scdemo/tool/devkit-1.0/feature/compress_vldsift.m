function input = compress_vldsift(input)

for i=1:numel(input)
    input(i).vldsift = compress_v(input(i).vldsift);
end

function w=compress_v(w)
w.x = single(w.x);
w.y = single(w.y);
w.scale = int8(w.scale);
w.norm = single(w.norm);
w.desc = single(w.desc);
