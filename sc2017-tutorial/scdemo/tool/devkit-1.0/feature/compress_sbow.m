function input = compress_sbow(input)

for i=1:numel(input)
    input(i).sbow = compress_v(input(i).sbow);
end

function w=compress_v(w)
w.x = single(w.x);
w.y = single(w.y);
w.scale = int8(w.scale);
w.norm = single(w.norm);
w.word = int16(w.word);







