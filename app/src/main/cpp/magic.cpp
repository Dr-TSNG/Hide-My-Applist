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
 * Copyright (C) 2021 QNotified Contributors
 */

#include <jni.h>
#include <memory>
#include <android/log.h>

// This code is forked from QNotified
// Check custom dex tail to prevent modification

static const int DEX_MAX_SIZE = 12 * 1024 * 1024;
static int64_t sBuildTimestamp = -2;

jclass Application;
jclass Companion;
jobject application;
jobject companion;

inline uint32_t readLe32(const uint8_t *buf, int index) {
    return *((uint32_t *) (buf + index));
}

uint32_t update_adler32(unsigned adler, const uint8_t *data, uint32_t len) {
    unsigned s1 = adler & 0xffffu;
    unsigned s2 = (adler >> 16u) & 0xffffu;

    while (len > 0) {
        /*at least 5550 sums can be done before the sums overflow, saving a lot of module divisions*/
        unsigned amount = len > 5550 ? 5550 : len;
        len -= amount;
        while (amount > 0) {
            s1 += (*data++);
            s2 += s1;
            --amount;
        }
        s1 %= 65521;
        s2 %= 65521;
    }
    return (s2 << 16u) | s1;
}

uint8_t *extractPayload(uint8_t *dex, int dexLength, int *outLength) {
    int chunkROff = readLe32(dex, dexLength - 4);
    if (chunkROff > dexLength) {
        *outLength = 0;
        return nullptr;
    }
    int base = dexLength - chunkROff;
    int size = readLe32(dex, base);
    if (size > dexLength) {
        *outLength = 0;
        return nullptr;
    }
    uint32_t flags = readLe32(dex, base + 4);
    uint32_t a32_got = readLe32(dex, base + 8);
    uint32_t extra = readLe32(dex, base + 12);
    if (flags != 0) {
        *outLength = 0;
        return nullptr;
    }
    uint32_t key = extra & 0xFFu;
    auto *dat = (uint8_t *) malloc(size);
    if (key == 0) {
        memcpy(dat, dex + base + 16, size);
    } else {
        for (int i = 0; i < size; i++) {
            dat[i] = (uint8_t) (key ^ dex[base + 16 + i]);
        }
    }
    uint32_t a32 = update_adler32(1, dat, size);
    if (a32 != a32_got) {
        free(dat);
        *outLength = 0;
        return nullptr;
    }
    return dat;
}

jlong getBuildTimestamp(JNIEnv *env, jclass clazz) {
    if (sBuildTimestamp != -2)return sBuildTimestamp;
    jclass cl_Class = env->FindClass("java/lang/Class");
    jobject loader = env->CallObjectMethod(clazz,
                                           env->GetMethodID(cl_Class, "getClassLoader",
                                                            "()Ljava/lang/ClassLoader;"));
    jobject eu = env->CallObjectMethod(loader,
                                       env->GetMethodID(env->FindClass("java/lang/ClassLoader"),
                                                        "findResources",
                                                        "(Ljava/lang/String;)Ljava/util/Enumeration;"),
                                       env->NewStringUTF("classes.dex"));
    if (eu == nullptr) {
        return -2;
    }
    jclass cl_Enum = env->FindClass("java/util/Enumeration");
    jclass cl_Url = env->FindClass("java/net/URL");
    jmethodID hasMoreElements = env->GetMethodID(cl_Enum, "hasMoreElements", "()Z");
    jmethodID nextElement = env->GetMethodID(cl_Enum, "nextElement", "()Ljava/lang/Object;");
    jbyteArray buf = env->NewByteArray(2048);
    jmethodID openStream = env->GetMethodID(cl_Url, "openStream", "()Ljava/io/InputStream;");
    jclass cIs = env->FindClass("java/io/InputStream");
    jmethodID is_read = env->GetMethodID(cIs, "read", "([B)I");
    jmethodID is_close = env->GetMethodID(cIs, "close", "()V");
    jmethodID toString = env->GetMethodID(env->FindClass("java/lang/Object"), "toString",
                                          "()Ljava/lang/String;");
    if (env->ExceptionCheck()) {
        return -2;
    }
    auto *dex = (uint8_t *) (malloc(DEX_MAX_SIZE));
    if (dex == nullptr) {
        __android_log_print(ANDROID_LOG_ERROR, "SIG", "unable to allocate dex buffer\n");
        return -2;
    }
    int count = 0;
    while (env->CallBooleanMethod(eu, hasMoreElements)) {
        jobject url = env->CallObjectMethod(eu, nextElement);
        if (url == nullptr) {
            continue;
        }
        count++;
        jobject is = env->CallObjectMethod(url, openStream);
        if (is == nullptr) {
            jthrowable ex = env->ExceptionOccurred();
            if (ex != nullptr) {
                auto jst = (jstring) env->CallObjectMethod(ex, toString);
                const char *errStr = env->GetStringUTFChars(jst, nullptr);
                __android_log_print(ANDROID_LOG_ERROR, "SIG", "dex openStream error: %s\n",
                                    errStr);
                env->ReleaseStringUTFChars(jst, errStr);
            }
            env->ExceptionClear();
            continue;
        }
        int length = 0;
        int ri;
        while (!env->ExceptionCheck() && (ri = env->CallIntMethod(is, is_read, buf)) > 0) {
            if (length + ri < DEX_MAX_SIZE) {
                env->GetByteArrayRegion(buf, 0, ri, (jbyte *) (dex + length));
            }
            length += ri;
        }
        if (env->ExceptionCheck()) {
            jthrowable ex = env->ExceptionOccurred();
            if (ex != nullptr) {
                auto jst = (jstring) env->CallObjectMethod(ex, toString);
                const char *errStr = env->GetStringUTFChars(jst, nullptr);
                __android_log_print(ANDROID_LOG_ERROR, "SIG", "dex read error: %s\n",
                                    errStr);
                env->ReleaseStringUTFChars(jst, errStr);
            }
            env->ExceptionClear();
            env->CallVoidMethod(is, is_close);
            env->ExceptionClear();
            continue;
        }
        env->CallVoidMethod(is, is_close);
        env->ExceptionClear();
        {
            //parse [dex, dex+length]
            if (length < 128 * 1024) {
                continue;
            }
            int tailLength = 0;
            uint8_t *tail = extractPayload(dex, length, &tailLength);
            if (tail != nullptr) {
                uint64_t time = 0;
                for (int i = 0; i < 8; i++) {
                    time |= ((uint64_t) ((((uint64_t) tail[i]) & ((uint64_t) 0xFFLLu)))
                            << (8u * i));
                }
                sBuildTimestamp = time;
                free(tail);
                free(dex);
                return time;
            }
        }
    }
    free(dex);
    if (count == 0) {
        __android_log_print(ANDROID_LOG_ERROR, "SIG", "getBuildTimestamp/E urls.size == 0\n");
        return -2;
    }
    sBuildTimestamp = -1;
    return sBuildTimestamp;
}

