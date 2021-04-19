#include <cstdio>
#include "dobby.h"

FILE *(*orig_fopen)(const char *filename, const char *mode);

FILE *fake_fopen(const char *filename, const char *mode) {
    // TODO: a lot of things
}