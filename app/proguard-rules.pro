# Magic
-keep class com.tsng.hidemyapplist.Magic { *; }
-keep class com.tsng.hidemyapplist.app.MyApplication {
    static final com.tsng.hidemyapplist.app.MyApplication$Companion Companion;
}
-keep class com.tsng.hidemyapplist.app.MyApplication$Companion { *; }

# Views
-keep class com.tsng.hidemyapplist.app.ui.activities.ModuleActivity$Fragment { *; }
-keep class com.tsng.hidemyapplist.app.ui.views.FilterRulesView{ *; }

# Xposed
-keepclassmembers class com.tsng.hidemyapplist.app.MyApplication {
    static final boolean isModuleActivated;
}

# Enum class
-keepclassmembers,allowoptimization enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