static inline bool verifySignature(JNIEnv *env) {
    jclass PackageManager = env->FindClass("android/content/pm/PackageManager");
    jclass PackageInfo = env->FindClass("android/content/pm/PackageInfo");
    jclass Signature = env->FindClass("android/content/pm/Signature");
    jclass Arrays = env->FindClass("java/util/Arrays");
    jclass Magic = env->FindClass("com/tsng/hidemyapplist/Magic");

    jmethodID getPackageInfo = env->GetMethodID(
            PackageManager, "getPackageInfo",
            "(Ljava/lang/String;I)Landroid/content/pm/PackageInfo;");
    jmethodID byteArrayEquals = env->GetStaticMethodID(Arrays, "equals", "([B[B)Z");

    jobject pm = env->CallObjectMethod(
            application, env->GetMethodID(
                    Application, "getPackageManager","()Landroid/content/pm/PackageManager;"));
    jobject packageInfo = env->CallObjectMethod(
            pm, getPackageInfo, env->NewStringUTF("com.tsng.hidemyapplist"), 0x40);

    auto signatures = (jobjectArray) env->GetObjectField(packageInfo, env->GetFieldID(
            PackageInfo, "signatures", "[Landroid/content/pm/Signature;"));
    jobject cert = env->GetObjectArrayElement(signatures, 0);
    auto certByteArray = (jbyteArray) env->CallObjectMethod(
            cert, env->GetMethodID(Signature, "toByteArray", "()[B"));
    auto magicNumbers = (jbyteArray) env->GetStaticObjectField(
            Magic, env->GetStaticFieldID(Magic, "magicNumbers", "[B"));

    return env->CallStaticBooleanMethod(Arrays, byteArrayEquals, certByteArray, magicNumbers);
}

extern "C" JNIEXPORT void JNICALL
Java_com_tsng_hidemyapplist_app_MyApplication_nativeInit(JNIEnv *env, jobject thiz) {
    Application = env->GetObjectClass(thiz);
    Companion = env->FindClass("com/tsng/hidemyapplist/app/MyApplication$Companion");
    application = thiz;
    companion = env->GetStaticObjectField(
            Application, env->GetStaticFieldID(
                    Application, "Companion",
                    "Lcom/tsng/hidemyapplist/app/MyApplication$Companion;"));
    if (getBuildTimestamp(env, Application) < 0) return;
    if (!verifySignature(env)) return;

    jobject appContext = env->CallObjectMethod(thiz, env->GetMethodID(
            Application, "getApplicationContext", "()Landroid/content/Context;"));
    env->CallVoidMethod(companion, env->GetMethodID(
            Companion, "setAppContext", "(Landroid/content/Context;)V"), appContext);
}