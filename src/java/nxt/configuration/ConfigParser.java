/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2020 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of this software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

package nxt.configuration;

import nxt.Nxt;
import nxt.addons.AddOns;
import nxt.util.Logger;
import nxt.util.security.BlockchainPermission;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class ConfigParser {

    private static ConfigParser instance;

    private final Map<String, ConfigProperty> properties;

    private ConfigParser(List<ConfigPropertyBuilder> addOnsProperties) {
        // parse and load nxt-default.properties
        Map<String, ConfigPropertyBuilder> builders = PropertiesParser.parseDefault();

        // add all properties defined by add-ons
        for (ConfigPropertyBuilder property : addOnsProperties) {
            if (builders.containsKey(property.getName())) {
                Logger.logWarningMessage("Ignoring add-on provided property " + property.getName() + ", already registered.");
            } else {
                builders.put(property.getName(), property);
            }
        }

        // load and process nxt-installer.properties
        Properties installer = new Properties();
        Nxt.loadProperties(installer, Nxt.NXT_INSTALLER_PROPERTIES, true);
        for(String name : installer.stringPropertyNames()) {
            builders.computeIfAbsent(name, ConfigPropertyBuilder::createUserProperty)
                    .setInstallerValue(installer.getProperty(name));
        }

        // load and process nxt.properties
        Properties user = new Properties();
        Nxt.loadProperties(user, Nxt.NXT_PROPERTIES, false);
        for(String name : user.stringPropertyNames()) {
            builders.computeIfAbsent(name, ConfigPropertyBuilder::createUserProperty)
                    .setConfiguredValue(user.getProperty(name));
        }

        // populate the final property list from the list of builders (keeping the definition order)
        Map<String, ConfigProperty> properties = new LinkedHashMap<>();
        builders.values().stream().map(ConfigPropertyBuilder::build)
                .forEachOrdered(configProperty -> properties.put(configProperty.getName(), configProperty));
        this.properties = Collections.unmodifiableMap(properties);
    }

    public synchronized static ConfigParser getInstance() {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new BlockchainPermission("properties"));
        }
        if (instance == null) {
            instance = new ConfigParser(AddOns.getAddOnProperties());
        }
        return instance;
    }

    public Map<String, ConfigProperty> getProperties() {
        return properties;
    }

    public boolean isValidPropertyValue(String property, String value) {
        ConfigProperty configProperty = properties.get(property);
        if (configProperty == null) {
            return true; // we accept user-generated properties
        }
        return configProperty.isValidValue(value);
    }
}
