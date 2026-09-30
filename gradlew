#!/bin/sh

#
# Copyright © 2015-2021 the original authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

set -e

# Attempt to set APP_HOME
# Resolve links: $0 may be a link
PRG="$0"
# Need this for relative symlinks.
while [ -h "$PRG" ] ; do
    ls=`ls -ld "$PRG"`
    link=`expr "$ls" : '.*-> \(.*\)$'`
    if expr "$link" : '/.*' > /dev/null; then
        PRG="$link"
    else
        PRG=`dirname "$PRG"`"/$link"
    fi
done
SAVED="`pwd`"
cd "`dirname \"$PRG\"`/" >/dev/null
APP_HOME="`pwd -P`"
cd "$SAVED" >/dev/null

APP_NAME="Gradle"
APP_BASE_NAME=`basename "$0"`

# Use system gradle if available, otherwise java with gradle wrapper
if command -v gradle >/dev/null 2>&1; then
    exec gradle "$@"
fi

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

if [ -f "$CLASSPATH" ]; then
    if [ -n "$JAVA_HOME" ] ; then
        JAVACMD="$JAVA_HOME/bin/java"
    else
        JAVACMD="java"
    fi
    exec "$JAVACMD" -jar "$CLASSPATH" "$@"
else
    echo "Gradle executable or wrapper jar not found in PATH." >&2
    exit 1
fi
