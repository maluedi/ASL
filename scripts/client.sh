#!/bin/bash

while [ $# -gt 1 ]
do
	key="$1"
	case $key in
		-h|--host)
		HOST="$2"
		shift
		;;
		-p|--port)
		PORT="$2"
		shift
		;;
		-r|--runtime)
		RUNTIME="$2"
		shift
		;;
		*)
		;;
	esac
	shift
done

# HOST="$1"
# PORT="$2"
# RUNTIME="$3"

echo $HOST
echo $PORT
echo $RUNTIME

ms="200 2000"
tt="10 100"
nc="1 2 4 8 16 32"		
for msgSize in $ms
do
	for thinkTime in $tt
	do
		for nClients in $nc
		do
			# run experiment for 5 minutes
			echo $msgSize $thinkTime $nClients
			java -jar ASL_ReadWriteClient.jar $HOST $PORT $nClients $msgSize $thinkTime $RUNTIME
			mv trace.log e_${msgSize}_${thinkTime}_${nClients}.log
		done
	done
done

#java -jar ASL_Fill.jar $HOST $PORT 

#for msgSize in "200" "2000"
#do
	# for thinkTime in "10" "100"
	# do
		# for nClients in "1" "2" "4" "8" "16" "32"
		# do
			# run experiment for 5 minutes
			# java -jar ASL_Client.jar $HOST $PORT $nClients $msgSize $thinkTime $RUNTIME
			# rename trace f_${msgSize}_${thinkTime}_${nclients} trace.log
		# done
	# done
# done


echo done

exit 0