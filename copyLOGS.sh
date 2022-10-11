#!/bin/bash

function getFolder () {
	file=$1/$2
	for (( i=1;; i++ )) do
		[ -d $file ] || break;
		file=$1/$2$i;
	done
	echo $file;
}


logs=$(pwd)/logs

result=$(getFolder "$logs"  "Tests");
external=$(adb shell "echo \$EXTERNAL_STORAGE")
appCrawler=$external/AppCrawler

adb pull "$appCrawler" "$result"
adb shell rm -rf "$appCrawler/*"

# list=$(adb shell ls "$appCrawler");
# for f in $list; do
#	adb pull "$appCrawler/$f" "$result";
#	adb shell rm -rf "$appCrawler/$f"
# done

