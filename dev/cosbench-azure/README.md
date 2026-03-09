# COSBench Azure Blob Adaptor

This adaptor adds Azure Blob Storage support to COSBench through a standalone OSGi storage plugin. It is designed to minimize impact on the legacy codebase: the existing storage flow is unchanged, and Azure support is isolated in a new module.

## Runtime Requirement

This adaptor is currently built as Java 8 bytecode for compatibility with modern JDK tooling. The generated jar is architecture-neutral and can run on both arm64 and x86 hosts, but the target runtime must provide Java 8 or newer.

## Build

On macOS or Linux:

```bash
./dev/cosbench-azure/build-azure-adaptor.sh
```

This produces the bundle jar under `dist/osgi/plugins/`.

To export the broader driver plugin set from this source checkout into `dist/osgi/plugins/`, run:

```bash
./dev/cosbench-azure/export-driver-plugin-set.sh
```

This script compiles the driver-related COSBench modules in dependency order, uses any module-local embedded jars declared by the legacy adaptors, and packages the plugin jars expected by the driver runtime.

## Minimal OSGi Validation

On macOS or Linux, you can verify that the Azure bundle resolves inside a minimal Equinox runtime with:

```bash
./dev/cosbench-azure/validate-azure-bundle.sh
```

This is not a full COSBench driver boot. It is a focused validation that packages the minimum COSBench dependency bundles needed for the Azure adaptor and checks that `cosbench-azure` appears in the OSGi state output.

If the script reports `ACTIVE`, the bundle started successfully in the reduced runtime. If it reports `INSTALLED` or another non-`ACTIVE` state, that still confirms Equinox can discover the bundle, but it does not prove full activation; full activation depends on the broader COSBench plugin set used by the real driver runtime.

The current validation script is JDK 11 aware: it injects the legacy execution environment names and several Java platform packages that old Equinox and SpringSource bundles do not expose cleanly on newer JDKs. With those compatibility settings in place, the reduced validation runtime should report `cosbench-azure` as `ACTIVE`.

## Recommended JDK Choice

For Azure adaptor development, bundle compilation, and reduced OSGi validation, JDK 11 is acceptable.

For a real end-to-end COSBench controller and driver runtime, Java 8 is still the safer choice. The current blocker is not the Azure adaptor itself, but the old Equinox and Spring-OSGi runtime used by this repository.

## Full Driver Preflight

To check whether this checkout has enough exported plugin jars for a real driver boot, run:

```bash
./dev/cosbench-azure/check-driver-plugin-set.sh
```

This compares the plugin entries referenced by `release/conf/.driver/config.ini` with the actual contents of `dist/osgi/plugins/`. If it reports missing plugins, a full `release/start-driver.sh` validation is not yet possible from this checkout.

## Current Driver Runtime Status

From this checkout, the plugin export and driver preflight steps can now be completed successfully.

In this repository, the current startup scripts now include the compatibility flags needed to boot the legacy Equinox and Spring-OSGi runtime on JDK 11 for both driver and controller. The Azure adaptor has been validated in the reduced OSGi runtime and can also run in the full local startup flow from this source checkout. Java 8 is still the more conservative choice for long-term use of this legacy stack, but JDK 11 is now supported by the repository scripts.

## Supported Authentication Modes

1. `storage-key`
Uses Azure Storage Shared Key signing for all Blob REST requests.

2. `service-principal`
Uses Microsoft Entra client credentials to obtain a bearer token for `https://storage.azure.com/`.

3. `managed-identity`
Uses Azure Managed Identity token endpoints.
Works with IMDS by default and also supports environments that expose `MSI_ENDPOINT` and `MSI_SECRET`.

4. `azure-cli`
Uses the locally cached Azure CLI login context by running `az account get-access-token` and reuses that bearer token for Blob REST requests.
This mode is intended for developer workstations or jump hosts where `az login` has already been completed.

## Environment Variable Support

The adaptor now supports reading required settings from environment variables when the workload `config` does not provide them. The resolution order is:

1. Value explicitly set in workload `config`
2. Supported environment variable
3. Built-in default value, if the field has one

Supported environment variables:

