#!/bin/bash
input=$1
maxitems=$2
websites=("http://www.google.com" "http://www.hortonworks.com" "http://hadoop.apache.org" "http://www.yahoo.com" "http://cnn.com" "http://news.bbc.co.uk" "http://www.facebook.com")


if test -z "$2"
then
	maxitems=0
fi

# Seed random generator
RANDOM=$$$(date +%s)
count=0
cat $input | while read line
do
	count=$((count+1))
	# get first field from csv as id
        id=`echo $line | cut -d, -f1`                          
	#pick random website form above list
	randomwebsite=${websites[$RANDOM % ${#websites[@]} ]}         
	echo "$id,$randomwebsite"

	if [[ "$maxitems" -gt 0 ]]; then
	        #sleep for random time
        	sleep 1
	fi

	if [[ "$count" -eq $maxitems ]]; then
		#echo "Maximum items $maxitems reached. Exitting..."
		exit 0
	fi
done
