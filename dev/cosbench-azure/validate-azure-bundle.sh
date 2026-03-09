#!/bin/bash

set -eu

ROOT_DIR=$(cd "$(dirname "$0")/../.." && pwd)
AZURE_DIR="$ROOT_DIR/dev/cosbench-azure"
BUILD_SCRIPT="$AZURE_DIR/build-azure-adaptor.sh"
VALIDATION_DIR="$AZURE_DIR/.build/validation"
PLUGINS_DIR="$VALIDATION_DIR/plugins"
CONFIG_DIR="$VALIDATION_DIR/config"
LOG_FILE="$VALIDATION_DIR/osgi.log"
CONSOLE_PORT=${CONSOLE_PORT:-18123}
VERSION_VALUE=$(cat "$ROOT_DIR/VERSION")
AZURE_JAR="$ROOT_DIR/dist/osgi/plugins/cosbench-azure_${VERSION_VALUE}.jar"
PID_FILE="$VALIDATION_DIR/osgi.pid"

cleanup() {
  if [ -f "$PID_FILE" ]; then
    PID=$(cat "$PID_FILE")
    kill "$PID" >/dev/null 2>&1 || true
    rm -f "$PID_FILE"
  fi
}

trap cleanup EXIT

echo "Building Azure adaptor before validation"
"$BUILD_SCRIPT" >/dev/null

echo "Preparing minimal OSGi validation runtime"
rm -rf "$VALIDATION_DIR"
mkdir -p "$PLUGINS_DIR" "$CONFIG_DIR"

