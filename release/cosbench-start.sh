#!/bin/bash
#
#Copyright 2013 Intel Corporation, All Rights Reserved.
#
#Licensed under the Apache License, Version 2.0 (the "License");
#you may not use this file except in compliance with the License.
#You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
#Unless required by applicable law or agreed to in writing, software
#distributed under the License is distributed on an "AS IS" BASIS,
#WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#See the License for the specific language governing permissions and
#limitations under the License.
#

#-------------------------------
# COSBENCH STARTUP SCRIPT
#-------------------------------

SERVICE_NAME=$1

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
ROOT_DIR=$(cd "$SCRIPT_DIR/.." && pwd)

BOOT_LOG="$SCRIPT_DIR/log/$SERVICE_NAME-boot.log"

OSGI_BUNDLES="$2"

OSGI_CONSOLE_PORT=$3

OSGI_CONFIG="$SCRIPT_DIR/conf/.$SERVICE_NAME"

TOMCAT_CONFIG="$SCRIPT_DIR/conf/$SERVICE_NAME-tomcat-server.xml"
COSBENCH_USERS_CONFIG="$SCRIPT_DIR/conf/cosbench-users.xml"
SERVICE_CONFIG="$SCRIPT_DIR/conf/$SERVICE_NAME.conf"

TOOL="nc"
TOOL_PARAMS="-i 0"
TOOL_TIMEOUT=""
STOP_SCRIPT="$SCRIPT_DIR/stop-$SERVICE_NAME.sh"
JAVA_MAJOR=`java -version 2>&1 | awk -F[\".] '/version/ { if ($2 == "1") print $3; else print $2; exit }'`
JVM_COMPAT_ARGS=""
JVM_ADD_OPENS_ARGS=""
OSGI_BOOTDELEGATION="java.sql,java.sql.*,javax.security.auth,javax.security.auth.*,javax.security.cert,javax.security.cert.*,javax.security.sasl,javax.security.sasl.*"
OSGI_SYSTEM_PACKAGES="java.sql,javax.crypto,javax.crypto.interfaces,javax.crypto.spec,javax.management,javax.management.loading,javax.management.modelmbean,javax.management.monitor,javax.management.openmbean,javax.management.relation,javax.management.remote,javax.management.timer,javax.naming,javax.naming.directory,javax.naming.event,javax.naming.ldap,javax.naming.spi,javax.net,javax.net.ssl,javax.security.auth,javax.security.auth.callback,javax.security.auth.login,javax.security.auth.spi,javax.security.auth.x500,javax.security.cert,javax.security.sasl,javax.sql,javax.swing,javax.swing.border,javax.swing.event,javax.swing.table,javax.swing.text,javax.swing.tree,javax.xml.parsers,javax.xml.transform,javax.xml.transform.dom,javax.xml.transform.sax,javax.xml.transform.stream,org.ietf.jgss,org.w3c.dom,org.xml.sax,org.xml.sax.ext,org.xml.sax.helpers"

list_service_pids() {
        ps -eo pid=,command= | awk -v service="/$SERVICE_NAME" -v port="$OSGI_CONSOLE_PORT" '
                /org\.eclipse\.equinox\.launcher\.Main/ {
                        if (index($0, service) || index($0, "-console " port)) print $1;
                }
        '
}

if [ "$JAVA_MAJOR" -ge 9 ] 2>/dev/null; then
                JVM_COMPAT_ARGS="--add-modules=ALL-SYSTEM -Dosgi.compatibility.bootdelegation=true -Dosgi.parentClassloader=ext -Dorg.osgi.framework.bootdelegation=$OSGI_BOOTDELEGATION -Dorg.osgi.framework.system.packages.extra=$OSGI_SYSTEM_PACKAGES"
fi

if [ "$JAVA_MAJOR" -ge 17 ] 2>/dev/null; then
	JVM_ADD_OPENS_ARGS="--add-opens=java.base/java.net=ALL-UNNAMED"
fi

if command -v timeout >/dev/null 2>&1; then
        TOOL_TIMEOUT="timeout 5"
fi

#-------------------------------

if [ -d "$SCRIPT_DIR/main" ] && [ -d "$SCRIPT_DIR/osgi" ]; then
	MAIN_DIR="$SCRIPT_DIR/main"
	OSGI_DIR="$SCRIPT_DIR/osgi"
	LAYOUT_NAME="packaged"
else
	MAIN_DIR="$ROOT_DIR/dist/main"
	OSGI_DIR="$ROOT_DIR/dist/osgi"
	LAYOUT_NAME="source"
fi

RUNTIME_CONFIG_DIR="$SCRIPT_DIR/workspace/.runtime/$SERVICE_NAME"
RUNTIME_CONFIG_FILE="$RUNTIME_CONFIG_DIR/config.ini"
RUNTIME_TOMCAT_CONFIG_FILE="$RUNTIME_CONFIG_DIR/tomcat-server.xml"
# MAIN
#-------------------------------

rm -f "$BOOT_LOG"
mkdir -p "$SCRIPT_DIR/log"

rm -rf "$RUNTIME_CONFIG_DIR"
mkdir -p "$RUNTIME_CONFIG_DIR"

if [ ! -d "$MAIN_DIR" ]; then
	echo "Runtime main directory does not exist: $MAIN_DIR"
	exit 1
fi

if [ ! -d "$OSGI_DIR" ]; then
	echo "Runtime OSGi directory does not exist: $OSGI_DIR"
	exit 1
