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

import static org.apache.commons.lang.SystemUtils.IS_OS_WINDOWS;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.intel.cosbench.config.ConfigException;
import com.intel.cosbench.config.castor.CastorConfigTools;
import com.intel.cosbench.controller.model.*;
import com.intel.cosbench.service.*;

public class COSBControllerServiceFactory extends AbstractServiceFactory
        implements ControllerServiceFactory {

    private static final String SERVICE_NAME = "controller";

    private static final String CFG_FILE_KEY = "cosbench.controller.config";

    private static final String UNIX_DEFAULT_CFG_FILE = "/etc/cosbench/controller.conf";

    private static final String WIN_DEFAULT_CFG_FILE = "C:\\controller.conf";

    public COSBControllerServiceFactory() {
        /* loading workload XML mappings */
        CastorConfigTools.getWorkloadResolver();
//        /* creating workload archive directory */
//        new SimpleWorkloadArchiver();
    }

    @Override
    protected String getConfigFile() {
        String configFile;
        if ((configFile = System.getProperty(CFG_FILE_KEY)) != null)
            return configFile;
        if (new File("controller.conf").exists())
            return "controller.conf";
        if (new File("conf/controller.conf").exists())
            return "conf/controller.conf";
        return IS_OS_WINDOWS ? WIN_DEFAULT_CFG_FILE : UNIX_DEFAULT_CFG_FILE;
    }

    @Override
    protected String getServiceName() {
        return SERVICE_NAME;
    }

    @Override
    public ControllerService getControllerService() {
        COSBControllerService service = new COSBControllerService();
        ControllerContext context = getControllerContext();
        service.setContext(context);
        service.init();
        return service;
    }

    private ControllerContext getControllerContext() {
        ControllerContext context = new ControllerContext();
        context.setName(loadControllerName());
        context.setUrl(loadControllerUrl());
        context.setArchive_dir(loadArchiveDir());
        context.setConcurrency(loadConcurrency());
        context.setDriverRegistry(getDriverRegistry());
        return context;
    }

    protected String loadLogLevel() {
        return config.get("controller.log_level", "INFO");
    }

    protected String loadLogFile() {
        return config.get("controller.log_file", "log/system.log");
    }

    private String loadArchiveDir() {
    	return config.get("controller.archive_dir", "archive");
    }
    
    private String loadControllerName() {
        return config.get("controller.name", "N/A");
    }

    private String loadControllerUrl() {
        return config.get("controller.url", "N/A");
    }

    private int loadConcurrency() {
        return config.getInt("controller.concurrency", 1);
    }

    private DriverRegistry getDriverRegistry() {
        DriverRegistry registry = new DriverRegistry();
        List<String> driverSections = loadDriverSections();
        Set<String> driverNames = new LinkedHashSet<String>();
        for (String section : driverSections) {
            DriverContext context = getDriverContext(section);
            if (context.getName() == null || context.getName().trim().isEmpty()) {
                throw new ConfigException("missing driver name in section [" + section + "]");
            }
            if (context.getUrl() == null || context.getUrl().trim().isEmpty()) {
                throw new ConfigException("missing driver url in section [" + section + "]");
            }
            if (!driverNames.add(context.getName())) {
                throw new ConfigException("duplicate driver name detected: " + context.getName());
            }
            registry.addDriver(context);
        }
        return registry;
    }

    private List<String> loadDriverSections() {
        String configured = config.get("controller.drivers", "");
        configured = configured == null ? "" : configured.trim();

        if (configured.length() == 0) {
            List<String> discovered = discoverDriverSections();
            if (discovered.isEmpty()) {
                throw new ConfigException("no driver section found in controller.conf");
            }
            return discovered;
        }

        if (configured.matches("\\d+")) {
            int count = Integer.parseInt(configured);
            List<String> sections = new ArrayList<String>();
            for (int i = 1; i <= count; i++) {
                sections.add("driver" + i);
            }
            return sections;
        }

        List<String> sections = new ArrayList<String>();
        for (String token : configured.split(",")) {
            String section = token.trim();
            if (section.length() == 0) {
                continue;
            }
            if (section.matches("\\d+")) {
                section = "driver" + section;
            }
            sections.add(section);
        }

        if (sections.isEmpty()) {
            throw new ConfigException("controller.drivers must be either an integer count or a comma-separated driver section list");
        }

        return sections;
    }

    private List<String> discoverDriverSections() {
        List<String> sections = new ArrayList<String>();
        File configFile = new File(getConfigFile());
        if (!configFile.exists()) {
            return sections;
        }

        try {
            BufferedReader reader = new BufferedReader(new FileReader(configFile));
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                        String section = trimmed.substring(1, trimmed.length() - 1).trim();
                        if (section.startsWith("driver")) {
                            sections.add(section);
                        }
                    }
                }
            } finally {
                reader.close();
            }
        } catch (IOException e) {
            throw new ConfigException("cannot discover driver sections from " + configFile.getAbsolutePath(), e);
        }

        return sections;
    }

    private DriverContext getDriverContext(String section) {
        DriverContext context = new DriverContext();
        context.setName(loadDriverName(section));
        context.setUrl(loadDriverUrl(section));
        context.setAliveState(false);
        return context;
    }

    private String loadDriverName(String section) {
        return config.get(section + ".name");
    }

    private String loadDriverUrl(String section) {
        return config.get(section + ".url");
    }

}
