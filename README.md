# Hide My Applist

## This project is suspended 此项目已停更

[![Stars](https://img.shields.io/github/stars/Dr-TSNG/Hide-My-Applist?label=Stars)](https://github.com/Dr-TSNG)
[![Release](https://img.shields.io/github/v/release/Dr-TSNG/Hide-My-Applist?label=Release)](https://github.com/Dr-TSNG/Hide-My-Applist/releases/latest)
[![Download](https://img.shields.io/github/downloads/Dr-TSNG/Hide-My-Applist/total)](https://github.com/Dr-TSNG/Hide-My-Applist/releases/latest)
[![Channel](https://img.shields.io/badge/Telegram-Channel-blue.svg?logo=telegram)](https://t.me/HideMyApplist)
[![License](https://img.shields.io/github/license/Dr-TSNG/Hide-My-Applist?label=License)](https://choosealicense.com/licenses/gpl-3.0/)

![banner](banner.png)

## About this module

Although it's bad practice to detect the installation of specific apps, not every app using root provides random package name support. In this case, if apps that use root (such as Fake Location and Storage Isolation) are detected, it is tantamount to detecting that the device is rooted.

Additionally, some apps use various loopholes to acquire your app list, in order to use it as fingerprinting data or for other nefarious purposes.

This module can work as an Xposed module to hide apps or reject app list requests, and provides some methods to test whether you have hidden your app list properly.

## Update Log / 更新日志
[Reference to the release page / 参考发布页面](https://github.com/Dr-TSNG/Hide-My-Applist/releases)  

## 关于该模块  
虽然“检测安装的应用是不正确的做法”，而且很蠢，但是并不是所有的插件类应用都提供了随机包名支持。这就意味着检测到安装了 root 类应用（如 Fake Location、存储重定向）与检测到了 root 本身区别不大。（会使用检测手段的 app 可不会认为你是在“我就蹭蹭不进去”）  
与此同时，部分“不安分”的 app 会使用各种漏洞绕过系统权限来获取你的应用列表，从而对你建立用户画像（如陈叔叔将安装了 V2Ray 的用户分为一类），或是类似于某某校园某某乐跑的软件会要求你卸载作弊软件。  
该模块提供了一些检测方式用于测试您是否成功地隐藏了某些特定的包名，如 Magisk/Edxposed Manager；同时可作为 Xposed 模块用于隐藏应用列表或特定应用，保护隐私。  

## Document
### Maps scan rules
Maps refers to /proc/self/maps (See [Linux manpage](https://man7.org/linux/man-pages/man5/proc.5.html) for more information).  
When something such as an Xposed module or a Zygisk module injects into target app, it will show its path on /proc/\<pid\>/maps. Though LSPosed and Riru did some work to make module maps info anonymous, if a module dlopen a native library by itself, the loaded so path will still be written on maps (Such module like QNotified).  

How to use it: paths that contains configured strings will be filtered on /proc/self/maps  
Notice that under **MOST** circumstances you do not need to switch on this interception nor need to add any rule.
### Custom query params
This refers to the string params of methods of [PackageManagerService](https://cs.android.com/android/platform/superproject/+/master:frameworks/base/services/core/java/com/android/server/pm/PackageManagerService.java)  

How to use it: pms methods whose string params contain configured strings will be intercepted  
Notice that under **MOST** circumstances you do not need to switch on this interception nor need to add any rule.  
### How did HMA Magisk module work?
HMA install inline hooks for syscalls and replace the path to dummy to make the app think there "isn't" suspicious files or directories.

However, syscall hook is very **unstable**, and can be detected by some methods. So do not switch on *file detection / maps scan* interceptions if not needed.
