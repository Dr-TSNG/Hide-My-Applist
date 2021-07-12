# 指定压缩级别
-optimizationpasses 5
# 混淆时采用的算法
-optimizations !code/simplification/arithmetic, !field,!class/merging, !code/allocation/variable
# 优化时允许访问并修改有修饰符的类和类的成员
-allowaccessmodification
# 将文件来源重命名为“SourceFile”字符串
-renamesourcefileattribute SourceFile
# 保持异常不被混淆
-keepattributes Exceptions
# 保留行号
-keepattributes SourceFile, LineNumberTable
# 保留注解不混淆
-keepattributes *Annotation*,InnerClasses
# 保持泛型
-keepattributes Signature
# 保持反射
-keepattributes EnclosingMethod
# 保留 native 方法的类名和方法名
-keepclasseswithmembernames class * { native <methods>; }

#---------------------------------默认保留区---------------------------------
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference
-keep public class * extends android.view.View
-keep class android.support.** { *; }
#--------------------------------------------------------------------------

# Magic
-keep class com.tsng.hidemyapplist.Magic { *; }

# Views
-keep class com.tsng.hidemyapplist.app.ui.activities.ModuleActivity$Fragment { *; }
-keep class com.tsng.hidemyapplist.app.ui.views.MapsRulesView{ *; }

# Config
-keep class com.tsng.hidemyapplist.JsonConfig { *; }
-keep class com.tsng.hidemyapplist.JsonConfig$* { *; }

# Xposed
-keep class com.tsng.hidemyapplist.xposed.XposedEntry
-keep class com.tsng.hidemyapplist.xposed.PackageManagerService
-keepclassmembers class com.tsng.hidemyapplist.app.MyApplication {
    static final boolean isModuleActivated;
}

# Dontwarn
-dontwarn org.bouncycastle.jsse.BCSSLParameters
-dontwarn org.bouncycastle.jsse.BCSSLSocket
-dontwarn org.bouncycastle.jsse.provider.BouncyCastleJsseProvider
-dontwarn org.conscrypt.Conscrypt*
-dontwarn org.openjsse.javax.net.ssl.SSLParameters
-dontwarn org.openjsse.javax.net.ssl.SSLSocket
-dontwarn org.openjsse.net.ssl.OpenJSSE