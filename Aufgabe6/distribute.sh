#!/bin/bash

function print_usage() {
    echo "Usage: $0 <simple/fancy> [<path_to_class_files>] [<path_to_config_files (i.e., stack.xml, my_hosts, logging.cfg)>]"
}

if [ -z $1 ]
then
    print_usage
    exit 1
fi

if [ -z $2 ]
then
    program_path="$PWD"
else
    program_path="$2"
fi
if [ -z $3 ]
then
    path_to_configs="$program_path"
else
    path_to_configs="$3"
fi
java_cmd="java -cp ${program_path}:/proj/i4vs/pub/aufgabe5/jgroups-3.6.8.Final.jar"
start_class="vsue.distlock.VSLockTestCases"
log_name="distlock"
base_port=$(( 14000 + (UID * 337 % 977) * 13 ))

######################################################
##########                                 ###########
########## NO USER SERVICEABLE PARTS BELOW ###########
##########                                 ###########
######################################################

if [ ! -f "${path_to_configs}/my_hosts" -o ! -f "${path_to_configs}/stack.xml" ]; then
	echo "ERROR: my_hosts or stack.xml not found."
	echo "You need to copy those files to your current working directory."
	print_usage
	exit 1
fi

classfile=`printf "%s" "${program_path}/$start_class" | sed -e "s?\\.?/?g"`.class
if [ ! -f "$classfile" ]; then
	echo "Please make sure the class file '$classfile' exists."
	print_usage
	exit 1
fi

total_hosts=0
host_count=0
group_list=""
hosts=()

while read -r next_host; do
	test -z "$next_host" && continue;
	
	group_list="$group_list,$next_host[$((base_port+total_hosts))]"

	total_hosts=$((total_hosts+1))
	hosts+=($next_host)
done <${path_to_configs}/my_hosts

if [ "$total_hosts" -le 1 -o "$total_hosts" -gt 10 ]; then
	echo "Found $total_hosts host entries, but must be between 2 and 10."
	exit 1;
fi

cat >__screenrc__ <<EOF
startup_message off
zombie kc
logfile ${log_name}-log.%n
hardstatus alwayslastline "%-Lw%{= bW}%50>%n%f* %t%{-}%+Lw%<"
EOF

for next_host in "${hosts[@]}"
do
    printf "%s\n" \
    "screen -X screen -S vs ssh -t $next_host '/bin/bash -c \"\
cd '$program_path'; \
$java_cmd \
-Djava.util.logging.config.file=${path_to_configs}/logging.cfg \
-Djg.bind_port=$((base_port+host_count)) \
-Djg.initial_hosts=${group_list:1} \
-Dconfigs_path=${path_to_configs} \
-Dbind.address=$next_host \
$start_class $total_hosts $1\"'" >>__screenrc__
    host_count=$((host_count+1))
done

rm -f "${log_name}-log."{0..9}
screen -S locktest -c __screenrc__
rm -f __screenrc__

