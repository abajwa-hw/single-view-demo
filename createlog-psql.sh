#!/bin/bash
input=$1
maxitems=$2
websites=("SAMSUNG GALAXY NOTE 5"  "iPad Pro" "Samsung Galaxy S6" "Apple iPhone 6S" "Microsoft Surface Pro 4" "Vizio M series (2015)" "Apple TV (2015)" "Google Nexus 6P" "LG V10 (unlocked)" "iPhone 6S Plus" "Samsung Galaxy S6 Edge" "BlackBerry Priv" "Vizio E series (2015)" "Microsoft Surface Book" "Google Nexus 5X" "Samsung Galaxy S5" "Motorola Droid Turbo 2" "Apple TV" "Fitbit Charge HR" "Apple Watch" "Ninja Coffee Bar" "Apple iPhone 6" "Roku 3 (2015)" "Samsung Galaxy Tab S2" "Samsung Galaxy Note 4" "Apple iPad Mini 4" "Samsung Gear S2" "Apple iPhone 6 Plus" "Roku 2 (2015)" "iPad Air 2" "Bose QuietComfort 25" "LG G4" "Samsung Galaxy S4" "Samsung UNJS8500 series" "TCL S3800 series (Roku TV, 2015)" "Samsung Galaxy S6 Edge+" "SimpliSafe Home Security" "Samsung Galaxy Grand Prime" "Asus Chromebit" "LG EF9500 series" "Roku 4" "Audio-Technica ATH-M50x" "Beats Studio Wireless Headphones" "LG G3" "Bose SoundLink Mini II" "iPad Mini 2" "Beats Powerbeats2 Wireless" "Nikon D3300" "Amazon Echo" "UE Boom 2" "Nest Cam" "Microsoft Xbox One" "Asus AC2400 RT-AC87U Dual-band Wireless Gigabit Router" "Amazon Kindle Paperwhite (2015)" "Yamaha YAS-203" "Google Nexus 6" "HTC One M9" "Apple MacBook Pro with Retina Display (13-inch, 2015)" "Apple MacBook (12-inch, 2015)" "Nest Learning Thermostat Third Generation" "iPad Air 2" "Lenovo Yoga 900" "GoPro Hero3" "Nest Learning Thermostat" "LG G Stylo" "Samsung UNJU7100 series" "Samsung Gear VR (2015)" "Dyson v6" "Samsung Galaxy S6 Active" "TiVo Bolt" "Bose SoundLink Around-Ear Wireless Headphones II" "Samsung Galaxy Tab A 8.0" "Asus RT-AC68U Dual-band Wireless-AC1900 Gigabit Router" "Apple iPhone 5S" "TCL FS4610R (Roku TV)" "Google Nexus 9" "Chromecast Audio" "Sony Alpha 6000 (ILCE-6000)" "JBL Flip 3" "Lenovo Yoga 3 14" "Jawbone Up2" "Apple CarPlay" "GoPro Hero4 Silver" "HP Spectre x360" "Sony Xperia Z5 Premium" "Netgear Arlo Smart Home Security" "Apple MacBook Air (13-inch, 2015)" "Bose SoundLink Bluetooth Speaker III" "Samsung Galaxy Core Prime" "Microsoft Lumia 950" "Sennheiser HD 598" "Beats Solo 2" "Vizio SB4051-C0" "Microsoft Surface Pro 3" "HP Stream 11" "Sony Xperia Z5" "JBL Charge 2+" "Sennheiser Momentum 2.0" "Samsung SSD 850 Evo" "Apple iPhone 5")


if test -z "$2"
then
	maxitems=0
fi

# Seed random generator
RANDOM=$$$(date +%s)
count=0

#for each user....
cat $input | while read line
do
	count=$((count+1))
	
	#ignore header row
	if [ "$count" -gt 1 ]
	then
	
		# get userid from first field of csv
	    userid=`echo $line | cut -d, -f1`                          

		#pick number of browsing sessions for user
		numsessions=$(shuf -i 3-10 -n 1)

		# for each session
		for (( sessionid=1; sessionid<=$numsessions; sessionid++ ))
		do
			#pick random website form above list
			randomwebsite=${websites[$RANDOM % ${#websites[@]} ]}   

			#for first session pick random time between years 2007-2009 (this is ranger of our sales data)
			if [[ $sessionid -eq 1 ]]; then							
				unixtime=$(shuf -i 1167609600-1262217600 -n 1)
				
			#otherwise pick time spent on previous product (between 3 and 230s) and add to previous sessions start time
			else
				deltatime=$(shuf -i 3-230 -n 1)
				#echo "$unixtime $deltatime"
				unixtime=$(( $unixtime+$deltatime ))
			fi
			timestamp=$(date -d @$unixtime +'%Y-%m-%d %H:%M:%S')
		
			echo "$sessionid-$userid,$userid,$timestamp,$randomwebsite"

			if [[ "$maxitems" -gt 0 ]]; then
			        #sleep for random time between 0-3s		        
		        	sleep $(shuf -i 0-3 -n 1)s
			fi
		done
		
		if [[ "$count" -gt $maxitems ]]; then
			#echo "Maximum items $maxitems reached. Exitting..."
			exit 0
		fi
		
	fi
done
