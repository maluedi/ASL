while [[ $# > 1 ]]
do
	key="$1"
	case $key in
		-h|--host)
		host="$2"
		shift
		;;
		-p|--port)
		port="$2"
		shift
		;;
done
		
for msgSize in "200" "2000"
do
	for thinkTime in "10" "100"
	do
		for nClients in "1" "2" "4" "8" "16" "32"
		do
			#run experiment for 5 minutes
			java -jar ASL_Client.jar $host $port $nClients $msgSize $thinkTime 300
			rename trace e_${msgSize}_${thinkTime}_${nclients} trace.log
		done
	done
done

java -jar ASL_Fill.jar $host $port 

for msgSize in "200" "2000"
do
	for thinkTime in "10" "100"
	do
		for nClients in "1" "2" "4" "8" "16" "32"
		do
			#run experiment for 5 minutes
			java -jar ASL_Client.jar $host $port $nClients $msgSize $thinkTime 300
			rename trace f_${msgSize}_${thinkTime}_${nclients} trace.log
		done
	done
done


echo done