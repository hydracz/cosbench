#!/bin/bash

set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
ROOT_DIR="$SCRIPT_DIR"
GENERATED_DIR="$ROOT_DIR/release/workspace/.generated"

auth_type="azure-cli"
template=""
controller="anonymous:cosbench@127.0.0.1:19088"
account="${COSBENCH_AZURE_ACCOUNT:-}"
endpoint="${COSBENCH_AZURE_ENDPOINT:-}"
storage_key="${COSBENCH_AZURE_STORAGE_KEY:-${AZURE_STORAGE_KEY:-}}"
tenant_id="${AZURE_TENANT_ID:-${COSBENCH_AZURE_TENANT_ID:-}}"
client_id="${AZURE_CLIENT_ID:-${COSBENCH_AZURE_CLIENT_ID:-}}"
client_secret="${AZURE_CLIENT_SECRET:-${COSBENCH_AZURE_CLIENT_SECRET:-}}"
keep_generated=0
dry_run=0

default_template_for_auth() {
    case "$1" in
        storage-key)
            echo "$ROOT_DIR/release/conf/azure-config-storage-key-sample.xml"
            ;;
        service-principal)
            echo "$ROOT_DIR/release/conf/azure-config-service-principal-sample.xml"
            ;;
        managed-identity)
            echo "$ROOT_DIR/release/conf/azure-config-managed-identity-sample.xml"
            ;;
        azure-cli)
            echo "$ROOT_DIR/release/conf/azure-config-azure-cli-sample.xml"
            ;;
        *)
            echo "unsupported auth type: $1" >&2
            exit 1
            ;;
    esac
}

append_config() {
    local key="$1"
    local value="$2"
    if [ -n "$value" ]; then
        storage_config="$storage_config;$key=$value"
    fi
}

require_value() {
    local value="$1"
    local message="$2"
    if [ -z "$value" ]; then
        echo "$message" >&2
        exit 1
    fi
}

usage() {
    cat <<'EOF'
Usage:
  ./submit-azure-workload.sh [options]

Options:
  --auth-type <type>      storage-key | service-principal | managed-identity | azure-cli
                          Default: azure-cli
  --account <name>        Azure Storage account name. Defaults to COSBENCH_AZURE_ACCOUNT.
  --endpoint <url>        Full Blob endpoint. Defaults to COSBENCH_AZURE_ENDPOINT.
  --storage-key <key>     Storage key. Defaults to COSBENCH_AZURE_STORAGE_KEY / AZURE_STORAGE_KEY.
  --tenant-id <id>        Tenant ID for service-principal. Defaults to AZURE_TENANT_ID.
  --client-id <id>        Client ID for service-principal or user-assigned managed identity.
  --client-secret <sec>   Client secret for service-principal. Defaults to AZURE_CLIENT_SECRET.
  --template <path>       Workload template XML. Default depends on auth type.
  --controller <target>   CLI target in username:password@host:port form.
                          Default: anonymous:cosbench@127.0.0.1:19088
  --keep-generated        Keep the generated XML after submit.
  --dry-run               Only generate XML, do not submit.
  -h, --help              Show this help.

Examples:
  ./submit-azure-workload.sh --auth-type azure-cli --account mystorage
  ./submit-azure-workload.sh --auth-type storage-key --account mystorage --storage-key 'xxx'
  ./submit-azure-workload.sh --auth-type service-principal --account mystorage --tenant-id tid --client-id cid --client-secret sec
  ./submit-azure-workload.sh --auth-type managed-identity --account mystorage
EOF
}

