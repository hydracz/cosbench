#!/bin/bash

set -eu

ROOT_DIR=$(cd "$(dirname "$0")/../.." && pwd)
DRIVER_CONFIG="$ROOT_DIR/release/conf/.driver/config.ini"
BUILD_DIR="$ROOT_DIR/dev/cosbench-azure/.build/driver-export"
CLASSES_DIR="$BUILD_DIR/classes"
PLUGINS_DIR="$ROOT_DIR/dist/osgi/plugins"
VERSION_VALUE=$(cat "$ROOT_DIR/VERSION")
ERROR_DIR="$BUILD_DIR/errors"

if [ ! -f "$DRIVER_CONFIG" ]; then
  echo "Driver OSGi config does not exist: $DRIVER_CONFIG" >&2
  exit 1
fi

mkdir -p "$PLUGINS_DIR"
rm -rf "$BUILD_DIR"
mkdir -p "$CLASSES_DIR" "$ERROR_DIR"

modules=$(sed -n 's/.*plugins\/\([^@,\\]*\).*/\1/p' "$DRIVER_CONFIG" | awk '!seen[$0]++')

LIB_CP=$(printf "%s:" "$ROOT_DIR"/dist/osgi/libs/*.jar)
LIB_CP="${LIB_CP}$ROOT_DIR/dist/osgi/org.eclipse.osgi-3.7.0.v20110613.jar"

compiled_modules=""
skipped_modules=""

echo "Compiling driver plugin sources by module"
for module in $modules; do
  module_dir="$ROOT_DIR/dev/$module"
  if [ ! -d "$module_dir/src" ]; then
    continue
  fi

  java_files=$(find "$module_dir/src" -type f -name '*.java' ! -path '*/.*/*')
  if [ -z "$java_files" ]; then
    continue
  fi

  module_cp="$LIB_CP:$CLASSES_DIR"
  if find "$module_dir" -maxdepth 1 -type f -name '*.jar' | grep . >/dev/null; then
    module_jars=$(printf "%s:" "$module_dir"/*.jar)
    module_cp="$module_cp:$module_jars"
  fi

  error_file="$ERROR_DIR/$module.log"
  if javac --release 8 -encoding ISO-8859-1 -cp "$module_cp" -d "$CLASSES_DIR" $java_files >"$error_file" 2>&1; then
    compiled_modules="$compiled_modules $module"
    echo "COMPILED  $module"
  else
    skipped_modules="$skipped_modules $module"
    echo "SKIPPED   $module"
    echo "  compile log: $error_file"
  fi
done

echo "Packaging driver plugin jars"
for module in $modules; do
  module_dir="$ROOT_DIR/dev/$module"
  manifest_file="$module_dir/META-INF/MANIFEST.MF"
  output_jar="$PLUGINS_DIR/${module}_${VERSION_VALUE}.jar"

  if [ ! -f "$manifest_file" ]; then
    echo "Skipping $module because manifest is missing" >&2
    continue
  fi

  temp_dir="$BUILD_DIR/$module"
  rm -rf "$temp_dir"
  mkdir -p "$temp_dir"

  cp -R "$module_dir/META-INF" "$temp_dir/"

  if [ -d "$module_dir/src" ]; then
    if ! echo "$compiled_modules" | grep -q " $module"; then
      echo "Skipping jar packaging for $module because compilation did not complete"
      continue
    fi

    find "$module_dir/src" -type f -name '*.java' ! -path '*/.*/*' | while read -r source_file; do
      source_relative=${source_file#"$module_dir/src/"}
      class_relative=${source_relative%.java}.class
      class_dir=$(dirname "$class_relative")
      class_base=$(basename "${class_relative%.class}")
      if [ -d "$CLASSES_DIR/$class_dir" ]; then
        mkdir -p "$temp_dir/$class_dir"
        find "$CLASSES_DIR/$class_dir" -maxdepth 1 \( -name "$class_base.class" -o -name "$class_base\$*.class" \) -exec cp {} "$temp_dir/$class_dir/" \;
      fi
    done

    find "$module_dir/src" -type f ! -name '*.java' ! -path '*/.*/*' | while read -r resource; do
      relative_path=${resource#"$module_dir/src/"}
      mkdir -p "$temp_dir/$(dirname "$relative_path")"
      cp "$resource" "$temp_dir/$relative_path"
    done
  fi

  for resource_name in index.html log4j.properties castor.properties; do
    if [ -f "$module_dir/$resource_name" ]; then
      cp "$module_dir/$resource_name" "$temp_dir/"
    fi
  done

  find "$module_dir" -maxdepth 1 -type f -name '*.jar' | while read -r embedded_jar; do
    cp "$embedded_jar" "$temp_dir/"
  done

  for resource_dir in WEB-INF resources; do
    if [ -d "$module_dir/$resource_dir" ]; then
      cp -R "$module_dir/$resource_dir" "$temp_dir/"
    fi
  done

  rm -f "$output_jar"
  jar cfm "$output_jar" "$manifest_file" -C "$temp_dir" . >/dev/null
  ln -sfn "$(basename "$output_jar")" "$PLUGINS_DIR/$module"
  echo "Created $output_jar"
done

echo "Driver plugin export complete"
if [ -n "$skipped_modules" ]; then
  echo "Skipped modules because required third-party dependencies are not available in this checkout:$skipped_modules"
fi