LIB_CP=$(printf "%s:" "$ROOT_DIR"/dist/osgi/libs/*.jar)
LIB_CP="${LIB_CP}$ROOT_DIR/dist/osgi/org.eclipse.osgi-3.7.0.v20110613.jar"

CLASSES_DIR="$AZURE_DIR/.build/classes"

javac --release 8 -cp "$LIB_CP" -d "$CLASSES_DIR" $(find \
  "$ROOT_DIR/dev/cosbench-api/src" \
  "$ROOT_DIR/dev/cosbench-config/src" \
  "$ROOT_DIR/dev/cosbench-log/src" \
  "$ROOT_DIR/dev/cosbench-http/src" \
  -name '*.java') >/dev/null

jar cfm "$PLUGINS_DIR/cosbench-api.jar" "$ROOT_DIR/dev/cosbench-api/META-INF/MANIFEST.MF" \
  -C "$CLASSES_DIR" com/intel/cosbench/api >/dev/null
jar cfm "$PLUGINS_DIR/cosbench-config.jar" "$ROOT_DIR/dev/cosbench-config/META-INF/MANIFEST.MF" \
  -C "$CLASSES_DIR" com/intel/cosbench/config >/dev/null
jar cfm "$PLUGINS_DIR/cosbench-log.jar" "$ROOT_DIR/dev/cosbench-log/META-INF/MANIFEST.MF" \
  -C "$CLASSES_DIR" com/intel/cosbench/log >/dev/null
jar cfm "$PLUGINS_DIR/cosbench-http.jar" "$ROOT_DIR/dev/cosbench-http/META-INF/MANIFEST.MF" \
  -C "$CLASSES_DIR" com/intel/cosbench/client/http >/dev/null
cp "$AZURE_JAR" "$PLUGINS_DIR/cosbench-azure.jar"

cat > "$CONFIG_DIR/config.ini" <<EOF
osgi.bundles=\
reference:file:$ROOT_DIR/dist/osgi/libs/com.springsource.org.apache.commons.io-1.4.0.jar@2\:start,\
reference:file:$ROOT_DIR/dist/osgi/libs/com.springsource.javax.servlet-2.5.0.jar@1\:start,\
reference:file:$ROOT_DIR/dist/osgi/libs/com.springsource.javax.el-1.0.0.jar@1\:start,\
reference:file:$ROOT_DIR/dist/osgi/libs/com.springsource.javax.servlet.jsp-2.1.0.jar@1\:start,\
reference:file:$ROOT_DIR/dist/osgi/libs/com.springsource.org.apache.log4j-1.2.15.jar@2\:start,\
reference:file:$ROOT_DIR/dist/osgi/libs/com.springsource.slf4j.api-1.5.6.jar@2\:start,\
reference:file:$ROOT_DIR/dist/osgi/libs/com.springsource.slf4j.log4j-1.5.6.jar@2\:start,\
reference:file:$ROOT_DIR/dist/osgi/libs/com.springsource.slf4j.org.apache.commons.logging-1.5.6.jar@2\:start,\
reference:file:$ROOT_DIR/dist/osgi/libs/com.springsource.org.apache.commons.configuration-1.5.0.jar@2\:start,\
reference:file:$ROOT_DIR/dist/osgi/libs/com.springsource.org.apache.commons.beanutils-1.7.0.jar@2\:start,\
reference:file:$ROOT_DIR/dist/osgi/libs/com.springsource.org.apache.commons.collections-3.2.0.jar@2\:start,\
reference:file:$ROOT_DIR/dist/osgi/libs/com.springsource.org.apache.commons.digester-1.8.0.jar@2\:start,\
reference:file:$ROOT_DIR/dist/osgi/libs/com.springsource.org.apache.commons.jxpath-1.2.0.jar@2\:start,\
reference:file:$ROOT_DIR/dist/osgi/libs/com.springsource.org.apache.commons.lang-2.5.0.jar@2\:start,\
reference:file:$ROOT_DIR/dist/osgi/libs/com.springsource.org.jdom-1.1.0.jar@2\:start,\
reference:file:$ROOT_DIR/dist/osgi/libs/com.springsource.org.castor-1.2.0.jar@2\:start,\
reference:file:$ROOT_DIR/dist/osgi/libs/com.springsource.org.apache.commons.codec-1.3.0.jar@2\:start,\
reference:file:$ROOT_DIR/dist/osgi/libs/com.springsource.org.aopalliance-1.0.0.jar@2\:start,\
reference:file:$ROOT_DIR/dist/osgi/libs/org.apache.httpcomponents.httpclient_4.1.3.jar@2\:start,\
reference:file:$ROOT_DIR/dist/osgi/libs/org.apache.httpcomponents.httpcore_4.1.4.jar@2\:start,\
reference:file:$ROOT_DIR/dist/osgi/libs/org.springframework.aop-3.0.5.RELEASE.jar@3\:start,\
reference:file:$ROOT_DIR/dist/osgi/libs/org.springframework.asm-3.0.5.RELEASE.jar@3\:start,\
reference:file:$ROOT_DIR/dist/osgi/libs/org.springframework.beans-3.0.5.RELEASE.jar@3\:start,\
reference:file:$ROOT_DIR/dist/osgi/libs/org.springframework.context-3.0.5.RELEASE.jar@3\:start,\
reference:file:$ROOT_DIR/dist/osgi/libs/org.springframework.context.support-3.0.5.RELEASE.jar@3\:start,\
reference:file:$ROOT_DIR/dist/osgi/libs/org.springframework.core-3.0.5.RELEASE.jar@3\:start,\
reference:file:$ROOT_DIR/dist/osgi/libs/org.springframework.expression-3.0.5.RELEASE.jar@3\:start,\
reference:file:$ROOT_DIR/dist/osgi/libs/spring-osgi-core-2.0.0.M1.jar@4\:start,\
reference:file:$ROOT_DIR/dist/osgi/libs/spring-osgi-extender-2.0.0.M1.jar@4\:start,\
reference:file:$ROOT_DIR/dist/osgi/libs/spring-osgi-io-2.0.0.M1.jar@4\:start,\
reference:file:$PLUGINS_DIR/cosbench-log.jar@6\:start,\
reference:file:$PLUGINS_DIR/cosbench-config.jar@6\:start,\
reference:file:$PLUGINS_DIR/cosbench-http.jar@6\:start,\
reference:file:$PLUGINS_DIR/cosbench-api.jar@7\:start,\
reference:file:$PLUGINS_DIR/cosbench-azure.jar@7\:start
osgi.clean=true
osgi.noShutdown=true
osgi.startLevel=8
osgi.bundles.defaultStartLevel=8
osgi.configuration.cascaded=false
osgi.framework=$ROOT_DIR/dist/osgi/org.eclipse.osgi-3.7.0.v20110613.jar
org.osgi.framework.executionenvironment=J2SE-1.5,JavaSE-1.6,JavaSE-1.7,JavaSE-1.8,JavaSE-9,JavaSE-10,JavaSE-11
org.osgi.framework.system.packages.extra=javax.crypto,javax.crypto.interfaces,javax.crypto.spec,javax.management,javax.management.loading,javax.management.modelmbean,javax.management.monitor,javax.management.openmbean,javax.management.relation,javax.management.remote,javax.management.timer,javax.naming,javax.naming.directory,javax.naming.event,javax.naming.ldap,javax.naming.spi,javax.net,javax.net.ssl,javax.security.auth.x500,javax.sql,javax.swing,javax.swing.border,javax.swing.event,javax.swing.table,javax.swing.text,javax.swing.tree,javax.xml.parsers,javax.xml.transform,javax.xml.transform.dom,javax.xml.transform.sax,javax.xml.transform.stream,org.ietf.jgss,org.w3c.dom,org.xml.sax,org.xml.sax.ext,org.xml.sax.helpers
eclipse.consoleLog=true
eclipse.ignoreApp=true
EOF

echo "Launching minimal OSGi runtime on console port $CONSOLE_PORT"
java -cp "$ROOT_DIR/dist/main/*" org.eclipse.equinox.launcher.Main \
  -configuration "$CONFIG_DIR" \
  -console "$CONSOLE_PORT" >"$LOG_FILE" 2>&1 &
echo $! > "$PID_FILE"

sleep 8

STATUS_OUTPUT=$(printf 'ss\n' | nc 127.0.0.1 "$CONSOLE_PORT" || true)
echo "$STATUS_OUTPUT"

AZURE_LINE=$(echo "$STATUS_OUTPUT" | grep 'cosbench-azure' || true)

if [ -z "$AZURE_LINE" ]; then
  echo "Azure bundle is not present in the OSGi state output" >&2
  exit 1
fi

AZURE_STATE=$(echo "$AZURE_LINE" | awk '{print $2}')
echo "Azure bundle state: $AZURE_STATE"

if [ "$AZURE_STATE" = "ACTIVE" ]; then
  echo "Azure bundle is active in the minimal OSGi runtime"
else
  echo "Azure bundle is visible but not active in the minimal OSGi runtime"
  echo "This reduced validation environment proves bundle discovery only; full driver activation still requires the complete COSBench plugin set." 
fi