while [ $# -gt 0 ]; do
    case "$1" in
        --auth-type)
            [ $# -ge 2 ] || { echo "missing value for --auth-type" >&2; exit 1; }
            auth_type="$2"
            shift 2
            ;;
        --account)
            [ $# -ge 2 ] || { echo "missing value for --account" >&2; exit 1; }
            account="$2"
            shift 2
            ;;
        --endpoint)
            [ $# -ge 2 ] || { echo "missing value for --endpoint" >&2; exit 1; }
            endpoint="$2"
            shift 2
            ;;
        --storage-key)
            [ $# -ge 2 ] || { echo "missing value for --storage-key" >&2; exit 1; }
            storage_key="$2"
            shift 2
            ;;
        --tenant-id)
            [ $# -ge 2 ] || { echo "missing value for --tenant-id" >&2; exit 1; }
            tenant_id="$2"
            shift 2
            ;;
        --client-id)
            [ $# -ge 2 ] || { echo "missing value for --client-id" >&2; exit 1; }
            client_id="$2"
            shift 2
            ;;
        --client-secret)
            [ $# -ge 2 ] || { echo "missing value for --client-secret" >&2; exit 1; }
            client_secret="$2"
            shift 2
            ;;
        --template)
            [ $# -ge 2 ] || { echo "missing value for --template" >&2; exit 1; }
            template="$2"
            shift 2
            ;;
        --controller)
            [ $# -ge 2 ] || { echo "missing value for --controller" >&2; exit 1; }
            controller="$2"
            shift 2
            ;;
        --keep-generated)
            keep_generated=1
            shift
            ;;
        --dry-run)
            dry_run=1
            shift
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            echo "unknown argument: $1" >&2
            usage >&2
            exit 1
            ;;
    esac
done

if [ -z "$template" ]; then
    template=$(default_template_for_auth "$auth_type")
fi

if [ ! -f "$template" ]; then
    echo "template not found: $template" >&2
    exit 1
fi

if [ -z "$account" ] && [ -z "$endpoint" ]; then
    echo "either --account or --endpoint must be provided" >&2
    echo "tip: export COSBENCH_AZURE_ACCOUNT before running this helper" >&2
    exit 1
fi

case "$auth_type" in
    storage-key)
        require_value "$storage_key" "storage-key auth requires --storage-key or COSBENCH_AZURE_STORAGE_KEY"
        ;;
    service-principal)
        require_value "$tenant_id" "service-principal auth requires --tenant-id or AZURE_TENANT_ID"
        require_value "$client_id" "service-principal auth requires --client-id or AZURE_CLIENT_ID"
        require_value "$client_secret" "service-principal auth requires --client-secret or AZURE_CLIENT_SECRET"
        ;;
    managed-identity)
        ;;
    azure-cli)
        ;;
    *)
        echo "unsupported auth type: $auth_type" >&2
        exit 1
        ;;
esac

mkdir -p "$GENERATED_DIR"
generated="$GENERATED_DIR/${auth_type}-$(date +%Y%m%d-%H%M%S).xml"

storage_config="auth_type=$auth_type"
append_config "account" "$account"
append_config "endpoint" "$endpoint"

case "$auth_type" in
    storage-key)
        append_config "storagekey" "$storage_key"
        ;;
    service-principal)
        append_config "tenantid" "$tenant_id"
        append_config "clientid" "$client_id"
        append_config "clientsecret" "$client_secret"
        ;;
    managed-identity)
        append_config "clientid" "$client_id"
        ;;
    azure-cli)
        ;;
esac

awk -v storage_config="$storage_config" '
    BEGIN { updated = 0 }
    /<storage type="azure" config="/ && updated == 0 {
        sub(/config="[^"]*"/, "config=\"" storage_config "\"")
        updated = 1
    }
    { print }
    END {
        if (updated == 0) {
            exit 2
        }
    }
' "$template" > "$generated" || {
    echo "failed to inject Azure storage config into template: $template" >&2
    rm -f "$generated"
    exit 1
}

echo "Generated workload: $generated"
echo "Injected storage config: $storage_config"
echo "Using template: $template"

if [ "$dry_run" -eq 1 ]; then
    echo "Dry run enabled, skipping submit."
    exit 0
fi

"$ROOT_DIR/release/cli.sh" submit "$generated" "$controller"

if [ "$keep_generated" -eq 0 ]; then
    rm -f "$generated"
    echo "Removed generated workload file."
else
    echo "Kept generated workload file: $generated"
fi