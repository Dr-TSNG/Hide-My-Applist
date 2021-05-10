#include <jni.h>
#include <fcntl.h>
#include <unistd.h>
#include <sys/stat.h>
#include <cstdio>
#include <string>

#include "linux_syscall_support.h"

extern "C" JNIEXPORT jint JNICALL
Java_com_tsng_hidemyapplist_ui_DetectionActivity_00024DetectionTask_nativeFile(JNIEnv *env, jobject, jstring path) {
    const char *cpath = env->GetStringUTFChars(path, nullptr);
    jint result = 0;
    struct stat buf;
    struct kernel_stat buf_s;
    result |= (access(cpath, F_OK) != -1);
    result |= (stat(cpath, &buf) != -1) << 1;
    result |= (fstat(open(cpath, O_PATH), &buf) != -1) << 2;
    result |= (sys_stat(cpath, &buf_s) != -1) << 3;
    result |= (sys_fstat(sys_open(cpath, O_PATH, 0), &buf_s) != -1) << 4;
    return result;
}