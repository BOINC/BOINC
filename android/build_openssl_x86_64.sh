#!/bin/sh
set -e

#
# See: http://boinc.berkeley.edu/trac/wiki/AndroidBuildClient#
#

# Script to compile OpenSSL for Android

COMPILEOPENSSL="${COMPILEOPENSSL:-yes}"
SILENT_MODE="${SILENT_MODE:-no}"
CONFIGURE="yes"
MAKECLEAN="yes"

OPENSSL="${OPENSSL_SRC:-$HOME/src/openssl-1.0.2p}" #openSSL sources, requiered by BOINC

export ANDROID_TC="${ANDROID_TC:-$HOME/android-tc}"
export ANDROIDTC="${ANDROID_TC_X86_64:-$ANDROID_TC/x86_64}"
export TCBINARIES="$ANDROIDTC/bin"
export TCINCLUDES="$ANDROIDTC/x86_64-linux-android"
export TCSYSROOT="$ANDROIDTC/sysroot"
export STDCPPTC="$TCINCLUDES/lib/libstdc++.a"

export PATH="$TCBINARIES:$TCINCLUDES/bin:$PATH"
export CC=x86_64-linux-android-clang
export CXX=x86_64-linux-android-clang++
export LD=x86_64-linux-android-ld
export CFLAGS="--sysroot=$TCSYSROOT -DANDROID -Wall -I$TCINCLUDES/include -O3 -fomit-frame-pointer -fPIE -D__ANDROID_API__=21"
export CXXFLAGS="--sysroot=$TCSYSROOT -DANDROID -Wall -funroll-loops -fexceptions -O3 -fomit-frame-pointer -fPIE -D__ANDROID_API__=21"
export LDFLAGS="-L$TCSYSROOT/usr/lib -L$TCINCLUDES/lib -llog -fPIE -pie -latomic -static-libstdc++"
export GDB_CFLAGS="--sysroot=$TCSYSROOT -Wall -g -I$TCINCLUDES/include"

# Prepare android toolchain and environment
./build_androidtc_x86_64.sh

if [ "$COMPILEOPENSSL" = "yes" ]; then
    echo "===== building openssl for x86-64 from $OPENSSL ====="
    cd "$OPENSSL"
    if [ -n "$MAKECLEAN" ]; then
        if [ "$SILENT_MODE" = "yes" ]; then
            make clean 1>/dev/null 2>&1
        else
            make clean
        fi
    fi
    if [ -n "$CONFIGURE" ]; then
        if [ "$SILENT_MODE" = "yes" ]; then
            ./Configure linux-x86_64 no-shared no-dso -DL_ENDIAN --openssldir="$TCINCLUDES/ssl" 1>/dev/null
        else
            ./Configure linux-x86_64 no-shared no-dso -DL_ENDIAN --openssldir="$TCINCLUDES/ssl"
        fi
        #override flags in Makefile
        sed -e "s/^CFLAG=.*$/`grep -e \^CFLAG= Makefile` \$(CFLAGS)/g
s%^INSTALLTOP=.*%INSTALLTOP=$TCINCLUDES%g" Makefile > Makefile.out
        mv Makefile.out Makefile
    fi
    if [ "$SILENT_MODE" = "yes" ]; then
        make 1>/dev/null
        make install_sw 1>/dev/null
    else
        make
        make install_sw
    fi
    echo "===== openssl for x86-64 build done ====="
fi
