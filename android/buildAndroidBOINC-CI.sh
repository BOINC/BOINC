#!/bin/sh
set -e

# This file is part of BOINC.
# http://boinc.berkeley.edu
# Copyright (C) 2018 University of California
#
# BOINC is free software; you can redistribute it and/or modify it
# under the terms of the GNU Lesser General Public License
# as published by the Free Software Foundation,
# either version 3 of the License, or (at your option) any later version.
#
# BOINC is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
# See the GNU Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public License
# along with BOINC.  If not, see <http://www.gnu.org/licenses/>.
#

# checks if a given path is canonical (absolute and does not contain relative links)
# from http://unix.stackexchange.com/a/256437
isPathCanonical() {
  case "x$1" in
    (x*/..|x*/../*|x../*|x*/.|x*/./*|x./*)
        rc=1
        ;;
    (x/*)
        rc=0
        ;;
    (*)
        rc=1
        ;;
  esac
  return $rc
}

doclean=""
cache_dir=""
arch=""
while [[ $# -gt 0 ]]; do
    key="$1"
    case $key in
        --cache_dir)
        cache_dir="$2"
        shift
        ;;
        --clean)
        doclean="yes"
        ;;
        --arch)
        arch="$2"
        shift
        ;;
        *)
        echo "unrecognized option $key"
        ;;
    esac
    shift # past argument or value
done

if [ "x$cache_dir" != "x" ]; then
    if isPathCanonical "$cache_dir" && [ "$cache_dir" != "/" ]; then
         PREFIX="$cache_dir"
     else
         echo "cache_dir must be an absolute path without ./ or ../ in it"
         exit 1
     fi
else
    cd ../
    PREFIX="$(pwd)/3rdParty/buildCache/android"
    cd android/
fi

mkdir -p "${PREFIX}"

if [ "${doclean}" = "yes" ]; then
    echo "cleaning cache"
    rm -rf "${PREFIX}"
    mkdir -p "${PREFIX}"
fi

case "$arch" in
    "arm")
        ./build_androidtc_arm.sh --cache_dir "${PREFIX}"
        ./build_openssl_arm.sh --cache_dir "${PREFIX}"
        ./build_curl_arm.sh --cache_dir "${PREFIX}"
        ./build_boinc_arm.sh --cache_dir "${PREFIX}"

        exit 0
    ;;
    "arm64")
        ./build_androidtc_arm64.sh --cache_dir "${PREFIX}"
        ./build_openssl_arm64.sh --cache_dir "${PREFIX}"
        ./build_curl_arm64.sh --cache_dir "${PREFIX}"
        ./build_boinc_arm64.sh --cache_dir "${PREFIX}"

        exit 0
    ;;
    "mips")
        ./build_androidtc_mips.sh --cache_dir "${PREFIX}"
        ./build_openssl_mips.sh --cache_dir "${PREFIX}"
        ./build_curl_mips.sh --cache_dir "${PREFIX}"
        ./build_boinc_mips.sh --cache_dir "${PREFIX}"

        exit 0
    ;;
    "mips64")
        ./build_androidtc_mips64.sh --cache_dir "${PREFIX}"
        ./build_openssl_mips64.sh --cache_dir "${PREFIX}"
        ./build_curl_mips64.sh --cache_dir "${PREFIX}"
        ./build_boinc_mips64.sh --cache_dir "${PREFIX}"

        exit 0
    ;;
    "x86")
        ./build_androidtc_x86.sh --cache_dir "${PREFIX}"
        ./build_openssl_x86.sh --cache_dir "${PREFIX}"
        ./build_curl_x86.sh --cache_dir "${PREFIX}"
        ./build_boinc_x86.sh --cache_dir "${PREFIX}"

        exit 0
    ;;
    "x86_64")
        ./build_androidtc_x86_64.sh --cache_dir "${PREFIX}"
        ./build_openssl_x86_64.sh --cache_dir "${PREFIX}"
        ./build_curl_x86_64.sh --cache_dir "${PREFIX}"
        ./build_boinc_x86_64.sh --cache_dir "${PREFIX}"

        exit 0
    ;;
esac

echo "unknown architecture: $arch"
exit 1
