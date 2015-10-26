@echo off
REM defaults

set nConns=4
set msgSize=200
set runTime=600

set dbMachine=%1

set serverMachine1=%2

set serverMachine2=%3

shift
shift
shift

set CUSTOM_ID=
set START_DB=
set CHECK_MACHINES=
set COPY_FILES=

:parse
if "%1"=="" goto endparse
if "%1"=="/N" (
	set nConns=%2
	shift
)
if "%1"=="/M" (
	set msgSize=%2
	shift
)
if "%1"=="/T" (
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

set /A totConns = %nConns%*2

if not defined CUSTOM_ID (
	set experimentID=%db_benchmark%
)

echo db-conns: %totConns%
echo msg: %msgSize%B
echo runtime: %runTime%s

set ASLPath=E:\Users\Marcel\Documents\ETH\ASL
set puttyPath=%ASLPath%\putty
set keyFile=%puttyPath%\keys\ASL_key.ppk

if defined CHECK_MACHINES (
	echo y | %puttyPath%\plink -ssh ec2-user@%dbMachine% -i %keyFile% "echo db: ok"
	echo y | %puttyPath%\plink -ssh ec2-user@%serverMachine1% -i %keyFile% "echo s1: ok"
	echo y | %puttyPath%\plink -ssh ec2-user@%serverMachine2% -i %keyFile% "echo s2: ok"
)

if defined START_DB (
	%puttyPath%\plink -ssh ec2-user@%dbMachine% -i %keyFile% -t "sudo service postgresql start"
)

echo starting server
start /B %puttyPath%\plink -ssh ec2-user@%serverMachine1% -i %keyFile% -t "java -jar ASL_DbTestServer.jar %dbMachine%:5432 postgres qwer1 %nConns% %msgSize% %runTime%" > server1.out
start /B %puttyPath%\plink -ssh ec2-user@%serverMachine2% -i %keyFile% -t "java -jar ASL_DbTestServer.jar %dbMachine%:5432 postgres qwer1 %nConns% %msgSize% %runTime%" > server2.out

set /A wait=%runTime%+5
timeout /T %wait% 

echo shutting down server
%puttyPath%\plink -ssh ec2-user@%serverMachine1% -i %keyFile% -t "killall java"
%puttyPath%\plink -ssh ec2-user@%serverMachine2% -i %keyFile% -t "killall java"

%puttyPath%\plink -ssh ec2-user@%dbMachine% -i %keyFile% -t "psql -d ASL -h localhost -p 5432 -U postgres -c ""delete from messages; delete from queues; delete from users;"""
if defined START_DB (
	%puttyPath%\plink -ssh ec2-user@%dbMachine% -i %keyFile% -t "sudo service postgresql stop"
)

%puttyPath%\plink -ssh ec2-user@%serverMachine1% -i %keyFile% -t "mv trace.log s1_%totConns%_%msgSize%.log"
%puttyPath%\plink -ssh ec2-user@%serverMachine2% -i %keyFile% -t "mv trace.log s2_%totConns%_%msgSize%.log"

set scpPath=%ASLPath%\winscp
if defined COPY_FILES (
	echo copying files

	mkdir %ASLPath%\logs\%experimentID%

	%scpPath%\WinSCP.com /script=getLog.txt /parameter %serverMachine1%

	%scpPath%\WinSCP.com /script=getLog.txt /parameter %serverMachine2%
)