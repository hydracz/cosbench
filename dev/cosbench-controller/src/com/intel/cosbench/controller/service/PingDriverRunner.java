/** 
 
Copyright 2013 Intel Corporation, All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License. 
*/ 

package com.intel.cosbench.controller.service;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;

import com.intel.cosbench.model.DriverInfo;

public class PingDriverRunner implements Runnable{

	private static final int DEFAULT_DRIVER_PORT = 18088;

	private static class DriverEndpoint {
		private final String host;
		private final int port;

		private DriverEndpoint(String host, int port) {
			this.host = host;
			this.port = port;
		}
	}

	private int interval = 5000;
	private DriverInfo[] driverInfos;
	
	PingDriverRunner(DriverInfo[] driverInfos){
		this.driverInfos = driverInfos;
	}
	
	@Override
	public void run() {
		while (true) {
			pingDrivers(driverInfos);
			try {
				Thread.sleep(interval);
			} catch (InterruptedException ignore) {
			}
		}
	}

	private void pingDrivers(DriverInfo[] driverInfos) {
		for (DriverInfo driver : driverInfos) {
			boolean isAlive = false;

			DriverEndpoint endpoint = parseDriverEndpoint(driver.getUrl());
			try {
				if (endpoint != null) {
					try {
						InetSocketAddress reAddress = new InetSocketAddress(endpoint.host, endpoint.port);
						Socket socket = new Socket();
						socket.connect(reAddress, 3000);
						socket.close();
						isAlive = true;
					} catch (Exception e) {
							isAlive = false;
						}
				}
			} finally {
				driver.setAliveState(isAlive);
			}
		}
	}
	
	private DriverEndpoint parseDriverEndpoint(String url) {
		if (url == null || url.trim().isEmpty()) {
			return null;
		}

		try {
			URI uri = new URI(url.trim());
			String host = uri.getHost();
			if (host == null || host.length() == 0) {
				return null;
			}
			int port = uri.getPort();
			if (port <= 0) {
				port = DEFAULT_DRIVER_PORT;
			}
			return new DriverEndpoint(host, port);
		} catch (URISyntaxException e) {
			return null;
		}
	}
	
}

