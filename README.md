# TodayDock-Android
[![Play Store](.github/playstore-badge.svg)](https://play.google.com/store/apps/details?id=systems.sieber.fsclock)
[![App Gallery](.github/appgallery-badge.svg)](https://appgallery.cloud.huawei.com/ag/n/app/C104084493?channelId=github&id=fee3a2847b6941c1ab22fc546213b987&s=955678FB43D4F883623B96C10E5A4EA80812A8DBFEC073C89BC02EF15B521D76&detailType=0&v=)
[![Amazon Appstore](.github/amazon-appstore-badge.svg)](https://www.amazon.com/gp/product/B09BK3HQJ9)
[![APK Download](.github/apk-badge.svg)](https://github.com/schorschii/FsClock-Android/releases)

Simple Fullscreen Clock Screensaver
- analog and digital clock can be activated or deactivated individually
- optional date and weekday view
- displays appointments from Android calendar or internal event database
- adjustable background and clock color
- can be set as Android system screensaver ("DayDream") on smartphones and tablets
- compatible with Android TV devices

## Screenshots
![Screenshot](.github/screenshot.png)

## Screensaver on Amazon FireOS / FireTV
Amazon FireOS (on FireTV devices) does currently not officially allow changing the system screensaver to another app, but there is a workaround possible using the Android developer tools. Without this workaround, the clock can still be started on FireTV devices like a normal app.

1. Enable Debugging on your FireTV.  
   All details (including how to install ADB on your computer) are described [here](https://developer.amazon.com/docs/fire-tv/connecting-adb-to-device.html).

2. Execute the following commands
   ```
   # set this app as screensaver
   # please note: the FireTV settings app will still show the Amazon screensaver, but the Android system will now start the clock instead
   adb shell settings put secure screensaver_components systems.sieber.fsclock/systems.sieber.fsclock.FullscreenDream

   # if you want to restore the Amazon default screen saver, execute the following command
   adb shell settings put secure screensaver_components com.amazon.bueller.photos/.daydream.ScreenSaverService
   ```
