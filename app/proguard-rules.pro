# Magic
-keep class com.tsng.hidemyapplist.Magic { *; }

# Xposed
-keepclassmembers class icu.nullptr.hidemyapplist.MyApp {
    boolean isHooked;
}

# Enum class
-keepclassmembers,allowoptimization enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keep class icu.nullptr.hidemyapplist.data.UpdateData { *; }
-keep class icu.nullptr.hidemyapplist.data.UpdateData$* { *; }
