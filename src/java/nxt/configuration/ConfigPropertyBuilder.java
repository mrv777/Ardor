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
import nxt.util.Logger;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static nxt.configuration.PropertyType.ACCOUNT;
import static nxt.configuration.PropertyType.BOOLEAN;
import static nxt.configuration.PropertyType.INTEGER;
import static nxt.configuration.PropertyType.STRING;

@SuppressWarnings("unused")
public class ConfigPropertyBuilder {
    private static final String ADD_ONS_GROUP = "Add-ons";
    private static final String USER_GROUP = "User";

    private final String name;
    private final String group;
    private String description;
    private final String defaultValue;
    private String installerValue;
    private String configuredValue;
    private PropertyType type = STRING;
    private boolean isList;
    private Integer min;
    private Integer max;

    private ConfigPropertyBuilder(String name, String group, String defaultValue) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.group = Objects.requireNonNull(group, "group must not be null");
        this.defaultValue = Convert.nullToEmpty(defaultValue);
    }

    private ConfigPropertyBuilder(String name, String group, String description, String defaultValue, PropertyType type) {
        this(name, group, defaultValue);
        this.description = description;
        this.type = type;
    }

    /**
     * Creates a builder parsing the comments for metadata found on the nxt-default.properties file.
     *
     * @param name         the name of the property
     * @param defaultValue the default value of the property
     * @param comments     the list of comments for the property including an optional last line of metadata
     * @param group        the group of the property
     * @return a builder initialized with the parsed data
     */
    static ConfigPropertyBuilder parseDefault(String name, String defaultValue, List<String> comments, String group) {
        ConfigPropertyBuilder builder = new ConfigPropertyBuilder(name, group, defaultValue);

        if (!comments.isEmpty() && comments.get(comments.size() - 1).startsWith("$type=")) {
            String metadata = comments.remove(comments.size() - 1);
            String[] tokens = metadata.split(" ");
            for (String token : tokens) {
                try {
                    builder.parseToken(token);
                } catch (Exception e) {
                    Logger.logDebugMessage("Ignoring metadata token " + token, e);
                }
            }
        }

        builder.description = String.join("\n", comments);
        return builder;
    }

    private void parseToken(String token) {
        if (token.startsWith("$type=")) {
            type = PropertyType.valueOf(token.substring("$type=".length()).toUpperCase());
        } else if ("$isList=true".equals(token)) {
            isList = true;
        } else if (token.startsWith("$min=")) {
            min = Integer.parseInt(token.substring("$min=".length()));
        } else if (token.startsWith("$max=")) {
            max = Integer.parseInt(token.substring("$max=".length()));
        }
    }

    /**
     * Creates a configuration property definition for configured properties not found on the default list nor defined
     * by any current add-on.
     *
     * @param name the name of the property
     * @return a configuration metadata definition
     */
    static ConfigPropertyBuilder createUserProperty(String name) {
        ConfigPropertyBuilder builder = new ConfigPropertyBuilder(name, USER_GROUP, "");
        String lowerName = name.toLowerCase();
        if (lowerName.contains("password") || lowerName.contains("secret") || lowerName.contains("passphrase")) {
            builder.type = PropertyType.PASSWORD;
        }
        return builder;
    }

    /**
     * Creates a configuration metadata definition for a string property.
     *
     * @param name         the name of the property
     * @param defaultValue the default value of the property, can be empty but not null
     * @param description  the description of the property
     * @return a configuration metadata definition
     */
    public static ConfigPropertyBuilder createStringProperty(String name, String defaultValue, String description) {
        return new ConfigPropertyBuilder(name, ADD_ONS_GROUP, description, defaultValue, STRING);
    }

    /**
     * Creates a configuration metadata definition for a string list property.
     *
     * @param name         the name of the property
     * @param defaultValue the default value of the property, can be empty but not null
     * @param description  the description of the property
     * @return a configuration metadata definition
     */
    public static ConfigPropertyBuilder createStringListProperty(String name, List<String> defaultValue, String description) {
        final String defaultValueString = String.join(";", defaultValue);
        return new ConfigPropertyBuilder(name, ADD_ONS_GROUP, description, defaultValueString, STRING).setList();
    }

    /**
     * Creates a configuration metadata definition for a boolean property.
     *
     * @param name         the name of the property
     * @param defaultValue the default value of the property
     * @param description  the description of the property
     * @return a configuration metadata definition
     */
    public static ConfigPropertyBuilder createBooleanProperty(String name, boolean defaultValue, String description) {
        return new ConfigPropertyBuilder(name, ADD_ONS_GROUP, description, Boolean.toString(defaultValue), BOOLEAN);
    }

    /**
     * Creates a configuration metadata definition for an integer property.
     *
     * @param name         the name of the property
     * @param defaultValue the default value of the property
     * @param description  the description of the property
     * @return a configuration metadata definition
     */
    public static ConfigPropertyBuilder createIntegerProperty(String name, int defaultValue, String description) {
        return new ConfigPropertyBuilder(name, ADD_ONS_GROUP, description, Integer.toString(defaultValue), INTEGER);
    }

    /**
     * Creates a configuration metadata definition for an account property.
     *
     * @param name         the name of the property
     * @param defaultValue the default value of the property, set to null for an empty default value
     * @param description  the description of the property
     * @return a configuration metadata definition
     */
    public static ConfigPropertyBuilder createAccountProperty(String name, Long defaultValue, String description) {
        final String defaultValueString = defaultValue == null ? "" : Convert.rsAccount(defaultValue);
        return new ConfigPropertyBuilder(name, ADD_ONS_GROUP, description, defaultValueString, ACCOUNT);
    }

    /**
     * Creates a configuration metadata definition for an account list property.
     *
     * @param name         the name of the property
     * @param defaultValue the default value of the property, can be empty but not null
     * @param description  the description of the property
     * @return a configuration metadata definition
     */
    public static ConfigPropertyBuilder createAccountListProperty(String name, List<Long> defaultValue, String description) {
        final String defaultValueString = defaultValue.stream().map(Convert::rsAccount).collect(Collectors.joining(";"));
        return new ConfigPropertyBuilder(name, ADD_ONS_GROUP, description, defaultValueString, ACCOUNT).setList();
    }

    /**
     * Creates a configuration metadata definition for a password property.
     *
     * @param name         the name of the property
     * @param defaultValue the default value of the property, can be empty but not null
     * @param description  the description of the property
     * @return a configuration metadata definition
     */
    public static ConfigPropertyBuilder createPasswordProperty(String name, String defaultValue, String description) {
        return new ConfigPropertyBuilder(name, ADD_ONS_GROUP, description, defaultValue, PropertyType.PASSWORD);
    }

    ConfigProperty build() {
        return new ConfigProperty(name, group, description, defaultValue, installerValue, configuredValue, type, isList, min, max);
    }

    String getName() {
        return name;
    }

    void setInstallerValue(String installerValue) {
        this.installerValue = installerValue;
    }

    void setConfiguredValue(String configuredValue) {
        this.configuredValue = configuredValue;
    }

    private ConfigPropertyBuilder setList() {
        isList = true;
        return this;
    }

    /**
     * @param min the defined minimum value, set to null to disable the constraint
     * @return this, to continue building
     */
    public ConfigPropertyBuilder setMin(Integer min) {
        this.min = min;
        return this;
    }

    /**
     * @param max the defined maximum value, set to null to disable the constraint
     * @return this, to continue building
     */
    public ConfigPropertyBuilder setMax(Integer max) {
        this.max = max;
        return this;
    }
}
