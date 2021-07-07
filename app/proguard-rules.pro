#指定压缩级别
-optimizationpasses 5

#不跳过非公共的库的类成员
-dontskipnonpubliclibraryclassmembers

#混淆时采用的算法
-optimizations !code/simplification/arithmetic, !field,!class/merging, !code/allocation/variable

#把混淆类中的方法名也混淆了
-useuniqueclassmembernames

#优化时允许访问并修改有修饰符的类和类的成员
-allowaccessmodification

#将文件来源重命名为“SourceFile”字符串
-renamesourcefileattribute SourceFile

#保持异常不被混淆
-keepattributes Exceptions

#保留行号
-keepattributes SourceFile, LineNumberTable

#保持泛型
-keepattributes Signature

#保持反射
-keepattributes EnclosingMethod

#保持所有实现 Serializable 接口的类成员
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

#Fragment不需要在AndroidManifest.xml中注册，需要额外保护下
-keep public class * extends androidx.fragment.app.Fragment

#Config
-keep class com.tsng.hidemyapplist.JsonConfig { *; }
-keep class com.tsng.hidemyapplist.JsonConfig$* { *; }

#Xposed
-keep class com.tsng.hidemyapplist.xposed.XposedEntry
-keepclassmembers class com.tsng.hidemyapplist.app.ui.activities.MainActivity {
    static final boolean isModuleActivated;
}