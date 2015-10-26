function [rtavc,rtstdc,rtavs,rtstds] = responsetime(C,S,step,print)

if ~exist('step','var') || isempty(step)
    step = 5;
end
if ~exist('print','var') || isempty(print)
   print = true; 
end

i = 1;
rtavc = [];
rtstdc = [];
while i <= size(C,1)
    ts = C(i,3);
    t = ts;
    rti = [];
    while t < ts+step*1000 && i <= size(C,1)
        t = C(i,3);
        rti = [rti;C(i,4)-C(i,3)];
        i = i+1;
    end
    rtavc = [rtavc;mean(rti)];
    rtstdc = [rtstdc;std(rti)];
end


i = 1;
rtavs = [];
rtstds = [];
while i <= size(S,1)
    ts = S(i,3);
    t = ts;
    rti = [];
    while t < ts+step*1000 && i <= size(S,1)
        t = S(i,3);
        rti = [rti;S(i,4)-S(i,3),S(i,5)-S(i,4),S(i,6)-S(i,5),S(i,7)-S(i,6),S(i,8)-S(i,7)];
        i = i+1;
    end
    rtavs = [rtavs;mean(rti)];
    rtstds = [rtstds;std(rti)];
end

if print
    figure; 
    hold on;
    errorbar(((1:length(rtavc))-1)*step,rtavc,rtstdc);
    errorbar(((1:size(rtavs,1))-1)*step,rtavs(:,1),rtstds(:,1));
    errorbar(((1:size(rtavs,1))-1)*step,rtavs(:,2),rtstds(:,2));
    errorbar(((1:size(rtavs,1))-1)*step,rtavs(:,3),rtstds(:,3));
    errorbar(((1:size(rtavs,1))-1)*step,rtavs(:,4),rtstds(:,4));
    errorbar(((1:size(rtavs,1))-1)*step,rtavs(:,5),rtstds(:,5));

    xlabel('time [s]');ylabel('response time [ms]');
    legend('overall response time','time in socket queue','reading time','waiting for db-connection','db interaction','sending time');

    ylim([0,ceil(max(rtavc+rtstdc))]);
    xlim([-step,length(rtavc)*step]);
end
end