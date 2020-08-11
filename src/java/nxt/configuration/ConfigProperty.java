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

import nxt.util.Convert;

import java.util.Objects;
import java.util.stream.Stream;

public class ConfigProperty {
    private final String name;
    private final String group;
    private final String description;
    private final String defaultValue;
    private final String installerValue;
    private final String configuredValue;
    private final PropertyType type;
    private final boolean isList;
    private final Integer min;
    private final Integer max;

    ConfigProperty(String name, String group, String description,
                   String defaultValue, String installerValue, String configuredValue,
                   PropertyType type, boolean isList, Integer min, Integer max) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.group = Objects.requireNonNull(group, "group must not be null");
        this.description = description;
        this.type = Objects.requireNonNull(type, "type must not be null");

        if (type == PropertyType.PASSWORD) {
            this.defaultValue = "";
            this.installerValue = installerValue == null ? null : "";
            this.configuredValue = configuredValue == null ? null : "";
        } else {
            this.defaultValue = Convert.nullToEmpty(defaultValue);
            this.installerValue = installerValue;
            this.configuredValue = configuredValue;
        }

        this.isList = isList;
        this.min = min;
        this.max = max;

        if (isList && !(type == PropertyType.STRING || type == PropertyType.ACCOUNT)) {
            throw new IllegalArgumentException("Only string and account properties can be lists.");
        }

        if (type != PropertyType.INTEGER && (min != null || max != null)) {
            throw new IllegalArgumentException("Only integer properties can have a minimum or maximum.");
        }
    }

    /**
     * Checks if a given value is valid for this property.
     * <p>
     * This doesn't change any internal value, it's just a validation that can be used to accept new values.
     *
     * @param value the value to check
     * @return true if the provided value is valid for this property
     */
    public boolean isValidValue(String value) {
        if (value == null) {
            return false; // this is not a valid value, a null should remove the configuration property
        }
        switch (type) {
            case STRING:
            case PASSWORD:
                return true;
            case READONLY:
                return false;
            case BOOLEAN:
                return value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false");
            case INTEGER:
                try {
                    int iValue = Integer.parseInt(value);
                    return (min == null || iValue >= min) && (max == null || iValue <= max);
                } catch (RuntimeException e) {
                    return false;
                }
            case ACCOUNT:
                if (isList) {
                    return Stream.of(value.split(";")).allMatch(this::isValidAccountId);
                } else {
                    return isValidAccountId(value);
                }
        }
        throw new RuntimeException("Unknown ConfigType: " + type);
    }

    private boolean isValidAccountId(String value) {
        try {
            return Convert.parseAccountId(value) != 0;
        } catch (RuntimeException e) {
            return false;
        }
    }

    public String getName() {
        return name;
    }

    public String getGroup() {
        return group;
    }

    public String getDescription() {
        return description;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public String getInstallerValue() {
        return installerValue;
    }

    public String getConfiguredValue() {
        return configuredValue;
    }

    public PropertyType getType() {
        return type;
    }

    public boolean isList() {
        return isList;
    }

    public Integer getMin() {
        return min;
    }

    public Integer getMax() {
        return max;
    }
}
