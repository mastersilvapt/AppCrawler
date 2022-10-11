#!/bin/bash

project=$(pwd)
# apkTestNew=$project/app/build/intermediates/apk/androidTest/debug/app-debug-androidTest.apk
apkTestNew=$project/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
apkNew=$project/app/build/outputs/apk/debug/app-debug.apk

apkTestOld=$project/bin/com.eaway.appcrawler.test.apk
apkOld=$project/bin/com.eaway.appcrawler.apk

shaTestNew=$(sha256sum $apkTestNew | awk -F ' ' '{print $1}')
shaNew=$(sha256sum $apkNew | awk -F ' ' '{print $1}')

[[ -f $apkTestOld ]] && shaTestOld=$(sha256sum $apkTestOld | awk -F ' ' '{print $1}')
[[ -f $apkOld ]] && shaOld=$(sha256sum $apkOld | awk -F ' ' '{print $1}')

if [ -f $apkTestOld ] && [ $shaTestNew = $shaTestOld ]; then
	echo "Same Test no need for copy"
else 
	cp $apkTestNew $apkTestOld
	echo "Copy Test app with success"
fi

if [ -f $apkOld ] && [ $shaNew = $shaOld ]; then
	echo "Same app file no need for copy"
else 
	cp $apkNew $apkOld
	echo "Copy app with success"
fi

((adb shell pm list packages | grep "com.eaway.appcrawler.test" > /dev/null) && adb uninstall com.eaway.appcrawler.test) || echo "package not installed"
((adb shell pm list packages | grep "com.eaway.appcrawler" > /dev/null) && adb uninstall com.eaway.appcrawler) || echo "package not installed"

adb install $apkTestOld
adb install $apkOld
