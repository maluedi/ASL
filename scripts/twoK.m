stats = zeros(32,9);

lbl = ['connections','workers','clients','message size','think time','average responsetime','std of responstime','average throughput','std of throughput'];
i = 1;
for d = [4,16]
    for w = [4,16]
        for c = [4,64]
            for m = [200,2000]
                for t = [10,500]
                    [C,S] = readLog('2kfactorialTake2',sprintf('%d_%d_%d_%d_%d',d,w,c,m,t));
                    meanRT = mean(C(:,4)-C(:,3));
                    stdRT = std(C(:,4)-C(:,3));
                    et = sort(C(:,4));
                    meanTP = mean(100000./(et(101:end)-et(1:end-100)));
                    stdTP = std(100000./(et(101:end)-et(1:end-100)));
                    stats(i,:) = [d,w,c,m,t,meanRT,stdRT,meanTP,stdTP];
                    
                    responsetime(C,S);
                    title(sprintf('rt: %d %d %d %d %d',d,w,c,m,t));
                    
                    throughput(S);
                    title(sprintf('tp: %d %d %d %d %d',d,w,c,m,t));
                    i = i + 1;
                end
            end
        end
    end
end

[~,Irt] = sort(stats(:,6));
[~,Itp] = sort(stats(:,8));