| Purpose | Preferred variable | Compatibility variable |
| --- | --- | --- |
| Account name | `COSBENCH_AZURE_ACCOUNT` | `AZURE_STORAGE_ACCOUNT` |
| Blob endpoint | `COSBENCH_AZURE_ENDPOINT` | - |
| Protocol | `COSBENCH_AZURE_PROTOCOL` | - |
| Endpoint suffix | `COSBENCH_AZURE_ENDPOINT_SUFFIX` | - |
| Storage key | `COSBENCH_AZURE_STORAGE_KEY` | `AZURE_STORAGE_KEY` |
| Tenant ID | `AZURE_TENANT_ID` | `COSBENCH_AZURE_TENANT_ID` |
| Client ID | `AZURE_CLIENT_ID` | `COSBENCH_AZURE_CLIENT_ID` |
| Client secret | `AZURE_CLIENT_SECRET` | `COSBENCH_AZURE_CLIENT_SECRET` |
| AAD endpoint | `COSBENCH_AZURE_AAD_ENDPOINT` | `AZURE_AUTHORITY_HOST` |
| Token resource | `COSBENCH_AZURE_TOKEN_RESOURCE` | - |
| MSI endpoint | `MSI_ENDPOINT` | - |
| MSI secret | `MSI_SECRET` | - |
| MSI API version | `COSBENCH_AZURE_MSI_API_VERSION` | - |
| Blob API version | `COSBENCH_AZURE_API_VERSION` | - |

Example using environment variables instead of embedding secrets in workload XML:

```bash
export COSBENCH_AZURE_ACCOUNT=myaccount
export AZURE_TENANT_ID=<tenant-id>
export AZURE_CLIENT_ID=<client-id>
export AZURE_CLIENT_SECRET=<client-secret>
```

Then the workload can stay minimal:

```xml
<storage type="azure" config="auth_type=service-principal" />
```

## Quick Test Runbook

The steps below assume you are at the repository root.

### 1. Build and preflight

```bash
./dev/cosbench-azure/build-azure-adaptor.sh
./dev/cosbench-azure/export-driver-plugin-set.sh
./dev/cosbench-azure/check-driver-plugin-set.sh
./dev/cosbench-azure/validate-azure-bundle.sh
```

### 2. Start services

```bash
cd release
./start-driver.sh
./start-controller.sh
```

Open the controller UI at `http://127.0.0.1:19088/controller`.

### 3. Submit workload by CLI

```bash
./submit-azure-workload.sh --auth-type managed-identity --account myaccount
cd release && ./cli.sh info
```

### 4. Storage key example

```bash
export COSBENCH_AZURE_ACCOUNT=myaccount
export COSBENCH_AZURE_STORAGE_KEY='your-storage-key'

./submit-azure-workload.sh --auth-type storage-key --account myaccount --storage-key 'your-storage-key'
```

### 5. Service principal example

```bash
export COSBENCH_AZURE_ACCOUNT=myaccount
export AZURE_TENANT_ID='your-tenant-id'
export AZURE_CLIENT_ID='your-client-id'
export AZURE_CLIENT_SECRET='your-client-secret'

./submit-azure-workload.sh \
  --auth-type service-principal \
  --account myaccount \
  --tenant-id 'your-tenant-id' \
  --client-id 'your-client-id' \
  --client-secret 'your-client-secret'
```

### 6. Managed identity example

```bash
export COSBENCH_AZURE_ACCOUNT=myaccount

./submit-azure-workload.sh --auth-type managed-identity --account myaccount
```

If you use a user-assigned managed identity, keep the same environment variables and add `clientid=<managed-identity-client-id>` to the workload `config`.

### 7. Azure CLI cached login example

```bash
az login
export COSBENCH_AZURE_ACCOUNT=myaccount

./submit-azure-workload.sh --auth-type azure-cli --account myaccount
```

If you use a non-default cloud or a custom Azure CLI profile location, keep that configured in the shell environment before starting COSBench.

The repository root helper selects the auth-specific sample template automatically, injects the storage config into a temporary workload XML, submits it through `release/cli.sh`, and then removes the generated file unless `--keep-generated` is specified.

The older helper name `submit-azure-cli-workload.sh` is still available as a compatibility wrapper, but `submit-azure-workload.sh` is now the primary entry point.

### 8. Check logs and results

Boot logs are written under `release/log/`.

The runtime system log is written to `log/system.log`.

Archived workload results are written under `archive/`.

If you are debugging an environment issue and want the most conservative runtime choice for this legacy stack, use JDK 8. For this repository checkout, the provided startup scripts are already adjusted to support JDK 11 as well.

## Supported COSBench Operations

- Create container
- Delete container
- Upload object as Block Blob
- Download object
- Delete object
- List blobs in a container

## Configuration Keys

Common keys:

| Key | Required | Default | Description |
| --- | --- | --- | --- |
| `auth_type` | No | `storage-key` | `storage-key`, `service-principal`, `managed-identity`, or `azure-cli` |
| `account` | Yes, unless derivable from `endpoint` | empty | Azure storage account name |
| `endpoint` | No | derived | Full Blob endpoint, such as `https://myacct.blob.core.windows.net` |
| `protocol` | No | `https` | Used only when `endpoint` is not provided |
| `endpoint_suffix` | No | `blob.core.windows.net` | Used only when `endpoint` is not provided |
| `timeout` | No | `30000` | Connection and socket timeout in milliseconds |
| `api_version` | No | `2021-12-02` | Blob REST API version sent in `x-ms-version` |

