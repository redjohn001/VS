#!/bin/bash
filename="$2"
if [ $# -ne 3 ]
then
	echo "command <terminal> <host-file> <workspace>"
	exit -1 
fi
var=0

while read -r line
do
    name="$line"
    host=($(echo $name | sed s/:/\\n/g))
    host=${host[0]}
    $1 -e ssh $host "cd $3; java -cp bin:jgroups-3.6.8.Final.jar -Djava.util.logging.config.file=logging.cfg vsue.replica.VSKeyValueReplica $var replica.addresses" 2>/dev/null >/dev/null & 
    sleep 8
    var=$((var+1))
done < "$filename"
