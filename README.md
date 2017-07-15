# DetectingCP2102

## System Requirements
-------------------
1. Android Studio

## Motivation
-------------------
This project will detect any USB device connected to the phone. But for that, change the Product and Vendor ID. 
You can find the product and Vendor ID on your Device Manager (Windows)

## Changes to the original Code
--------------------

I removed the button functionlaity. Now the doReadRawDescriptors() function executes in the onCreate() method itself.
I also changed the text that is shown in the Toast.
Some other minor UI elements were made.

## Author:
-----------------------
http://android-er.blogspot.in/2014/02/search-usb-device-for-specified-vendor.html
