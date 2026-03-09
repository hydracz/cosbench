#!/bin/bash

set -eu

ROOT_DIR=$(cd "$(dirname "$0")/../.." && pwd)
PLUGINS_DIR="$ROOT_DIR/dist/osgi/plugins"
DRIVER_CONFIG="$ROOT_DIR/release/conf/.driver/config.ini"

if [ ! -d "$PLUGINS_DIR" ]; then
  echo "Plugin directory does not exist: $PLUGINS_DIR" >&2
  exit 1
fi

if [ ! -f "$DRIVER_CONFIG" ]; then
  echo "Driver OSGi config does not exist: $DRIVER_CONFIG" >&2
  exit 1
fi

echo "Checking driver plugin prerequisites in $PLUGINS_DIR"

required_plugins=$(sed -n 's/.*plugins\/\([^@,\\]*\).*/\1/p' "$DRIVER_CONFIG" | sort -u)

missing_count=0
present_count=0

for plugin in $required_plugins; do
  if find "$PLUGINS_DIR" -maxdepth 1 \( -name "$plugin" -o -name "$plugin.jar" -o -name "${plugin}_*.jar" \) | grep . >/dev/null; then
    present_count=$((present_count + 1))
    echo "PRESENT  $plugin"
  else
    missing_count=$((missing_count + 1))
    echo "MISSING  $plugin"
  fi
done

echo
echo "Present: $present_count"
echo "Missing: $missing_count"

if [ "$missing_count" -ne 0 ]; then
  echo "The full COSBench driver startup cannot be validated from this checkout until the missing plugin set is exported to dist/osgi/plugins." >&2
  exit 2
fi

echo "All driver plugin entries referenced by release/conf/.driver/config.ini are present."