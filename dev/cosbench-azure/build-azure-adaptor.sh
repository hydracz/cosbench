#!/bin/bash

set -eu

ROOT_DIR=$(cd "$(dirname "$0")/../.." && pwd)
AZURE_DIR="$ROOT_DIR/dev/cosbench-azure"
BUILD_DIR="$AZURE_DIR/.build"
CLASSES_DIR="$BUILD_DIR/classes"
VERSION_VALUE=$(cat "$ROOT_DIR/VERSION")
OUTPUT_JAR="$ROOT_DIR/dist/osgi/plugins/cosbench-azure_${VERSION_VALUE}.jar"

echo "Preparing Azure adaptor build directories"
rm -rf "$BUILD_DIR" "$AZURE_DIR/bin"
mkdir -p "$CLASSES_DIR" "$AZURE_DIR/bin" "$ROOT_DIR/dist/osgi/plugins"

LIB_CP=$(printf "%s:" "$ROOT_DIR"/dist/osgi/libs/*.jar)
LIB_CP="${LIB_CP}$ROOT_DIR/dist/osgi/org.eclipse.osgi-3.7.0.v20110613.jar"

echo "Compiling Azure adaptor sources and required COSBench APIs"
javac --release 8 -cp "$LIB_CP" -d "$CLASSES_DIR" $(find \
  "$ROOT_DIR/dev/cosbench-api/src" \
  "$ROOT_DIR/dev/cosbench-config/src" \
  "$ROOT_DIR/dev/cosbench-log/src" \
  "$ROOT_DIR/dev/cosbench-http/src" \
  "$ROOT_DIR/dev/cosbench-azure/src" \
  -name '*.java')

cp -R "$CLASSES_DIR"/. "$AZURE_DIR/bin/"

echo "Packaging Azure adaptor jar: $OUTPUT_JAR"
jar cfm "$OUTPUT_JAR" "$AZURE_DIR/META-INF/MANIFEST.MF" \
  -C "$CLASSES_DIR" com/intel/cosbench/api/azure \
  -C "$CLASSES_DIR" com/intel/cosbench/client/azure \
  -C "$AZURE_DIR" META-INF

echo "Build complete"
echo "$OUTPUT_JAR"