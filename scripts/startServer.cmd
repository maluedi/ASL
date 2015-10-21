start /B %puttyPath%\plink -ssh ec2-user@%serverMachine% -i %keyFile% -t "java -jar ASL_Server.jar 1313 %dbMachine%:5432 postgres qwer1 %nWorkers% %nConns%" > server.out
