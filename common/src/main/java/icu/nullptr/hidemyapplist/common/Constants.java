package icu.nullptr.hidemyapplist.common;

public class Constants {
    public static final String APP_PACKAGE_NAME = "com.tsng.hidemyapplist";
    public static final String CLASS_PMS = "com.android.server.pm.PackageManagerService";
    public static final String[] CLASS_EXT_PMS = {
            "com.android.server.pm.OplusPackageManagerService",
            "com.android.server.pm.OppoPackageManagerService"
    };

    public static final String DESCRIPTOR = "android.content.pm.IPackageManager";
    public static final int TRANSACTION = 'H' << 24 | 'M' << 16 | 'A' << 8 | 'D';
    public static final int ACTION_GET_BINDER = 1;
}