Storage key mode:

| Key | Required | Description |
| --- | --- | --- |
| `storagekey` | Yes | Account access key |

If omitted from `config`, the adaptor will try `COSBENCH_AZURE_STORAGE_KEY`, then `AZURE_STORAGE_KEY`.

Service principal mode:

| Key | Required | Default | Description |
| --- | --- | --- | --- |
| `tenantid` | Yes | empty | Entra tenant ID |
| `clientid` | Yes | empty | Application or service principal client ID |
| `clientsecret` | Yes | empty | Client secret |
| `aad_endpoint` | No | `https://login.microsoftonline.com` | Login endpoint, useful for sovereign clouds |
| `token_resource` | No | `https://storage.azure.com/` | OAuth resource |

If omitted from `config`, the adaptor will try `AZURE_TENANT_ID`, `AZURE_CLIENT_ID`, `AZURE_CLIENT_SECRET`, and `AZURE_AUTHORITY_HOST`.

Managed identity mode:

| Key | Required | Default | Description |
| --- | --- | --- | --- |
| `clientid` | No | empty | Optional user-assigned managed identity client ID |
| `msi_endpoint` | No | `http://169.254.169.254/metadata/identity/oauth2/token` | Token endpoint override |
| `msi_api_version` | No | `2018-02-01` | Managed identity API version |
| `token_resource` | No | `https://storage.azure.com/` | OAuth resource |

`account` can also be omitted from `config` if `COSBENCH_AZURE_ACCOUNT` or `AZURE_STORAGE_ACCOUNT` is set.

Azure CLI mode:

| Key | Required | Default | Description |
| --- | --- | --- | --- |
| `token_resource` | No | `https://storage.azure.com/` | OAuth resource passed to `az account get-access-token --resource` |

Before using this mode, run `az login` in the same user context that starts COSBench. The adaptor shells out to `az`, so the Azure CLI must be installed and available in `PATH`.

## Workload Examples

Storage key:

```xml
<storage type="azure" config="auth_type=storage-key" />
```

Service principal:

```xml
<storage type="azure" config="auth_type=service-principal" />
```

Managed identity:

```xml
<storage type="azure" config="auth_type=managed-identity" />
```

Azure CLI cached login:

```xml
<storage type="azure" config="auth_type=azure-cli" />
```

Managed identity with user-assigned identity:

```xml
<storage type="azure" config="auth_type=managed-identity;account=<account>;clientid=<user-assigned-client-id>" />
```

Azure China or other sovereign cloud endpoint example:

```xml
<storage type="azure" config="auth_type=service-principal;account=<account>;tenantid=<tenant>;clientid=<client-id>;clientsecret=<client-secret>;endpoint=https://<account>.blob.core.chinacloudapi.cn;aad_endpoint=https://login.chinacloudapi.cn" />
```

Full sample:

```xml
<workload name="azure-blob-sample" description="sample benchmark for Azure Blob Storage">
  <storage type="azure" config="auth_type=storage-key;account=<account>;storagekey=<key>" />

  <workflow>
    <workstage name="init">
      <work type="init" workers="1" config="cprefix=aztest;containers=r(1,2)" />
    </workstage>

    <workstage name="prepare">
      <work type="prepare" workers="1" config="cprefix=aztest;containers=r(1,2);objects=r(1,10);sizes=c(64)KB" />
    </workstage>

    <workstage name="main">
      <work name="main" workers="10" runtime="300">
        <operation type="read" ratio="80" config="cprefix=aztest;containers=u(1,2);objects=u(1,10)" />
        <operation type="write" ratio="20" config="cprefix=aztest;containers=u(1,2);objects=u(11,20);sizes=c(64)KB" />
      </work>
    </workstage>

    <workstage name="cleanup">
      <work type="cleanup" workers="1" config="cprefix=aztest;containers=r(1,2);objects=r(1,20)" />
    </workstage>

    <workstage name="dispose">
      <work type="dispose" workers="1" config="cprefix=aztest;containers=r(1,2)" />
    </workstage>
  </workflow>
</workload>
```

## Notes

- The adaptor intentionally uses Azure Blob REST APIs instead of the modern Azure Java SDK to remain compatible with this legacy Java and OSGi build.
- The generated jar is architecture-independent Java bytecode, so building on macOS arm64 does not prevent running it on x86.
- Secrets are masked in adaptor parameter logging, but benchmark configuration files still need to be handled carefully.
- For managed identity in App Service or similar hosts, ensure `MSI_ENDPOINT` and `MSI_SECRET` are available to the COSBench driver process when required.
- For user-assigned managed identity, pass `clientid=<managed-identity-client-id>` in the storage config.
- For sovereign clouds, override both `endpoint` and `aad_endpoint` together to keep Blob data plane and token issuer aligned.