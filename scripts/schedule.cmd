@echo off

set db=%1
set s1=%2
set s2=%3
set c1=%4
set c2=%5
set c3=%6
set c4=%7

set ASLPath=E:\Users\Marcel\Documents\ETH\ASL
set puttyPath=%ASLPath%\putty
set keyFile=%puttyPath%\keys\ASL_key.ppk
set experimentID=2kfactorialretakes
echo y | %puttyPath%\plink -ssh ec2-user@%db% -i %keyFile% "echo db: ok"
echo y | %puttyPath%\plink -ssh ec2-user@%s1% -i %keyFile% "echo s1: ok"
echo y | %puttyPath%\plink -ssh ec2-user@%s2% -i %keyFile% "echo s2: ok"
echo y | %puttyPath%\plink -ssh ec2-user@%c1% -i %keyFile% "echo c1: ok"
echo y | %puttyPath%\plink -ssh ec2-user@%c2% -i %keyFile% "echo c2: ok"
echo y | %puttyPath%\plink -ssh ec2-user@%c3% -i %keyFile% "echo c3: ok"
echo y | %puttyPath%\plink -ssh ec2-user@%c4% -i %keyFile% "echo c4: ok"

echo starting database
%puttyPath%\plink -ssh ec2-user@%db% -i %keyFile% -t "sudo service postgresql start"

call experiment.cmd %db% %s1% %s2% %c1% %c2% %c3% %c4% /W 4 /D 4 /C 4 /M 2000 /T 500 /R 600 /ID %experimentID%
timeout /T 300
call experiment.cmd %db% %s1% %s2% %c1% %c2% %c3% %c4% /W 4 /D 4 /C 64 /M 200 /T 10 /R 600 /ID %experimentID%
timeout /T 300
call experiment.cmd %db% %s1% %s2% %c1% %c2% %c3% %c4% /W 16 /D 4 /C 4 /M 200 /T 500 /R 600 /ID %experimentID%
timeout /T 300
call experiment.cmd %db% %s1% %s2% %c1% %c2% %c3% %c4% /W 16 /D 4 /C 64 /M 200 /T 10 /R 600 /ID %experimentID%
timeout /T 300
call experiment.cmd %db% %s1% %s2% %c1% %c2% %c3% %c4% /W 4 /D 16 /C 64 /M 2000 /T 10 /R 600 /ID %experimentID%
timeout /T 300
call experiment.cmd %db% %s1% %s2% %c1% %c2% %c3% %c4% /W 4 /D 16 /C 64 /M 2000 /T 500 /R 600 /ID %experimentID%
timeout /T 300
call experiment.cmd %db% %s1% %s2% %c1% %c2% %c3% %c4% /W 16 /D 16 /C 4 /M 200 /T 10 /R 600 /ID %experimentID%
timeout /T 300
call experiment.cmd %db% %s1% %s2% %c1% %c2% %c3% %c4% /W 16 /D 16 /C 4 /M 2000 /T 10 /R 600 /ID %experimentID%

echo stopping database
%puttyPath%\plink -ssh ec2-user@%db% -i %keyFile% -t "sudo service postgresql stop"

echo copying files
set scpPath=%ASLPath%\winscp

mkdir %ASLPath%\logs\%experimentID%

%scpPath%\winscp.com /script=getLog.txt /parameter %s1%
%scpPath%\winscp.com /script=getLog.txt /parameter %s2%

%scpPath%\winscp.com /script=getLog.txt /parameter %c1%
%scpPath%\winscp.com /script=getLog.txt /parameter %c2%
%scpPath%\winscp.com /script=getLog.txt /parameter %c3%
%scpPath%\winscp.com /script=getLog.txt /parameter %c4%