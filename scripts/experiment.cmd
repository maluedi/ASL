:: defaults

set nWorkers=16
set nConns=16
set nClients=8
set msgSize=200
set waitTime=10
set runTime=300

set dbMachine=52.29.85.94
set serverMachine=52.29.85.87
set clientMachine=52.29.85.162

set serverMachine2=52.29.79.229
set clientMachine2=52.29.86.89
set clientMachine3=52.28.236.183
set clientMachine4=52.29.52.207


set puttyPath=E:\Users\Marcel\Documents\ETH\ASL\putty
set keyFile=%puttyPath%\keys\ASL_key.ppk

%puttyPath%\plink -ssh ec2-user@%dbMachine% -i %keyFile% -t "sudo service postgresql start"

start /B %puttyPath%\plink -ssh ec2-user@%serverMachine% -i %keyFile% -t "java -jar ASL_Server.jar 1313 %dbMachine%:5432 postgres qwer1 %nWorkers% %nConns%" > server.out
start /B %puttyPath%\plink -ssh ec2-user@%serverMachine2% -i %keyFile% -t "java -jar ASL_Server.jar 1313 %dbMachine%:5432 postgres qwer1 %nWorkers% %nConns%" > server2.out
timeout /T 2 /nobreak > NUL

start /B %puttyPath%\plink -ssh ec2-user@%clientMachine% -i %keyFile% -t "java -jar ASL_Client.jar %serverMachine% 1313 %nClients% %msgSize% %waitTime% %runTime%" > client.out
start /B %puttyPath%\plink -ssh ec2-user@%clientMachine2% -i %keyFile% -t "java -jar ASL_Client.jar %serverMachine2% 1313 %nClients% %msgSize% %waitTime% %runTime%" > client2.out
start /B %puttyPath%\plink -ssh ec2-user@%clientMachine3% -i %keyFile% -t "java -jar ASL_Client.jar %serverMachine% 1313 %nClients% %msgSize% %waitTime% %runTime%" > client3.out
start /B %puttyPath%\plink -ssh ec2-user@%clientMachine4% -i %keyFile% -t "java -jar ASL_Client.jar %serverMachine2% 1313 %nClients% %msgSize% %waitTime% %runTime%" > client4.out

set /A wait=%runTime%+5
timeout /T %wait% 

%puttyPath%\plink -ssh ec2-user@%serverMachine% -i %keyFile% -t "killall java"
%puttyPath%\plink -ssh ec2-user@%serverMachine2% -i %keyFile% -t "killall java"
%puttyPath%\plink -ssh ec2-user@%dbMachine% -i %keyFile% -t "sudo service postgresql stop"

%puttyPath%\plink -ssh ec2-user@%serverMachine% -i %keyFile% -t "mv trace.log s1_%nWorkers%_%nConns%.log"
%puttyPath%\plink -ssh ec2-user@%serverMachine2% -i %keyFile% -t "mv trace.log s2_%nWorkers%_%nConns%.log"
%puttyPath%\plink -ssh ec2-user@%clientMachine% -i %keyFile% -t "mv trace.log c1_%nClients%_%msgSize%_%waitTime%.log"
%puttyPath%\plink -ssh ec2-user@%clientMachine2% -i %keyFile% -t "mv trace.log c2_%nClients%_%msgSize%_%waitTime%.log"
%puttyPath%\plink -ssh ec2-user@%clientMachine3% -i %keyFile% -t "mv trace.log c3_%nClients%_%msgSize%_%waitTime%.log"
%puttyPath%\plink -ssh ec2-user@%clientMachine4% -i %keyFile% -t "mv trace.log c4_%nClients%_%msgSize%_%waitTime%.log"

