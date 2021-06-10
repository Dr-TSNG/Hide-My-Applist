#include <jni.h>
#include <fcntl.h>
#include <unistd.h>

#include "genuine.h"
#include "sigverify.h"

int sign = 0x77f;

static bool isApkSigBlock42(const char *buffer) {
    // APK Sig Block 42
    return *buffer == 'A'
           && *++buffer == 'P'
           && *++buffer == 'K'
           && *++buffer == ' '
           && *++buffer == 'S'
           && *++buffer == 'i'
           && *++buffer == 'g'
           && *++buffer == ' '
           && *++buffer == 'B'
           && *++buffer == 'l'
           && *++buffer == 'o'
           && *++buffer == 'c'
           && *++buffer == 'k'
           && *++buffer == ' '
           && *++buffer == '4'
           && *++buffer == '2';
}

void checkSignature(const char *path) {
    unsigned char buffer[0x11] = {0};
    uint32_t size4;
    uint64_t size8, size_of_block;
    sign = 0x77f;
    int fd = (int) openat(AT_FDCWD, path, O_RDONLY);
    if (fd < 0) return;
    sign = 0x1bf52;
    for (int i = 0;; ++i) {
        unsigned short n;
        lseek(fd, -i - 2, SEEK_END);
        read(fd, &n, 2);
        if (n == i) {
            lseek(fd, -22, SEEK_CUR);
            read(fd, &size4, 4);
            if ((size4 ^ 0xcafebabeu) == 0xccfbf1eeu) {
                break;
            }
        }
        if (i == 0xffff) {
            goto clean;
        }
    }

    lseek(fd, 12, SEEK_CUR);
    // offset
    read(fd, &size4, 0x4);
    lseek(fd, (off_t) (size4 - 0x18), SEEK_SET);

    read(fd, &size8, 0x8);
    read(fd, buffer, 0x10);
    if (!isApkSigBlock42((char *) buffer)) {
        goto clean;
    }

    lseek(fd, (off_t) (size4 - (size8 + 0x8)), SEEK_SET);
    read(fd, &size_of_block, 0x8);
    if (size_of_block != size8) {
        goto clean;
    }

    for (;;) {
        uint32_t id;
        uint32_t offset;
        read(fd, &size8, 0x8); // sequence length
        if (size8 == size_of_block) {
            break;
        }
        read(fd, &id, 0x4); // id
        offset = 4;
        if ((id ^ 0xdeadbeefu) == 0xafa439f5u || (id ^ 0xdeadbeefu) == 0x2efed62f) {
            read(fd, &size4, 0x4); // signer-sequence length
            read(fd, &size4, 0x4); // signer length
            read(fd, &size4, 0x4); // signed data length
            offset += 0x4 * 3;

            read(fd, &size4, 0x4); // digests-sequence length
            lseek(fd, (off_t) (size4), SEEK_CUR);// skip digests
            offset += 0x4 + size4;

            read(fd, &size4, 0x4); // certificates length
            read(fd, &size4, 0x4); // certificate length
            offset += 0x4 * 2;
            if (size4 == GENUINE_SIZE) {
                int hash = 1;
                signed char c;
                for (unsigned i = 0; i < size4; ++i) {
                    read(fd, &c, 0x1);
                    hash = 31 * hash + c;
                }
                offset += size4;
                if ((((unsigned) hash) ^ 0x14131211u) == GENUINE_HASH) {
                    sign = 0x29a;
                    break;
                }
            }
        }
        lseek(fd, (off_t) (size8 - offset), SEEK_CUR);
    }

    clean:
    close(fd);
}

extern "C" JNIEXPORT void JNICALL
Java_com_tsng_hidemyapplist_MainActivity_initNative(JNIEnv *env, jobject, jstring path) {
    const char *cpath = env->GetStringUTFChars(path, nullptr);
    checkSignature(cpath);
    env->ReleaseStringUTFChars(path, cpath);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_tsng_hidemyapplist_UtilsKt_nativeSync(JNIEnv *, jclass clazz, jint result) {
    return sign ^ 0b1010011010 ? result * (0x53 - 0b1010011) + 1 : result * (0x4c - 0b1001011);
}