# Hide My Applist
[![Stars](https://img.shields.io/github/stars/Dr-TSNG/Hide-My-Applist?label=Stars)](https://github.com/Dr-TSNG)
[![Release](https://img.shields.io/github/v/release/Dr-TSNG/Hide-My-Applist?label=Release)](https://github.com/Dr-TSNG/Hide-My-Applist/releases/latest)
[![Download](https://img.shields.io/github/downloads/Dr-TSNG/Hide-My-Applist/total)](https://github.com/Dr-TSNG/Hide-My-Applist/releases/latest)
[![Channel](https://img.shields.io/badge/Telegram-Channel-blue.svg?logo=telegram)](https://t.me/HideMyApplist)
[![License](https://img.shields.io/github/license/Dr-TSNG/Hide-My-Applist?label=License)](https://choosealicense.com/licenses/gpl-3.0/)
## About this module  
Although It is incorrect to detect specific app installation, yet not every app using root provides random packagename support.  
In this case, detected apps that use root (such as Fake Location and Storage Isolation) is equal to detected root itself.  
At the same time, some "smart" apps use various loopholes to acquire your applist, so that it can draw a persona for you.  
This module provides some methods to test whether you have already hidden your applist nicely.  
Also, it can work as an Xposed module to hide some apps or reject applist requests to protect your privacy.   
## Compilation
This project uses Hidden API, so you should replace android.jar in Android Studio. [Reference](https://github.com/anggrayudi/android-hidden-api)  

This project uses [genuine](https://github.com/brevent/genuine) to add some signature verification hidden tricks to prevent it from being modified by MT script kiddies for profit. For those who want to compile the code, please generate you own genuine.h to prevent tricks. Also, self-signed apk cannot load the riru extension, this should be emphasized.
## Update Log / 更新日志
[Reference to the release page / 参考发布页面](https://github.com/Dr-TSNG/Hide-My-Applist/releases)  

## 关于该模块  
虽然“检测安装的应用是不正确的做法”，而且很蠢，但是并不是所有的插件类应用都提供了随机包名支持。这就意味着检测到安装了 root 类应用（如 Fake Location、存储重定向）与检测到了 root 本身区别不大。（会使用检测手段的 app 可不会认为你是在“我就蹭蹭不进去”）  
与此同时，部分“不安分”的 app 会使用各种漏洞绕过系统权限来获取你的应用列表，从而对你建立用户画像（如陈叔叔将安装了 V2Ray 的用户分为一类），或是类似于某某校园某某乐跑的软件会要求你卸载作弊软件。  
该模块提供了一些检测方式用于测试您是否成功地隐藏了某些特定的包名，如 Magisk/Edxposed Manager；同时可作为 Xposed 模块用于隐藏应用列表或特定应用，保护隐私。  
