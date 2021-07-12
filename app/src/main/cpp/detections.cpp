/*
 * This file is part of Hide My Applist.
 *
 * Hide My Applist is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with Hide My Applist.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2021 Hide My Applist Contributors
 */

#include <jni.h>
#include <sys/stat.h>
#include <android/log.h>
#include <linux_syscall_support.h>
#include <ctime>
#include <cstdio>

static int syscall_result;
static jint syscall_detect(int func) {
    jint result = (syscall_result == 0) ? func == 0 : -1;
    syscall_result = 0;
    return result;
}

static void signal_handler(int sig) {
    syscall_result = -1;
    __android_log_print(ANDROID_LOG_INFO, "[HMA Detections]", "[INFO] Syscall was denied");
}

extern "C" JNIEXPORT jintArray JNICALL
Java_com_tsng_hidemyapplist_app_ui_activities_DetectionActivity_00024DetectionTask_nativeFile(JNIEnv *env, jobject, jstring path) {
    const char *cpath = env->GetStringUTFChars( path, nullptr);
    const jsize sz = 6;
    jint results[sz];
    struct stat buf;
    struct kernel_stat buf_s;
    signal(SIGSYS, signal_handler);
    results[0] = (access(cpath, F_OK) == 0);
    results[1] = (stat(cpath, &buf) == 0);
    results[2] = (fstat(open(cpath, O_PATH), &buf) == 0);
    results[3] = syscall_detect(sys_stat(cpath, &buf_s));
    results[4] = syscall_detect(sys_fstat(sys_open(cpath, O_PATH, 0), &buf_s));
    jintArray ret = env->NewIntArray(sz);
    env->ReleaseStringUTFChars(path, cpath);
    env->SetIntArrayRegion(ret, 0, sz, results);
    return ret;
}