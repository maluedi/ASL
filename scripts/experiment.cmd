@echo off
REM defaults

set nWorkers=16
set nConns=16
set nClients=8
set msgSize=200
set waitTime=10
set runTime=300



set dbMachine=%1

set serverMachine=%2
set serverMachine2=%3

set clientMachine=%4
set clientMachine2=%5
set clientMachine3=%6
set clientMachine4=%7

shift
shift
shift
shift
shift
shift
shift

set CUSTOM_ID=
set START_DB=
set CHECK_MACHINES=
set COPY_FILES=

:parse
if "%1"=="" goto endparse
if "%1"=="/W" (
	set nWorkers=%2
	shift
)
if "%1"=="/D" (
	set nConns=%2
	shift
)
if "%1"=="/C" (
	set nClients=%2
	shift
)
if "%1"=="/M" (
	set msgSize=%2
	shift
)
if "%1"=="/T" (
	set waitTime=%2
	shift
)
if "%1"=="/R" (
	set runTime=%2
	shift
)
if "%1"=="/ID" (
	set experimentID=%2
	set CUSTOM_ID=true
	shift
)
if "%1"=="/DB" (
	set START_DB=true
)
if "%1"=="/check" (
	set CHECK_MACHINES=true
)
if "%1"=="/cp" (
	set COPY_FILES=true
)
shift
goto parse
:endparse

set /A totClients=4*%nClients%

if not defined CUSTOM_ID (
	set experimentID=%nWorkers%_%nConns%_%totClients%_%msgSize%_%waitTime%
)

echo workers: %nWorkers%
echo db-conns: %nConns%
echo clients: %totClients%
echo msg: %msgSize%B
echo wait: %waitTime%ms
echo runtime: %runTime%s

set ASLPath=E:\Users\Marcel\Documents\ETH\ASL
set puttyPath=%ASLPath%\putty
set keyFile=%puttyPath%\keys\ASL_key.ppk

if defined CHECK_MACHINES (
	echo y | %puttyPath%\plink -ssh ec2-user@%dbMachine% -i %keyFile% "echo db: ok"
	echo y | %puttyPath%\plink -ssh ec2-user@%serverMachine% -i %keyFile% "echo s1: ok"
	echo y | %puttyPath%\plink -ssh ec2-user@%serverMachine2% -i %keyFile% "echo s2: ok"
	echo y | %puttyPath%\plink -ssh ec2-user@%clientMachine% -i %keyFile% "echo c1: ok"
	echo y | %puttyPath%\plink -ssh ec2-user@%clientMachine2% -i %keyFile% "echo c2: ok"
	echo y | %puttyPath%\plink -ssh ec2-user@%clientMachine3% -i %keyFile% "echo c3: ok"
	echo y | %puttyPath%\plink -ssh ec2-user@%clientMachine4% -i %keyFile% "echo c4: ok"
)

if defined START_DB (
	%puttyPath%\plink -ssh ec2-user@%dbMachine% -i %keyFile% -t "sudo service postgresql start"
)

echo starting servers
start /B %puttyPath%\plink -ssh ec2-user@%serverMachine% -i %keyFile% -t "java -jar ASL_Server.jar 1313 %dbMachine%:5432 postgres qwer1 %nConns% %nWorkers%" > server.out
start /B %puttyPath%\plink -ssh ec2-user@%serverMachine2% -i %keyFile% -t "java -jar ASL_Server.jar 1313 %dbMachine%:5432 postgres qwer1 %nConns% %nWorkers%" > server2.out
timeout /T 2 /nobreak > NUL

echo starting clients
start /B %puttyPath%\plink -ssh ec2-user@%clientMachine% -i %keyFile% -t "java -jar ASL_Client.jar %serverMachine% 1313 %nClients% %msgSize% %waitTime% %runTime%" > client.out
start /B %puttyPath%\plink -ssh ec2-user@%clientMachine2% -i %keyFile% -t "java -jar ASL_Client.jar %serverMachine2% 1313 %nClients% %msgSize% %waitTime% %runTime%" > client2.out
start /B %puttyPath%\plink -ssh ec2-user@%clientMachine3% -i %keyFile% -t "java -jar ASL_Client.jar %serverMachine% 1313 %nClients% %msgSize% %waitTime% %runTime%" > client3.out
start /B %puttyPath%\plink -ssh ec2-user@%clientMachine4% -i %keyFile% -t "java -jar ASL_Client.jar %serverMachine2% 1313 %nClients% %msgSize% %waitTime% %runTime%" > client4.out

set /A wait=%runTime%+5
timeout /T %wait%  /nobreak

echo shutting down servers
%puttyPath%\plink -ssh ec2-user@%serverMachine% -i %keyFile% -t "killall java"
%puttyPath%\plink -ssh ec2-user@%serverMachine2% -i %keyFile% -t "killall java"

%puttyPath%\plink -ssh ec2-user@%dbMachine% -i %keyFile% -t "psql -d ASL -h localhost -p 5432 -U postgres -c ""delete from messages; delete from queues; delete from users;"""
if defined START_DB (
	%puttyPath%\plink -ssh ec2-user@%dbMachine% -i %keyFile% -t "sudo service postgresql stop"
)

%puttyPath%\plink -ssh ec2-user@%serverMachine% -i %keyFile% -t "mv trace.log s1_%nWorkers%_%nConns%_%totClients%_%msgSize%_%waitTime%.log"
%puttyPath%\plink -ssh ec2-user@%serverMachine2% -i %keyFile% -t "mv trace.log s2_%nWorkers%_%nConns%_%totClients%_%msgSize%_%waitTime%.log"
%puttyPath%\plink -ssh ec2-user@%clientMachine% -i %keyFile% -t "mv trace.log c1_%nWorkers%_%nConns%_%totClients%_%msgSize%_%waitTime%.log"
%puttyPath%\plink -ssh ec2-user@%clientMachine2% -i %keyFile% -t "mv trace.log c2_%nWorkers%_%nConns%_%totClients%_%msgSize%_%waitTime%.log"
%puttyPath%\plink -ssh ec2-user@%clientMachine3% -i %keyFile% -t "mv trace.log c3_%nWorkers%_%nConns%_%totClients%_%msgSize%_%waitTime%.log"
%puttyPath%\plink -ssh ec2-user@%clientMachine4% -i %keyFile% -t "mv trace.log c4_%nWorkers%_%nConns%_%totClients%_%msgSize%_%waitTime%.log"

set scpPath=%ASLPath%\winscp
if defined COPY_FILES (
	echo copying files

	mkdir %ASLPath%\logs\%experimentID%

	%scpPath%\winscp.com /script=getLog.txt /parameter %serverMachine%
	%scpPath%\winscp.com /script=getLog.txt /parameter %serverMachine2%

	%scpPath%\winscp.com /script=getLog.txt /parameter %clientMachine%
	%scpPath%\winscp.com /script=getLog.txt /parameter %clientMachine2%
	%scpPath%\winscp.com /script=getLog.txt /parameter %clientMachine3%
	%scpPath%\winscp.com /script=getLog.txt /parameter %clientMachine4%
)
