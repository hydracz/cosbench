package com.intel.cosbench.client.azure;

public interface AzureStorageConstants {

	String CONN_TIMEOUT_KEY = "timeout";
	int CONN_TIMEOUT_DEFAULT = 30000;

	String ACCOUNT_NAME_KEY = "account";
	String ACCOUNT_NAME_DEFAULT = "";

	String ENDPOINT_KEY = "endpoint";
	String ENDPOINT_DEFAULT = "";

	String PROTOCOL_KEY = "protocol";
	String PROTOCOL_DEFAULT = "https";

	String ENDPOINT_SUFFIX_KEY = "endpoint_suffix";
	String ENDPOINT_SUFFIX_DEFAULT = "blob.core.windows.net";

	String AUTH_TYPE_KEY = "auth_type";
	String AUTH_TYPE_DEFAULT = "storage-key";

	String STORAGE_KEY_KEY = "storagekey";
	String STORAGE_KEY_DEFAULT = "";

	String TENANT_ID_KEY = "tenantid";
	String TENANT_ID_DEFAULT = "";

	String CLIENT_ID_KEY = "clientid";
	String CLIENT_ID_DEFAULT = "";

	String CLIENT_SECRET_KEY = "clientsecret";
	String CLIENT_SECRET_DEFAULT = "";

	String AAD_ENDPOINT_KEY = "aad_endpoint";
	String AAD_ENDPOINT_DEFAULT = "https://login.microsoftonline.com";

	String TOKEN_RESOURCE_KEY = "token_resource";
	String TOKEN_RESOURCE_DEFAULT = "https://storage.azure.com/";

	String MSI_ENDPOINT_KEY = "msi_endpoint";
	String MSI_ENDPOINT_DEFAULT = "http://169.254.169.254/metadata/identity/oauth2/token";

	String MSI_API_VERSION_KEY = "msi_api_version";
	String MSI_API_VERSION_DEFAULT = "2018-02-01";

	String API_VERSION_KEY = "api_version";
	String API_VERSION_DEFAULT = "2021-12-02";

	String AUTH_TYPE_STORAGE_KEY = "storage-key";
	String AUTH_TYPE_SERVICE_PRINCIPAL = "service-principal";
	String AUTH_TYPE_MANAGED_IDENTITY = "managed-identity";
	String AUTH_TYPE_AZURE_CLI = "azure-cli";

	String ENV_ACCOUNT_NAME = "COSBENCH_AZURE_ACCOUNT";
	String ENV_ACCOUNT_NAME_COMPAT = "AZURE_STORAGE_ACCOUNT";
	String ENV_ENDPOINT = "COSBENCH_AZURE_ENDPOINT";
	String ENV_PROTOCOL = "COSBENCH_AZURE_PROTOCOL";
	String ENV_ENDPOINT_SUFFIX = "COSBENCH_AZURE_ENDPOINT_SUFFIX";
	String ENV_STORAGE_KEY = "COSBENCH_AZURE_STORAGE_KEY";
	String ENV_STORAGE_KEY_COMPAT = "AZURE_STORAGE_KEY";
	String ENV_TENANT_ID = "AZURE_TENANT_ID";
	String ENV_TENANT_ID_COMPAT = "COSBENCH_AZURE_TENANT_ID";
	String ENV_CLIENT_ID = "AZURE_CLIENT_ID";
	String ENV_CLIENT_ID_COMPAT = "COSBENCH_AZURE_CLIENT_ID";
	String ENV_CLIENT_SECRET = "AZURE_CLIENT_SECRET";
	String ENV_CLIENT_SECRET_COMPAT = "COSBENCH_AZURE_CLIENT_SECRET";
	String ENV_AAD_ENDPOINT = "COSBENCH_AZURE_AAD_ENDPOINT";
	String ENV_AAD_ENDPOINT_COMPAT = "AZURE_AUTHORITY_HOST";
	String ENV_TOKEN_RESOURCE = "COSBENCH_AZURE_TOKEN_RESOURCE";
	String ENV_MSI_ENDPOINT = "MSI_ENDPOINT";
	String ENV_MSI_SECRET = "MSI_SECRET";
	String ENV_MSI_API_VERSION = "COSBENCH_AZURE_MSI_API_VERSION";
	String ENV_API_VERSION = "COSBENCH_AZURE_API_VERSION";
}