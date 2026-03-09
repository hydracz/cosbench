package com.intel.cosbench.api.azure;

import static com.intel.cosbench.client.azure.AzureStorageConstants.AAD_ENDPOINT_DEFAULT;
import static com.intel.cosbench.client.azure.AzureStorageConstants.AAD_ENDPOINT_KEY;
import static com.intel.cosbench.client.azure.AzureStorageConstants.ACCOUNT_NAME_DEFAULT;
import static com.intel.cosbench.client.azure.AzureStorageConstants.ACCOUNT_NAME_KEY;
import static com.intel.cosbench.client.azure.AzureStorageConstants.API_VERSION_DEFAULT;
import static com.intel.cosbench.client.azure.AzureStorageConstants.API_VERSION_KEY;
import static com.intel.cosbench.client.azure.AzureStorageConstants.AUTH_TYPE_AZURE_CLI;
import static com.intel.cosbench.client.azure.AzureStorageConstants.AUTH_TYPE_DEFAULT;
import static com.intel.cosbench.client.azure.AzureStorageConstants.AUTH_TYPE_KEY;
import static com.intel.cosbench.client.azure.AzureStorageConstants.AUTH_TYPE_MANAGED_IDENTITY;
import static com.intel.cosbench.client.azure.AzureStorageConstants.AUTH_TYPE_SERVICE_PRINCIPAL;
import static com.intel.cosbench.client.azure.AzureStorageConstants.AUTH_TYPE_STORAGE_KEY;
import static com.intel.cosbench.client.azure.AzureStorageConstants.CLIENT_ID_DEFAULT;
import static com.intel.cosbench.client.azure.AzureStorageConstants.CLIENT_ID_KEY;
import static com.intel.cosbench.client.azure.AzureStorageConstants.CLIENT_SECRET_DEFAULT;
import static com.intel.cosbench.client.azure.AzureStorageConstants.CLIENT_SECRET_KEY;
import static com.intel.cosbench.client.azure.AzureStorageConstants.CONN_TIMEOUT_DEFAULT;
import static com.intel.cosbench.client.azure.AzureStorageConstants.CONN_TIMEOUT_KEY;
import static com.intel.cosbench.client.azure.AzureStorageConstants.ENDPOINT_DEFAULT;
import static com.intel.cosbench.client.azure.AzureStorageConstants.ENDPOINT_KEY;
import static com.intel.cosbench.client.azure.AzureStorageConstants.ENDPOINT_SUFFIX_DEFAULT;
import static com.intel.cosbench.client.azure.AzureStorageConstants.ENDPOINT_SUFFIX_KEY;
import static com.intel.cosbench.client.azure.AzureStorageConstants.ENV_AAD_ENDPOINT;
import static com.intel.cosbench.client.azure.AzureStorageConstants.ENV_AAD_ENDPOINT_COMPAT;
import static com.intel.cosbench.client.azure.AzureStorageConstants.ENV_ACCOUNT_NAME;
import static com.intel.cosbench.client.azure.AzureStorageConstants.ENV_ACCOUNT_NAME_COMPAT;
import static com.intel.cosbench.client.azure.AzureStorageConstants.ENV_API_VERSION;
import static com.intel.cosbench.client.azure.AzureStorageConstants.ENV_CLIENT_ID;
import static com.intel.cosbench.client.azure.AzureStorageConstants.ENV_CLIENT_ID_COMPAT;
import static com.intel.cosbench.client.azure.AzureStorageConstants.ENV_CLIENT_SECRET;
import static com.intel.cosbench.client.azure.AzureStorageConstants.ENV_CLIENT_SECRET_COMPAT;
import static com.intel.cosbench.client.azure.AzureStorageConstants.ENV_ENDPOINT;
import static com.intel.cosbench.client.azure.AzureStorageConstants.ENV_ENDPOINT_SUFFIX;
import static com.intel.cosbench.client.azure.AzureStorageConstants.ENV_MSI_API_VERSION;
import static com.intel.cosbench.client.azure.AzureStorageConstants.ENV_MSI_ENDPOINT;
import static com.intel.cosbench.client.azure.AzureStorageConstants.ENV_MSI_SECRET;
import static com.intel.cosbench.client.azure.AzureStorageConstants.ENV_PROTOCOL;
import static com.intel.cosbench.client.azure.AzureStorageConstants.ENV_STORAGE_KEY;
import static com.intel.cosbench.client.azure.AzureStorageConstants.ENV_STORAGE_KEY_COMPAT;
import static com.intel.cosbench.client.azure.AzureStorageConstants.ENV_TENANT_ID;
import static com.intel.cosbench.client.azure.AzureStorageConstants.ENV_TENANT_ID_COMPAT;
import static com.intel.cosbench.client.azure.AzureStorageConstants.ENV_TOKEN_RESOURCE;
import static com.intel.cosbench.client.azure.AzureStorageConstants.MSI_API_VERSION_DEFAULT;
import static com.intel.cosbench.client.azure.AzureStorageConstants.MSI_API_VERSION_KEY;
import static com.intel.cosbench.client.azure.AzureStorageConstants.MSI_ENDPOINT_DEFAULT;
import static com.intel.cosbench.client.azure.AzureStorageConstants.MSI_ENDPOINT_KEY;
import static com.intel.cosbench.client.azure.AzureStorageConstants.PROTOCOL_DEFAULT;
import static com.intel.cosbench.client.azure.AzureStorageConstants.PROTOCOL_KEY;
import static com.intel.cosbench.client.azure.AzureStorageConstants.STORAGE_KEY_DEFAULT;
import static com.intel.cosbench.client.azure.AzureStorageConstants.STORAGE_KEY_KEY;
import static com.intel.cosbench.client.azure.AzureStorageConstants.TENANT_ID_DEFAULT;
import static com.intel.cosbench.client.azure.AzureStorageConstants.TENANT_ID_KEY;
import static com.intel.cosbench.client.azure.AzureStorageConstants.TOKEN_RESOURCE_DEFAULT;
import static com.intel.cosbench.client.azure.AzureStorageConstants.TOKEN_RESOURCE_KEY;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;

