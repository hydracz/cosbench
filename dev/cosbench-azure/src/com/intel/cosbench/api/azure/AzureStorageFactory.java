package com.intel.cosbench.api.azure;

import com.intel.cosbench.api.storage.StorageAPI;
import com.intel.cosbench.api.storage.StorageAPIFactory;

public class AzureStorageFactory implements StorageAPIFactory {

	@Override
	public String getStorageName() {
		return "azure";
	}

	@Override
	public StorageAPI getStorageAPI() {
		return new AzureStorage();
	}
}