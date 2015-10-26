function [C,S] = readLog(experiment,name)
%READLOG Summary of this function goes here
%   Detailed explanation goes here

logDir = strcat('E:\Users\Marcel\Documents\ETH\ASL\logs\',experiment,'\');

d = dir(logDir);

Stemp = [];
Ctemp = [];
for i=1:length(d)
    if ~isempty(regexp(d(i).name,strcat('\w*',name,'\w*'), 'once'))
        if ~isempty(regexp(d(i).name,'s\w*', 'once'))
            Stemp = [Stemp;csvread(strcat(logDir,d(i).name))];
        end
        if ~isempty(regexp(d(i).name,'c\w*', 'once'))
            Ctemp = [Ctemp;csvread(strcat(logDir,d(i).name))];
        end
    end
end

% s1 = csvread(strcat(logDir,'s1_',name,'.log'));
% s2 = csvread(strcat(logDir,'s2_',name,'.log'));
% 
% Stemp = [s1;s2];
[~,I] = sort(Stemp(:,3));

S = Stemp(I,:);
S = S(S(:,1)==1 | S(:,1)==2 | S(:,1)==6,:);

% c1 = csvread(strcat(logDir,'c1_',name,'.log'));
% c2 = csvread(strcat(logDir,'c2_',name,'.log'));
% c3 = csvread(strcat(logDir,'c3_',name,'.log'));
% c4 = csvread(strcat(logDir,'c4_',name,'.log'));
% 
% Ctemp = [c1;c2;c3;c4];
[~,I] = sort(Ctemp(:,3));

C = Ctemp(I,:);

end