function [tpav,tpstd] = throughput(S,step,print)

if ~exist('step','var') || isempty(step)
    step = 5;
end
if ~exist('print','var') || isempty(print)
   print = true; 
end

i = 1;
tp = [];
cstep = 0.5;
while i <= size(S,1)
    n = 0;
    ts = S(i,8);
    t = ts;
    while t < ts+cstep*1000 && i <= size(S,1)
        t = S(i,8);
        n = n+1;
        i = i+1;
    end
    tp = [tp;n/cstep];
end

npa = step/cstep;
na = length(tp)/npa;
tpav = zeros(ceil(na),1);
tpstd = zeros(ceil(na),1);
for i = 1:na
   tpav(i) = mean(tp((i-1)*npa+1:i*npa));
   tpstd(i) = std(tp((i-1)*npa+1:i*npa));
end
tpav(end) = mean(tp(floor(na)*npa+1:end));
tpstd(end) = std(tp(floor(na)*npa+1:end));

if print
    figure;
    errorbar(((1:ceil(na))-1)*step,tpav,tpstd);
    
    ylim([0,ceil(max(tpav+tpstd))]);
    xlim([-step,length(tpav)*step]);
end

end