import com.intel.cosbench.api.context.AuthContext;
import com.intel.cosbench.api.storage.NoneStorage;
import com.intel.cosbench.api.storage.StorageException;
import com.intel.cosbench.client.http.HttpClientUtil;
import com.intel.cosbench.config.Config;
import com.intel.cosbench.log.Logger;

public class AzureStorage extends NoneStorage {

	private static final Pattern JSON_STRING_PATTERN = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"([^\"]*)\"");
	private static final long TOKEN_REFRESH_SKEW_MILLIS = 60000L;
	private static final long AZ_CLI_TIMEOUT_MILLIS = 30000L;

	private int timeout;
	private String accountName;
	private String endpoint;
	private String authType;
	private String storageKey;
	private String tenantId;
	private String clientId;
	private String clientSecret;
	private String aadEndpoint;
	private String tokenResource;
	private String managedIdentityEndpoint;
	private String managedIdentityApiVersion;
	private String managedIdentitySecret;
	private String apiVersion;

	private HttpClient client;
	private volatile HttpRequestBase currentRequest;
	private volatile AccessToken cachedToken;

	@Override
	public void init(Config config, Logger logger) {
		super.init(config, logger);
		initParms(config);
		client = HttpClientUtil.createHttpClient(timeout);
		logger.debug("Azure Blob client has been initialized for endpoint {}", endpoint);
	}

	private void initParms(Config config) {
		timeout = config.getInt(CONN_TIMEOUT_KEY, CONN_TIMEOUT_DEFAULT);
		authType = normalizeAuthType(getConfigOrDefault(config, AUTH_TYPE_KEY, AUTH_TYPE_DEFAULT));
		accountName = getConfigOrEnv(config, ACCOUNT_NAME_KEY, ACCOUNT_NAME_DEFAULT, ENV_ACCOUNT_NAME, ENV_ACCOUNT_NAME_COMPAT);
		endpoint = normalizeEndpoint(config);
		storageKey = getConfigOrEnv(config, STORAGE_KEY_KEY, STORAGE_KEY_DEFAULT, ENV_STORAGE_KEY, ENV_STORAGE_KEY_COMPAT);
		tenantId = getConfigOrEnv(config, TENANT_ID_KEY, TENANT_ID_DEFAULT, ENV_TENANT_ID, ENV_TENANT_ID_COMPAT);
		clientId = getConfigOrEnv(config, CLIENT_ID_KEY, CLIENT_ID_DEFAULT, ENV_CLIENT_ID, ENV_CLIENT_ID_COMPAT);
		clientSecret = getConfigOrEnv(config, CLIENT_SECRET_KEY, CLIENT_SECRET_DEFAULT, ENV_CLIENT_SECRET, ENV_CLIENT_SECRET_COMPAT);
		aadEndpoint = trimTrailingSlash(getConfigOrEnv(config, AAD_ENDPOINT_KEY, AAD_ENDPOINT_DEFAULT, ENV_AAD_ENDPOINT, ENV_AAD_ENDPOINT_COMPAT));
		tokenResource = getConfigOrEnv(config, TOKEN_RESOURCE_KEY, TOKEN_RESOURCE_DEFAULT, ENV_TOKEN_RESOURCE);
		managedIdentityEndpoint = getConfigOrEnv(config, MSI_ENDPOINT_KEY, MSI_ENDPOINT_DEFAULT, ENV_MSI_ENDPOINT);
		managedIdentityApiVersion = getConfigOrEnv(config, MSI_API_VERSION_KEY, MSI_API_VERSION_DEFAULT, ENV_MSI_API_VERSION);
		managedIdentitySecret = getEnvOrDefault(ENV_MSI_SECRET, "");
		apiVersion = getConfigOrEnv(config, API_VERSION_KEY, API_VERSION_DEFAULT, ENV_API_VERSION);

		validateConfiguration();

		parms.put(CONN_TIMEOUT_KEY, timeout);
		parms.put(AUTH_TYPE_KEY, authType);
		parms.put(ACCOUNT_NAME_KEY, accountName);
		parms.put(ENDPOINT_KEY, endpoint);
		parms.put(API_VERSION_KEY, apiVersion);
		parms.put(TOKEN_RESOURCE_KEY, tokenResource);
		if (storageKey.length() > 0) {
			parms.put(STORAGE_KEY_KEY, mask(storageKey));
		}
		if (tenantId.length() > 0) {
			parms.put(TENANT_ID_KEY, tenantId);
		}
		if (clientId.length() > 0) {
			parms.put(CLIENT_ID_KEY, clientId);
		}
		if (clientSecret.length() > 0) {
			parms.put(CLIENT_SECRET_KEY, mask(clientSecret));
		}
		if (managedIdentityEndpoint.length() > 0) {
			parms.put(MSI_ENDPOINT_KEY, managedIdentityEndpoint);
		}
		parms.put(MSI_API_VERSION_KEY, managedIdentityApiVersion);

		logger.debug("using storage config: {}", parms);
	}

	private String normalizeEndpoint(Config config) {
		String configuredEndpoint = trimTrailingSlash(getConfigOrEnv(config, ENDPOINT_KEY, ENDPOINT_DEFAULT, ENV_ENDPOINT));
		if (configuredEndpoint.length() > 0) {
			if (accountName.length() == 0) {
				accountName = deriveAccountName(configuredEndpoint);
			}
			return configuredEndpoint;
		}

		String protocol = getConfigOrEnv(config, PROTOCOL_KEY, PROTOCOL_DEFAULT, ENV_PROTOCOL);
		String suffix = getConfigOrEnv(config, ENDPOINT_SUFFIX_KEY, ENDPOINT_SUFFIX_DEFAULT, ENV_ENDPOINT_SUFFIX);
		if (accountName.length() == 0) {
			throw new StorageException("azure adaptor requires either endpoint or account");
		}
		return trimTrailingSlash(protocol + "://" + accountName + "." + suffix);
	}

	private void validateConfiguration() {
		if (accountName.length() == 0) {
			throw new StorageException("azure adaptor requires account to be specified or derivable from endpoint");
		}
		if (AUTH_TYPE_STORAGE_KEY.equals(authType)) {
			assertNotBlank(storageKey, "storagekey is required when auth_type=storage-key");
			return;
		}
		if (AUTH_TYPE_SERVICE_PRINCIPAL.equals(authType)) {
			assertNotBlank(tenantId, "tenantid is required when auth_type=service-principal");
			assertNotBlank(clientId, "clientid is required when auth_type=service-principal");
			assertNotBlank(clientSecret, "clientsecret is required when auth_type=service-principal");
			return;
		}
		if (AUTH_TYPE_MANAGED_IDENTITY.equals(authType)) {
			assertNotBlank(managedIdentityEndpoint, "managed identity endpoint is required when auth_type=managed-identity");
			return;
		}
		if (AUTH_TYPE_AZURE_CLI.equals(authType)) {
			return;
		}
		throw new StorageException("unsupported azure auth_type: " + authType);
	}

	private void assertNotBlank(String value, String message) {
		if (value == null || value.length() == 0) {
			throw new StorageException(message);
		}
	}

	private String deriveAccountName(String configuredEndpoint) {
		try {
			URI uri = new URI(configuredEndpoint);
			String host = uri.getHost();
			if (host == null || host.length() == 0) {
				return "";
			}
			int dotIndex = host.indexOf('.');
			return dotIndex > 0 ? host.substring(0, dotIndex) : host;
		} catch (URISyntaxException e) {
			throw new StorageException(e);
		}
	}

	@Override
	public void setAuthContext(AuthContext info) {
		super.setAuthContext(info);
	}

	@Override
	public void dispose() {
		abort();
		HttpClientUtil.disposeHttpClient(client);
		client = null;
		cachedToken = null;
		super.dispose();
	}

	@Override
	public void abort() {
		HttpRequestBase request = currentRequest;
		if (request != null) {
			request.abort();
		}
	}

	@Override
	public InputStream getObject(String container, String object, Config config) {
		super.getObject(container, object, config);
		HttpGet request = HttpClientUtil.makeHttpGet(buildBlobUrl(container, object, null));
		prepareRequest(request, buildBlobUrl(container, object, null), 0L);
		HttpResponse response = execute(request);
		ensureStatus(response, new int[] { HttpStatus.SC_OK }, "GET", container, object);
		return entityStream(response);
	}

	@Override
	public InputStream getList(String container, String object, Config config) {
		super.getList(container, object, config);
		String url = buildContainerUrl(container, "restype=container&comp=list");
		HttpGet request = HttpClientUtil.makeHttpGet(url);
		prepareRequest(request, url, 0L);
		HttpResponse response = execute(request);
		ensureStatus(response, new int[] { HttpStatus.SC_OK }, "LIST", container, object);
		return entityStream(response);
	}

	@Override
	public void createContainer(String container, Config config) {
		super.createContainer(container, config);
		String url = buildContainerUrl(container, "restype=container");
		HttpPut request = HttpClientUtil.makeHttpPut(url);
		prepareRequest(request, url, 0L);
		HttpResponse response = execute(request);
		ensureStatus(response, new int[] { HttpStatus.SC_CREATED, HttpStatus.SC_CONFLICT }, "PUT", container, null);
		consumeQuietly(response.getEntity());
	}

	@Override
	public void createObject(String container, String object, InputStream data, long length, Config config) {
		super.createObject(container, object, data, length, config);
		String url = buildBlobUrl(container, object, null);
		HttpPut request = HttpClientUtil.makeHttpPut(url);
		InputStreamEntity entity = new InputStreamEntity(data, length);
		entity.setContentType("application/octet-stream");
		request.setEntity(entity);
		request.setHeader("x-ms-blob-type", "BlockBlob");
		prepareRequest(request, url, length);
		HttpResponse response = execute(request);
		ensureStatus(response, new int[] { HttpStatus.SC_CREATED }, "PUT", container, object);
		consumeQuietly(response.getEntity());
	}

	@Override
	public void deleteContainer(String container, Config config) {
		super.deleteContainer(container, config);
		String url = buildContainerUrl(container, "restype=container");
		HttpDelete request = HttpClientUtil.makeHttpDelete(url);
		prepareRequest(request, url, 0L);
		HttpResponse response = execute(request);
		ensureStatus(response, new int[] { HttpStatus.SC_ACCEPTED, HttpStatus.SC_NOT_FOUND }, "DELETE", container, null);
		consumeQuietly(response.getEntity());
	}

	@Override
	public void deleteObject(String container, String object, Config config) {
		super.deleteObject(container, object, config);
		String url = buildBlobUrl(container, object, null);
		HttpDelete request = HttpClientUtil.makeHttpDelete(url);
		prepareRequest(request, url, 0L);
		HttpResponse response = execute(request);
		ensureStatus(response, new int[] { HttpStatus.SC_ACCEPTED, HttpStatus.SC_NOT_FOUND }, "DELETE", container, object);
		consumeQuietly(response.getEntity());
	}

	private void prepareRequest(HttpRequestBase request, String url, long contentLength) {
		String requestTime = formatDate(new Date());
		request.setHeader("x-ms-date", requestTime);
		request.setHeader("x-ms-version", apiVersion);
		request.setHeader("Accept", "application/xml");

		if (AUTH_TYPE_STORAGE_KEY.equals(authType)) {
			request.setHeader("Authorization", "SharedKey " + accountName + ":" + signRequest(request, url, contentLength));
			return;
		}

		AccessToken token = ensureToken();
		request.setHeader("Authorization", token.tokenType + " " + token.accessToken);
	}

	private HttpResponse execute(HttpRequestBase request) {
		currentRequest = request;
		try {
			return client.execute(request);
		} catch (IOException e) {
			throw new StorageException(e);
		} finally {
			currentRequest = null;
		}
	}

	private InputStream entityStream(HttpResponse response) {
		try {
			HttpEntity entity = response.getEntity();
			if (entity == null) {
				return new ByteArrayInputStream(new byte[0]);
			}
			return entity.getContent();
		} catch (IOException e) {
			throw new StorageException(e);
		}
	}

	private void ensureStatus(HttpResponse response, int[] acceptedCodes, String method, String container, String object) {
		int actual = response.getStatusLine().getStatusCode();
		for (int i = 0; i < acceptedCodes.length; i++) {
			if (acceptedCodes[i] == actual) {
				return;
			}
		}

		StringBuilder message = new StringBuilder();
		message.append("azure blob ").append(method).append(" failed with status ").append(actual);
		message.append(" on /").append(container);
		if (object != null) {
			message.append('/').append(object);
		}
		String responseBody = readEntityQuietly(response.getEntity());
		if (responseBody.length() > 0) {
			message.append(": ").append(responseBody);
		}
		throw new StorageException(message.toString());
	}

	private String readEntityQuietly(HttpEntity entity) {
		if (entity == null) {
			return "";
		}
		try {
			return EntityUtils.toString(entity);
		} catch (IOException e) {
			return "";
		}
	}

	private void consumeQuietly(HttpEntity entity) {
		if (entity == null) {
			return;
		}
		try {
			EntityUtils.consume(entity);
		} catch (IOException ignored) {
			// ignore cleanup failures
		}
	}

	private synchronized AccessToken ensureToken() {
		if (cachedToken != null && !cachedToken.shouldRefresh()) {
			return cachedToken;
		}
		if (AUTH_TYPE_SERVICE_PRINCIPAL.equals(authType)) {
			cachedToken = requestServicePrincipalToken();
		} else if (AUTH_TYPE_MANAGED_IDENTITY.equals(authType)) {
			cachedToken = requestManagedIdentityToken();
		} else if (AUTH_TYPE_AZURE_CLI.equals(authType)) {
			cachedToken = requestAzureCliToken();
		} else {
			throw new StorageException("unsupported azure token auth_type: " + authType);
		}
		return cachedToken;
	}

	private AccessToken requestServicePrincipalToken() {
		String url = aadEndpoint + "/" + tenantId + "/oauth2/token";
		HttpPost request = HttpClientUtil.makeHttpPost(url);
		request.setHeader("Content-Type", "application/x-www-form-urlencoded");
		String body = "grant_type=client_credentials"
				+ "&client_id=" + HttpClientUtil.encodeURL(clientId)
				+ "&client_secret=" + HttpClientUtil.encodeURL(clientSecret)
				+ "&resource=" + HttpClientUtil.encodeURL(tokenResource);
		try {
			request.setEntity(new StringEntity(body));
		} catch (UnsupportedEncodingException e) {
			throw new StorageException(e);
		}

		HttpResponse response = execute(request);
		ensureTokenStatus(response, url);
		String payload = readEntityQuietly(response.getEntity());
		return parseAccessToken(payload);
	}

	private AccessToken requestManagedIdentityToken() {
		StringBuilder url = new StringBuilder(managedIdentityEndpoint);
		if (managedIdentityEndpoint.indexOf('?') < 0) {
			url.append('?');
		} else {
			url.append('&');
		}
		url.append("api-version=").append(HttpClientUtil.encodeURL(managedIdentityApiVersion));
		url.append("&resource=").append(HttpClientUtil.encodeURL(tokenResource));
		if (clientId.length() > 0) {
			url.append("&client_id=").append(HttpClientUtil.encodeURL(clientId));
		}

		HttpGet request = HttpClientUtil.makeHttpGet(url.toString());
		request.setHeader("Metadata", "true");
		if (managedIdentitySecret.length() > 0) {
			request.setHeader("Secret", managedIdentitySecret);
		}

		HttpResponse response = execute(request);
		ensureTokenStatus(response, url.toString());
		String payload = readEntityQuietly(response.getEntity());
		return parseAccessToken(payload);
	}

	private AccessToken requestAzureCliToken() {
		String[] command = new String[] { "az", "account", "get-access-token", "--resource", tokenResource, "--output", "json" };
		String payload = runCommand(command, resolveAzureCliTimeout());
		return parseAccessToken(payload);
	}

	private void ensureTokenStatus(HttpResponse response, String url) {
		int actual = response.getStatusLine().getStatusCode();
		if (actual == HttpStatus.SC_OK) {
			return;
		}
		String body = readEntityQuietly(response.getEntity());
		throw new StorageException("azure token request failed for " + url + " with status " + actual + ": " + body);
	}

	private AccessToken parseAccessToken(String payload) {
		Map<String, String> values = parseFlatJson(payload);
		String accessToken = firstPresent(values, "access_token", "accessToken");
		if (accessToken == null || accessToken.length() == 0) {
			throw new StorageException("azure token response does not include access_token");
		}
		String tokenType = firstPresent(values, "token_type", "tokenType");
		if (tokenType == null || tokenType.length() == 0) {
			tokenType = "Bearer";
		}
		long expiresIn = parseLong(firstPresent(values, "expires_in"), 3600L);
		long expiresOn = System.currentTimeMillis() + expiresIn * 1000L;
		long parsedExpiresOn = parseExpiresOnMillis(firstPresent(values, "expires_on", "expiresOn"));
		if (parsedExpiresOn > 0L) {
			expiresOn = parsedExpiresOn;
		}
		return new AccessToken(accessToken, tokenType, expiresOn);
	}

	private Map<String, String> parseFlatJson(String payload) {
		Map<String, String> values = new LinkedHashMap<String, String>();
		Matcher matcher = JSON_STRING_PATTERN.matcher(payload);
		while (matcher.find()) {
			values.put(matcher.group(1), matcher.group(2));
		}

		captureNumericField(payload, values, "expires_in");
		captureNumericField(payload, values, "expires_on");
		return values;
	}

	private void captureNumericField(String payload, Map<String, String> values, String key) {
		Pattern pattern = Pattern.compile("\\\"" + key + "\\\"\\s*:\\s*([0-9]+)");
		Matcher matcher = pattern.matcher(payload);
		if (matcher.find()) {
			values.put(key, matcher.group(1));
		}
	}

	private String firstPresent(Map<String, String> values, String... keys) {
		if (keys == null) {
			return null;
		}
		for (int i = 0; i < keys.length; i++) {
			String key = keys[i];
			if (key == null || key.length() == 0) {
				continue;
			}
			String value = values.get(key);
			if (value != null && value.length() > 0) {
				return value;
			}
		}
		return null;
	}

	private long parseExpiresOnMillis(String value) {
		if (value == null || value.length() == 0) {
			return -1L;
		}

		long epochSeconds = parseLong(value, -1L);
		if (epochSeconds > 0L) {
			return epochSeconds * 1000L;
		}

		String[] patterns = new String[] {
			"yyyy-MM-dd HH:mm:ss.SSSSSS",
			"yyyy-MM-dd HH:mm:ss.SSS",
			"yyyy-MM-dd HH:mm:ss"
		};
		for (int i = 0; i < patterns.length; i++) {
			SimpleDateFormat format = new SimpleDateFormat(patterns[i], Locale.US);
			format.setLenient(false);
			try {
				Date parsed = format.parse(value);
				if (parsed != null) {
					return parsed.getTime();
				}
			} catch (ParseException ignored) {
				// try next format
			}
		}
		return -1L;
	}

	private long parseLong(String value, long fallback) {
		if (value == null || value.length() == 0) {
			return fallback;
		}
		try {
			return Long.parseLong(value);
		} catch (NumberFormatException e) {
			return fallback;
		}
	}

	private String signRequest(HttpRequestBase request, String url, long contentLength) {
		try {
			String stringToSign = buildStringToSign(request, url, contentLength);
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(Base64.decodeBase64(storageKey.getBytes("UTF-8")), "HmacSHA256"));
			return new String(Base64.encodeBase64(mac.doFinal(stringToSign.getBytes("UTF-8"))));
		} catch (UnsupportedEncodingException e) {
			throw new StorageException(e);
		} catch (NoSuchAlgorithmException e) {
			throw new StorageException(e);
		} catch (InvalidKeyException e) {
			throw new StorageException(e);
		}
	}

	private String buildStringToSign(HttpRequestBase request, String url, long contentLength) {
		StringBuilder canonicalizedHeaders = new StringBuilder();
		TreeMap<String, String> msHeaders = new TreeMap<String, String>();
		Header[] headers = request.getAllHeaders();
		for (int i = 0; i < headers.length; i++) {
			String name = headers[i].getName().toLowerCase(Locale.US);
			if (name.startsWith("x-ms-")) {
				msHeaders.put(name, normalizeHeaderValue(headers[i].getValue()));
			}
		}
		Iterator<Map.Entry<String, String>> iterator = msHeaders.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<String, String> entry = iterator.next();
			canonicalizedHeaders.append(entry.getKey()).append(':').append(entry.getValue()).append('\n');
		}

		StringBuilder stringToSign = new StringBuilder();
		stringToSign.append(request.getMethod()).append('\n');
		stringToSign.append(headerValue(request, "Content-Encoding")).append('\n');
		stringToSign.append(headerValue(request, "Content-Language")).append('\n');
		stringToSign.append(contentLength > 0L ? String.valueOf(contentLength) : "").append('\n');
		stringToSign.append(headerValue(request, "Content-MD5")).append('\n');
		stringToSign.append(headerValue(request, "Content-Type")).append('\n');
		stringToSign.append(headerValue(request, "Date")).append('\n');
		stringToSign.append(headerValue(request, "If-Modified-Since")).append('\n');
		stringToSign.append(headerValue(request, "If-Match")).append('\n');
		stringToSign.append(headerValue(request, "If-None-Match")).append('\n');
		stringToSign.append(headerValue(request, "If-Unmodified-Since")).append('\n');
		stringToSign.append(headerValue(request, "Range")).append('\n');
		stringToSign.append(canonicalizedHeaders);
		stringToSign.append(buildCanonicalizedResource(url));
		return stringToSign.toString();
	}

	private String buildCanonicalizedResource(String url) {
		try {
			URI uri = new URI(url);
			StringBuilder canonicalizedResource = new StringBuilder();
			canonicalizedResource.append('/').append(accountName);
			canonicalizedResource.append(uri.getRawPath());

			Map<String, List<String>> params = parseQuery(uri.getRawQuery());
			Iterator<Map.Entry<String, List<String>>> iterator = params.entrySet().iterator();
			while (iterator.hasNext()) {
				Map.Entry<String, List<String>> entry = iterator.next();
				Collections.sort(entry.getValue());
				canonicalizedResource.append('\n').append(entry.getKey()).append(':').append(join(entry.getValue(), ","));
			}
			return canonicalizedResource.toString();
		} catch (URISyntaxException e) {
			throw new StorageException(e);
		}
	}

	private Map<String, List<String>> parseQuery(String rawQuery) {
		TreeMap<String, List<String>> params = new TreeMap<String, List<String>>();
		if (rawQuery == null || rawQuery.length() == 0) {
			return params;
		}

		String[] pairs = rawQuery.split("&");
		for (int i = 0; i < pairs.length; i++) {
			String pair = pairs[i];
			String[] tokens = pair.split("=", 2);
			String key = urlDecode(tokens[0]).toLowerCase(Locale.US);
			String value = tokens.length > 1 ? urlDecode(tokens[1]) : "";
			List<String> values = params.get(key);
			if (values == null) {
				values = new ArrayList<String>();
				params.put(key, values);
			}
			values.add(value);
		}
		return params;
	}

	private String urlDecode(String value) {
		try {
			return URLDecoder.decode(value, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new StorageException(e);
		}
	}

	private String headerValue(HttpRequestBase request, String name) {
		Header header = request.getFirstHeader(name);
		return header == null ? "" : header.getValue();
	}

	private String normalizeHeaderValue(String value) {
		return value == null ? "" : value.trim().replaceAll("\\s+", " ");
	}

	private String buildContainerUrl(String container, String query) {
		StringBuilder url = new StringBuilder(endpoint);
		url.append('/').append(encodePathSegment(container));
		if (query != null && query.length() > 0) {
			url.append('?').append(query);
		}
		return url.toString();
	}

	private String buildBlobUrl(String container, String object, String query) {
		StringBuilder url = new StringBuilder(endpoint);
		url.append('/').append(encodePathSegment(container));
		url.append('/').append(encodeBlobPath(object));
		if (query != null && query.length() > 0) {
			url.append('?').append(query);
		}
		return url.toString();
	}

	private String encodeBlobPath(String path) {
		String[] segments = path.split("/");
		StringBuilder encoded = new StringBuilder();
		for (int i = 0; i < segments.length; i++) {
			if (i > 0) {
				encoded.append('/');
			}
			encoded.append(encodePathSegment(segments[i]));
		}
		return encoded.toString();
	}

	private String encodePathSegment(String value) {
		return HttpClientUtil.encodeURL(value);
	}

	private String normalizeAuthType(String value) {
		return safe(value).toLowerCase(Locale.US);
	}

	private String trimTrailingSlash(String value) {
		String result = safe(value);
		while (result.endsWith("/")) {
			result = result.substring(0, result.length() - 1);
		}
		return result;
	}

	private String safe(String value) {
		return value == null ? "" : value.trim();
	}

	private String getConfigOrDefault(Config config, String key, String fallback) {
		return safe(config.get(key, fallback));
	}

	private String getConfigOrEnv(Config config, String key, String fallback, String... envKeys) {
		String configured = safe(config.get(key, ""));
		if (configured.length() > 0) {
			return configured;
		}
		return getEnvOrDefault(fallback, envKeys);
	}

	private String getEnvOrDefault(String key, String fallback) {
		String value = System.getenv(key);
		return value == null || value.trim().length() == 0 ? fallback : value.trim();
	}

	private String getEnvOrDefault(String fallback, String... keys) {
		if (keys != null) {
			for (int i = 0; i < keys.length; i++) {
				String key = keys[i];
				if (key == null || key.length() == 0) {
					continue;
				}
				String value = System.getenv(key);
				if (value != null && value.trim().length() > 0) {
					return value.trim();
				}
			}
		}
		return safe(fallback);
	}

	private long resolveAzureCliTimeout() {
		return Math.max(AZ_CLI_TIMEOUT_MILLIS, timeout);
	}

	private String runCommand(String[] command, long commandTimeoutMillis) {
		Process process = null;
		try {
			ProcessBuilder builder = new ProcessBuilder(command);
			builder.redirectErrorStream(true);
			process = builder.start();
			boolean completed = process.waitFor(commandTimeoutMillis, TimeUnit.MILLISECONDS);
			if (!completed) {
				process.destroy();
				process.waitFor(5L, TimeUnit.SECONDS);
				if (process.isAlive()) {
					process.destroyForcibly();
				}
				throw new StorageException("azure cli command timed out; ensure az login is complete and retry");
			}

			String output = readStream(process.getInputStream());
			int exitCode = process.exitValue();
			if (exitCode != 0) {
				throw new StorageException("azure cli token request failed with exit code " + exitCode + ": " + output);
			}
			return output;
		} catch (IOException e) {
			throw new StorageException("failed to execute azure cli; ensure az is installed and available in PATH", e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new StorageException("interrupted while waiting for azure cli token", e);
		} finally {
			if (process != null) {
				closeQuietly(process.getInputStream());
				closeQuietly(process.getOutputStream());
				closeQuietly(process.getErrorStream());
			}
		}
	}

	private String readStream(InputStream stream) throws IOException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		byte[] data = new byte[4096];
		int read;
		while ((read = stream.read(data)) >= 0) {
			buffer.write(data, 0, read);
		}
		return buffer.toString("UTF-8").trim();
	}

	private void closeQuietly(InputStream stream) {
		if (stream == null) {
			return;
		}
		try {
			stream.close();
		} catch (IOException ignored) {
			// ignore cleanup failures
		}
	}

	private void closeQuietly(java.io.OutputStream stream) {
		if (stream == null) {
			return;
		}
		try {
			stream.close();
		} catch (IOException ignored) {
			// ignore cleanup failures
		}
	}

	private String join(List<String> values, String separator) {
		StringBuilder joined = new StringBuilder();
		for (int i = 0; i < values.size(); i++) {
			if (i > 0) {
				joined.append(separator);
			}
			joined.append(values.get(i));
		}
		return joined.toString();
	}

	private String mask(String value) {
		if (value == null || value.length() == 0) {
			return "";
		}
		if (value.length() <= 4) {
			return "****";
		}
		return value.substring(0, 2) + "****" + value.substring(value.length() - 2);
	}

	private String formatDate(Date date) {
		SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US);
		format.setTimeZone(TimeZone.getTimeZone("GMT"));
		return format.format(date);
	}

	private static class AccessToken {
		private final String accessToken;
		private final String tokenType;
		private final long expiresOnMillis;

		private AccessToken(String accessToken, String tokenType, long expiresOnMillis) {
			this.accessToken = accessToken;
			this.tokenType = tokenType;
			this.expiresOnMillis = expiresOnMillis;
		}

		private boolean shouldRefresh() {
			return System.currentTimeMillis() + TOKEN_REFRESH_SKEW_MILLIS >= expiresOnMillis;
		}
	}
}