fi

if [ ! -f "$COSBENCH_USERS_CONFIG" ]; then
        echo "Cosbench users config does not exist: $COSBENCH_USERS_CONFIG"
        exit 1
fi

if [ ! -f "$SERVICE_CONFIG" ]; then
	echo "Cosbench service config does not exist: $SERVICE_CONFIG"
	exit 1
fi

running_pids=`list_service_pids`
if [ -n "$running_pids" ]; then
        echo "Cosbench $SERVICE_NAME is already running with PID(s): $running_pids"
        echo "Stop it first with: $STOP_SCRIPT"
        exit 1
fi

sed \
        -e "s|^osgi.bundles=libs/|osgi.bundles=$OSGI_DIR/libs/|" \
        -e "s|^libs/|$OSGI_DIR/libs/|" \
        -e "s|^plugins/|$OSGI_DIR/plugins/|" \
        -e "s|^osgi.framework=osgi/|osgi.framework=$OSGI_DIR/|" \
	"$OSGI_CONFIG/config.ini" > "$RUNTIME_CONFIG_FILE"

if ! grep -q '^org.osgi.framework.executionenvironment=' "$RUNTIME_CONFIG_FILE"; then
	echo "org.osgi.framework.executionenvironment=J2SE-1.2,J2SE-1.3,J2SE-1.4,J2SE-1.5,J2SE-1.6,J2SE-1.7,J2SE-1.8,JavaSE-1.6,JavaSE-1.7,JavaSE-1.8,JavaSE-9,JavaSE-10,JavaSE-11" >> "$RUNTIME_CONFIG_FILE"
fi

if ! grep -q '^org.osgi.framework.bootdelegation=' "$RUNTIME_CONFIG_FILE"; then
	echo "org.osgi.framework.bootdelegation=$OSGI_BOOTDELEGATION" >> "$RUNTIME_CONFIG_FILE"
fi

if ! grep -q '^org.osgi.framework.system.packages.extra=' "$RUNTIME_CONFIG_FILE"; then
	echo "org.osgi.framework.system.packages.extra=$OSGI_SYSTEM_PACKAGES" >> "$RUNTIME_CONFIG_FILE"
fi

sed \
        -e "s|pathname=\"\./conf/cosbench-users.xml\"|pathname=\"$COSBENCH_USERS_CONFIG\"|" \
        -e "s|pathname=\"conf/cosbench-users.xml\"|pathname=\"$COSBENCH_USERS_CONFIG\"|" \
        "$TOMCAT_CONFIG" > "$RUNTIME_TOMCAT_CONFIG_FILE"

echo "Launching osgi framework ... "

(
	cd "$ROOT_DIR" || exit 1
        exec /usr/bin/nohup java $JVM_COMPAT_ARGS $JVM_ADD_OPENS_ARGS -Dcosbench.tomcat.config="$RUNTIME_TOMCAT_CONFIG_FILE" -Dcosbench.web.cosbenchUsers="$COSBENCH_USERS_CONFIG" -Dcosbench.$SERVICE_NAME.config="$SERVICE_CONFIG" -Dcosbench.runtime.layout="$LAYOUT_NAME" -server -cp "$MAIN_DIR"/* org.eclipse.equinox.launcher.Main -configuration "$RUNTIME_CONFIG_DIR" -console $OSGI_CONSOLE_PORT
) 1> "$BOOT_LOG" 2>&1 &

if [ $? -ne 0 ];
then
        echo "Error in launching osgi framework!"
        cat "$BOOT_LOG"
        exit 1
fi

sleep 1

echo "Successfully launched osgi framework!"

echo "Booting cosbench $SERVICE_NAME ... "

succ=1

which $TOOL 1>&2 >/dev/null
if [ $? -ne 0 ]; then
	echo "No appropriate tool found to detect cosbench $SERVICE_NAME status."
	attemps=60
	while [ $attemps -gt 0 ]; do
		attemps=`expr $attemps - 1`
                printf "."
		sleep 1
	done
	succ=2
	echo
	echo "Started cosbench $SERVICE_NAME!"
else

for module in $OSGI_BUNDLES
do
        bundle_name=`echo "$module" | sed 's/_[-0-9.]*$//'`
        ready=0
        attempts=60
        while [ $ready -ne 1 ];
        do
		printf 'ss\n' | $TOOL_TIMEOUT $TOOL $TOOL_PARAMS 0.0.0.0 $OSGI_CONSOLE_PORT 2>/dev/null | grep "$bundle_name" >> /dev/null
                if [ $? -ne 0 ];
                then
                        attempts=`expr $attempts - 1`
                        if [ $attempts -eq 0 ];
                        then
                                if [ $attempts -ne 60 ]; then echo ""; fi
                                echo "Starting    $bundle_name    [ERROR]"
                                succ=0
                                break
                        else
                                printf "."
                                sleep 1
                        fi
                else
                        if [ $attempts -ne 60 ]; then echo ""; fi
                        echo "Starting    $bundle_name    [OK]"
                        ready=1
                fi
        done
done
fi

if [ $succ -eq 0 ];
then
        echo "Error in booting cosbench $SERVICE_NAME!"
        exit 1
elif [ $succ -eq 1 ]; then
	echo "Successfully started cosbench $SERVICE_NAME!"
fi

cat "$BOOT_LOG"

exit 0
