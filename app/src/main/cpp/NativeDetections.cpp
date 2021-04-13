#include <jni.h>
#include <unistd.h>
#include <cstdio>
#include <string>

extern "C" JNIEXPORT jboolean JNICALL
Java_com_tsng_hidemyapplist_ui_DetectionActivity_00024DetectionTask_isFileExists(
        JNIEnv* env,
        jobject, jstring path) {
    const char *cpath = env ->GetStringUTFChars(path, nullptr);
    return access(cpath, F_OK) != -1;
}