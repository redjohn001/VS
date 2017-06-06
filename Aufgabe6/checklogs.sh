#!/bin/bash

if [ -z "$1" ]
then
    dir=$PWD
else
    dir=$1
fi

function check_log_number() {
    nlogs=`ls $1 2> /dev/null | wc -l` 

    if [ $nlogs -lt 2 ]
    then
	echo -e "\tFAIL \t(-> Found only $nlogs logs file(s).)"
    fi
}

echo "Checking logs..."

#######################################################
PATTERN_SIMPLE_LOGS='vslocktest.simple.*.log'
printf "  * Simple: "

check_log_number "$dir/$PATTERN_SIMPLE_LOGS"

done=`grep -h "SIMPLE: DONE." $dir/$PATTERN_SIMPLE_LOGS 2>/dev/null | wc -l`

if [ $done -lt 1 ]
then
    echo -e "\tFAIL \t(-> No instance seems to have reached the end. Deadlock?)"
else
    echo -e "\tSUCCESS"
fi

#######################################################
PATTERN_FANCY_LOGS='vslocktest.fancy.*.log'
printf "  * Fancy: "

check_log_number "$dir/$PATTERN_FANCY_LOGS"

done=`grep -h "FANCY: DONE." $dir/$PATTERN_FANCY_LOGS 2>/dev/null | wc -l`

if [ $done -lt 1 ]
then
    echo -e "\tFAIL \t(-> No instace seems to have reached the end. Deadlock?)"
    exit 1
fi

num_sums=`grep -h "Sum is" $dir/$PATTERN_FANCY_LOGS 2>/dev/null | uniq | wc -l`

if [ $num_sums -lt 1 ]
then
    echo -e "\tFAIL \t(-> No sum line found.)"
elif [ $num_sums -gt 1 ]
then
    echo -e "\tFAIL \t(-> Sums differ.)"
else
    echo -e "\tSUCCESS"
fi
