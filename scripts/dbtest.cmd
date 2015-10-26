@echo off

set db=%1
set s1=%2
set s2=%3

set ASLPath=E:\Users\Marcel\Documents\ETH\ASL
set puttyPath=%ASLPath%\putty
set keyFile=%puttyPath%\keys\ASL_key.ppk
set experimentID=dbBenchmark
echo y | %puttyPath%\plink -ssh ec2-user@%db% -i %keyFile% "echo db: ok"
echo y | %puttyPath%\plink -ssh ec2-user@%s1% -i %keyFile% "echo s1: ok"
echo y | %puttyPath%\plink -ssh ec2-user@%s2% -i %keyFile% "echo s2: ok"

echo starting database
%puttyPath%\plink -ssh ec2-user@%db% -i %keyFile% -t "sudo service postgresql start"
REM message size
for %%M in (200,2000) do (
	REM db connections
	for %%N in (1,2,4,8,12,16) do (
		call dbtestexp.cmd %db% %s1% %s2% /N %%N /M %%M /T 600 /ID %experimentID%
	)
)

echo stopping database
%puttyPath%\plink -ssh ec2-user@%db% -i %keyFile% -t "sudo service postgresql stop"

echo copying files
set scpPath=%ASLPath%\winscp

mkdir %ASLPath%\logs\%experimentID%

%scpPath%\winscp.com /script=getLog.txt /parameter %s1%
%scpPath%\winscp.com /script=getLog.txt /parameter %s2%