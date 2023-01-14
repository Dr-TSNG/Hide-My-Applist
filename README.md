# Hide My Applist

[![Stars](https://img.shields.io/github/stars/Dr-TSNG/Hide-My-Applist?label=Stars)](https://github.com/Dr-TSNG)
[![Crowdin](https://badges.crowdin.net/hide-my-applist/localized.svg)](https://crowdin.com/project/hide-my-applist)
[![Build](https://img.shields.io/github/actions/workflow/status/Dr-TSNG/Hide-My-Applist/main.yml?branch=master&logo=github)](https://github.com/Dr-TSNG/Hide-My-Applist/actions)
[![Release](https://img.shields.io/github/v/release/Dr-TSNG/Hide-My-Applist?label=Release)](https://github.com/Dr-TSNG/Hide-My-Applist/releases/latest)
[![Download](https://img.shields.io/github/downloads/Dr-TSNG/Hide-My-Applist/total)](https://github.com/Dr-TSNG/Hide-My-Applist/releases/latest)
[![Channel](https://img.shields.io/badge/Telegram-Channel-blue.svg?logo=telegram)](https://t.me/HideMyApplist)
[![License](https://img.shields.io/github/license/Dr-TSNG/Hide-My-Applist?label=License)](https://choosealicense.com/licenses/gpl-3.0/)

![banner](banner.png)

- English  
- [中文（简体）](README_zh_CN.md)

## About this module

Although it's bad practice to detect the installation of specific apps, not every app using root provides random package name support. In this case, if apps related to root (such as Fake Location and Storage Isolation) are detected, it is tantamount to detecting that the device is rooted.

Additionally, some apps use various loopholes to acquire your app list, in order to use it as fingerprinting data or for other nefarious purposes.

This module can work as an Xposed module to hide apps or reject app list requests, and provides some methods to test whether you have hidden your app list properly.

## Translation Contributing
You can contribute translation [here](https://crowdin.com/project/hide-my-applist).

## Update Log
[Reference to the release page](https://github.com/Dr-TSNG/Hide-My-Applist/releases)  
