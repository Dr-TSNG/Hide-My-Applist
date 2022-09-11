package icu.nullptr.hidemyapplist.common;

import icu.nullptr.hidemyapplist.common.LDMP;

interface IHMAService {

    void stopService(boolean cleanEnv) = 0;

    void syncConfig(String json) = 1;

    int getServiceVersion() = 2;

    int getFilterCount() = 3;

    String getLogs() = 4;

    void clearLogs() = 5;

    Map<String, LDMP> getLDMP(int uid) = 6;
}
