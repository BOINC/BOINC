#!/bin/bash

# This file is part of BOINC.
# http://boinc.berkeley.edu
# Copyright (C) 2017 University of California
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

# Script to build a wxWidgets GTK version for BOINC

# Usage:
#  cd [path]/wxWidgets-3.0.2/
#  source path_to_this_script [--clean] [--debug] [--prefix PATH]
#
# the --clean argument will force a full rebuild.
# the --debug argument will build the debug version of the library
# if --prefix is given as absolute path the library is installed into there

doclean=""
debug_flag=""
lprefix=""
cmdline_prefix=""
while [[ $# -gt 0 ]]; do
    key="$1"
    case $key in
        -clean|--clean)
        doclean="yes"
        ;;
        -prefix|--prefix)
        lprefix="$2"
        cmdline_prefix="--prefix=${lprefix}"
        shift
        ;;
    esac
    shift # past argument or value
done

cd googletest/make
make
if [ $? -ne 0 ]; then cd ..; return 1; fi
if [ "x${lprefix}" != "x" ]; then
    (cp gtest_main.a ${lprefix}/lib && cp -r ../include/gtest ${lprefix}/include)
    if [ $? -ne 0 ]; then cd ..; return 1; fi
fi

cd ../..
return 0
