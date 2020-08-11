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
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.Map;
import java.util.NoSuchElementException;

public class DefaultConfigParserTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void loadAndRefresh() {
        ConfigParser configParser = ConfigParser.getInstance();
        Assert.assertNotNull(configParser);
        Assert.assertFalse(configParser.getProperties().isEmpty());
        ConfigProperty property = configParser.getProperties().values().stream()
                .filter(p -> "nxt.version".equals(p.getName()))
                .findAny()
                .orElseThrow(NoSuchElementException::new);
        Assert.assertEquals("nxt.version", property.getName());
        Assert.assertEquals("Developers only", property.getGroup());
        Assert.assertEquals("Product version.", property.getDescription());
        Assert.assertEquals(Nxt.VERSION, property.getDefaultValue());
        Assert.assertNull(property.getInstallerValue());
        Assert.assertNull(property.getConfiguredValue());
        Assert.assertEquals(PropertyType.READONLY, property.getType());
        Assert.assertFalse(property.isList());
        Assert.assertNull(property.getMin());
        Assert.assertNull(property.getMax());
    }

    @Ignore("Depends on static init, can only be single-run")
    @Test
    public void empty() {
        System.setProperty(Nxt.NXT_DEFAULT_PROPERTIES, "test/java/nxt/configuration/empty-default.properties");
        System.setProperty(Nxt.NXT_INSTALLER_PROPERTIES, "test/java/nxt/configuration/empty.properties");
        System.setProperty(Nxt.NXT_PROPERTIES, "test/java/nxt/configuration/empty.properties");
        ConfigParser configParser = ConfigParser.getInstance();
        Assert.assertEquals(1, configParser.getProperties().size());
        configParser.getProperties().values().forEach(property -> {
            Assert.assertEquals("nxt.version", property.getName());
            Assert.assertEquals("", property.getGroup());
            Assert.assertEquals("Product version.", property.getDescription());
            Assert.assertEquals(Nxt.VERSION, property.getDefaultValue());
            Assert.assertNull(property.getInstallerValue());
            Assert.assertNull(property.getConfiguredValue());
            Assert.assertEquals(PropertyType.READONLY, property.getType());
            Assert.assertFalse(property.isList());
            Assert.assertNull(property.getMin());
            Assert.assertNull(property.getMax());
        });
    }

    @Ignore("Depends on static init, can only be single-run")
    @Test
    public void testDefaults() {
        System.setProperty(Nxt.NXT_DEFAULT_PROPERTIES, "test/java/nxt/configuration/test-default.properties");
        System.setProperty(Nxt.NXT_INSTALLER_PROPERTIES, "test/java/nxt/configuration/empty.properties");
        System.setProperty(Nxt.NXT_PROPERTIES, "test/java/nxt/configuration/empty.properties");
        ConfigParser configParser = ConfigParser.getInstance();

        ConfigProperty property = configParser.getProperties().get("nxt.version");
        Assert.assertEquals("nxt.version", property.getName());
        Assert.assertEquals("", property.getGroup());
        Assert.assertEquals("Product version.", property.getDescription());
        Assert.assertEquals("2.2.6", property.getDefaultValue());
        Assert.assertNull(property.getInstallerValue());
        Assert.assertNull(property.getConfiguredValue());
        Assert.assertEquals(PropertyType.READONLY, property.getType());
        Assert.assertFalse(property.isList());
        Assert.assertNull(property.getMin());
        Assert.assertNull(property.getMax());
        Assert.assertFalse(property.isValidValue(null));
        Assert.assertFalse(property.isValidValue(""));
        Assert.assertFalse(property.isValidValue("2.2.6"));

        property = configParser.getProperties().get("noMetadata");
        Assert.assertEquals("noMetadata", property.getName());
        Assert.assertEquals("FIRST GROUP", property.getGroup());
        Assert.assertEquals("No metadata. Defaults to $type=string", property.getDescription());
        Assert.assertEquals("hello world", property.getDefaultValue());
        Assert.assertNull(property.getInstallerValue());
        Assert.assertNull(property.getConfiguredValue());
        Assert.assertEquals(PropertyType.STRING, property.getType());
        Assert.assertFalse(property.isList());
        Assert.assertNull(property.getMin());
        Assert.assertNull(property.getMax());
        Assert.assertFalse(property.isValidValue(null));
        Assert.assertTrue(property.isValidValue(""));
        Assert.assertTrue(property.isValidValue("dummy"));

        property = configParser.getProperties().get("multilineComment");
        Assert.assertEquals("multilineComment", property.getName());
        Assert.assertEquals("FIRST GROUP", property.getGroup());
        Assert.assertEquals("Multiline\ncomment\nthis is also an allowed comment prefix", property.getDescription());
        Assert.assertEquals("andDifferentSeparator", property.getDefaultValue());
        Assert.assertNull(property.getInstallerValue());
        Assert.assertNull(property.getConfiguredValue());
        Assert.assertEquals(PropertyType.STRING, property.getType());
        Assert.assertFalse(property.isList());
        Assert.assertNull(property.getMin());
        Assert.assertNull(property.getMax());

        property = configParser.getProperties().get("noValue");
        Assert.assertEquals("noValue", property.getName());
        Assert.assertEquals("FIRST GROUP", property.getGroup());
        Assert.assertEquals("Empty value", property.getDescription());
        Assert.assertEquals("", property.getDefaultValue());
        Assert.assertNull(property.getInstallerValue());
        Assert.assertNull(property.getConfiguredValue());
        Assert.assertEquals(PropertyType.STRING, property.getType());
        Assert.assertFalse(property.isList());
        Assert.assertNull(property.getMin());
        Assert.assertNull(property.getMax());

        property = configParser.getProperties().get("completelyAlone");
        Assert.assertEquals("completelyAlone", property.getName());
        Assert.assertEquals("FIRST GROUP", property.getGroup());
        Assert.assertEquals("", property.getDescription());
        Assert.assertEquals("", property.getDefaultValue());
        Assert.assertNull(property.getInstallerValue());
        Assert.assertNull(property.getConfiguredValue());
        Assert.assertEquals(PropertyType.STRING, property.getType());
        Assert.assertFalse(property.isList());
        Assert.assertNull(property.getMin());
        Assert.assertNull(property.getMax());

        property = configParser.getProperties().get("basic");
        Assert.assertEquals("basic", property.getName());
        Assert.assertEquals("FIRST GROUP", property.getGroup());
        Assert.assertEquals("Basic property, explicit type to string.", property.getDescription());
        Assert.assertEquals("¡hola!", property.getDefaultValue());
        Assert.assertNull(property.getInstallerValue());
        Assert.assertNull(property.getConfiguredValue());
        Assert.assertEquals(PropertyType.STRING, property.getType());
        Assert.assertFalse(property.isList());
        Assert.assertNull(property.getMin());
        Assert.assertNull(property.getMax());

        property = configParser.getProperties().get("listOfString");
        Assert.assertEquals("listOfString", property.getName());
        Assert.assertEquals("FIRST GROUP", property.getGroup());
        Assert.assertEquals("Basic list.", property.getDescription());
        Assert.assertEquals("one;two", property.getDefaultValue());
        Assert.assertNull(property.getInstallerValue());
        Assert.assertNull(property.getConfiguredValue());
        Assert.assertEquals(PropertyType.STRING, property.getType());
        Assert.assertTrue(property.isList());
        Assert.assertNull(property.getMin());
        Assert.assertNull(property.getMax());

        property = configParser.getProperties().get("binary");
        Assert.assertEquals("binary", property.getName());
        Assert.assertEquals("LAST GROUP", property.getGroup());
        Assert.assertEquals("Basic boolean.", property.getDescription());
        Assert.assertEquals("true", property.getDefaultValue());
        Assert.assertNull(property.getInstallerValue());
        Assert.assertNull(property.getConfiguredValue());
        Assert.assertEquals(PropertyType.BOOLEAN, property.getType());
        Assert.assertFalse(property.isList());
        Assert.assertNull(property.getMin());
        Assert.assertNull(property.getMax());
        Assert.assertFalse(property.isValidValue(null));
        Assert.assertFalse(property.isValidValue(""));
        Assert.assertTrue(property.isValidValue("true"));
        Assert.assertTrue(property.isValidValue("True"));
        Assert.assertTrue(property.isValidValue("TRUE"));
        Assert.assertTrue(property.isValidValue("false"));
        Assert.assertTrue(property.isValidValue("False"));
        Assert.assertTrue(property.isValidValue("FALSE"));
        Assert.assertFalse(property.isValidValue("yes"));
        Assert.assertFalse(property.isValidValue("no"));

        property = configParser.getProperties().get("justMetadata");
        Assert.assertEquals("justMetadata", property.getName());
        Assert.assertEquals("LAST GROUP", property.getGroup());
        Assert.assertEquals("", property.getDescription());
        Assert.assertEquals("true", property.getDefaultValue());
        Assert.assertNull(property.getInstallerValue());
        Assert.assertNull(property.getConfiguredValue());
        Assert.assertEquals(PropertyType.BOOLEAN, property.getType());
        Assert.assertFalse(property.isList());
        Assert.assertNull(property.getMin());
        Assert.assertNull(property.getMax());

        property = configParser.getProperties().get("theAnswer");
        Assert.assertEquals("theAnswer", property.getName());
        Assert.assertEquals("LAST GROUP", property.getGroup());
        Assert.assertEquals("Basic integer", property.getDescription());
        Assert.assertEquals("42", property.getDefaultValue());
        Assert.assertNull(property.getInstallerValue());
        Assert.assertNull(property.getConfiguredValue());
        Assert.assertEquals(PropertyType.INTEGER, property.getType());
        Assert.assertFalse(property.isList());
        Assert.assertNull(property.getMin());
        Assert.assertNull(property.getMax());
        Assert.assertFalse(property.isValidValue(null));
        Assert.assertFalse(property.isValidValue(""));
        Assert.assertTrue(property.isValidValue("42"));
        Assert.assertTrue(property.isValidValue("-7"));
        Assert.assertTrue(property.isValidValue("0"));
        Assert.assertFalse(property.isValidValue("dummy"));

        property = configParser.getProperties().get("withMin");
        Assert.assertEquals("withMin", property.getName());
        Assert.assertEquals("LAST GROUP", property.getGroup());
        Assert.assertEquals("Integer with min", property.getDescription());
        Assert.assertEquals("14", property.getDefaultValue());
        Assert.assertNull(property.getInstallerValue());
        Assert.assertNull(property.getConfiguredValue());
        Assert.assertEquals(PropertyType.INTEGER, property.getType());
        Assert.assertFalse(property.isList());
        Assert.assertEquals(3, (long) property.getMin());
        Assert.assertNull(property.getMax());
        Assert.assertTrue(property.isValidValue("42"));
        Assert.assertFalse(property.isValidValue("-7"));
        Assert.assertFalse(property.isValidValue("0"));

        property = configParser.getProperties().get("withMax");
        Assert.assertEquals("withMax", property.getName());
        Assert.assertEquals("LAST GROUP", property.getGroup());
        Assert.assertEquals("Integer with max", property.getDescription());
        Assert.assertEquals("2", property.getDefaultValue());
        Assert.assertNull(property.getInstallerValue());
        Assert.assertNull(property.getConfiguredValue());
        Assert.assertEquals(PropertyType.INTEGER, property.getType());
        Assert.assertFalse(property.isList());
        Assert.assertNull(property.getMin());
        Assert.assertEquals(3, (long) property.getMax());
        Assert.assertFalse(property.isValidValue("42"));
        Assert.assertTrue(property.isValidValue("-7"));
        Assert.assertTrue(property.isValidValue("0"));

        property = configParser.getProperties().get("sharedCommentAndMetadata");
        Assert.assertEquals("sharedCommentAndMetadata", property.getName());
        Assert.assertEquals("LAST GROUP", property.getGroup());
        Assert.assertEquals("Integer with max", property.getDescription());
        Assert.assertEquals("1", property.getDefaultValue());
        Assert.assertNull(property.getInstallerValue());
        Assert.assertNull(property.getConfiguredValue());
        Assert.assertEquals(PropertyType.INTEGER, property.getType());
        Assert.assertFalse(property.isList());
        Assert.assertNull(property.getMin());
        Assert.assertEquals(3, (long) property.getMax());
        Assert.assertFalse(property.isValidValue("42"));
        Assert.assertTrue(property.isValidValue("-7"));
        Assert.assertTrue(property.isValidValue("0"));

        property = configParser.getProperties().get("bounded");
        Assert.assertEquals("bounded", property.getName());
        Assert.assertEquals("LAST GROUP", property.getGroup());
        Assert.assertEquals("Bounded integer", property.getDescription());
        Assert.assertEquals("112", property.getDefaultValue());
        Assert.assertNull(property.getInstallerValue());
        Assert.assertNull(property.getConfiguredValue());
        Assert.assertEquals(PropertyType.INTEGER, property.getType());
        Assert.assertFalse(property.isList());
        Assert.assertEquals(100, (long) property.getMin());
        Assert.assertEquals(999, (long) property.getMax());
        Assert.assertFalse(property.isValidValue(null));
        Assert.assertFalse(property.isValidValue(""));
        Assert.assertFalse(property.isValidValue("42"));
        Assert.assertFalse(property.isValidValue("-7"));
        Assert.assertTrue(property.isValidValue("112"));
        Assert.assertFalse(property.isValidValue("1120"));

        property = configParser.getProperties().get("accountRS");
        Assert.assertEquals("accountRS", property.getName());
        Assert.assertEquals("LAST GROUP", property.getGroup());
        Assert.assertEquals("Account RS format", property.getDescription());
        Assert.assertEquals("ARDOR-XK4R-7VJU-6EQG-7R335", property.getDefaultValue());
        Assert.assertNull(property.getInstallerValue());
        Assert.assertNull(property.getConfiguredValue());
        Assert.assertEquals(PropertyType.ACCOUNT, property.getType());
        Assert.assertFalse(property.isList());
        Assert.assertNull(property.getMin());
        Assert.assertNull(property.getMax());
        Assert.assertTrue(property.isValidValue("ARDOR-XK4R-7VJU-6EQG-7R335"));
        Assert.assertTrue(property.isValidValue("5873880488492319831"));
        Assert.assertTrue(property.isValidValue("-10000000"));

        property = configParser.getProperties().get("account");
        Assert.assertEquals("account", property.getName());
        Assert.assertEquals("LAST GROUP", property.getGroup());
        Assert.assertEquals("Account integer format", property.getDescription());
        Assert.assertEquals("5873880488492319831", property.getDefaultValue());
        Assert.assertNull(property.getInstallerValue());
        Assert.assertNull(property.getConfiguredValue());
        Assert.assertEquals(PropertyType.ACCOUNT, property.getType());
        Assert.assertFalse(property.isList());
        Assert.assertNull(property.getMin());
        Assert.assertNull(property.getMax());

        property = configParser.getProperties().get("testAccounts");
        Assert.assertEquals("testAccounts", property.getName());
        Assert.assertEquals("LAST GROUP", property.getGroup());
        Assert.assertEquals("Account list", property.getDescription());
        Assert.assertEquals("ARDOR-XK4R-7VJU-6EQG-7R335;ARDOR-EVHD-5FLM-3NMQ-G46NR;ARDOR-SZKV-J8TH-GSM9-9LKV6", property.getDefaultValue());
        Assert.assertNull(property.getInstallerValue());
        Assert.assertNull(property.getConfiguredValue());
        Assert.assertEquals(PropertyType.ACCOUNT, property.getType());
        Assert.assertTrue(property.isList());
        Assert.assertNull(property.getMin());
        Assert.assertNull(property.getMax());

        property = configParser.getProperties().get("secretPhrase");
        Assert.assertEquals("secretPhrase", property.getName());
        Assert.assertEquals("LAST GROUP", property.getGroup());
        Assert.assertEquals("Secret phrase", property.getDescription());
        Assert.assertEquals("", property.getDefaultValue());
        Assert.assertNull(property.getInstallerValue());
        Assert.assertNull(property.getConfiguredValue());
        Assert.assertEquals(PropertyType.PASSWORD, property.getType());
        Assert.assertFalse(property.isList());
        Assert.assertNull(property.getMin());
        Assert.assertNull(property.getMax());
        Assert.assertFalse(property.isValidValue(null));
        Assert.assertTrue(property.isValidValue(""));
        Assert.assertTrue(property.isValidValue("dummy"));

        property = configParser.getProperties().get("multilineValue");
        Assert.assertEquals("multilineValue", property.getName());
        Assert.assertEquals("LAST GROUP", property.getGroup());
        Assert.assertEquals("Multiline value", property.getDescription());
        Assert.assertEquals("hello world", property.getDefaultValue());
        Assert.assertNull(property.getInstallerValue());
        Assert.assertNull(property.getConfiguredValue());
        Assert.assertEquals(PropertyType.STRING, property.getType());
        Assert.assertFalse(property.isList());
        Assert.assertNull(property.getMin());
        Assert.assertNull(property.getMax());

        property = configParser.getProperties().get("multilineValueT");
        Assert.assertEquals("multilineValueT", property.getName());
        Assert.assertEquals("LAST GROUP", property.getGroup());
        Assert.assertEquals("Multiline value with a \\twist", property.getDescription());
        Assert.assertEquals("hello there", property.getDefaultValue());
        Assert.assertNull(property.getInstallerValue());
        Assert.assertNull(property.getConfiguredValue());
        Assert.assertEquals(PropertyType.STRING, property.getType());
        Assert.assertFalse(property.isList());
        Assert.assertNull(property.getMin());
        Assert.assertNull(property.getMax());

        property = configParser.getProperties().get("multilineValue3");
        Assert.assertEquals("multilineValue3", property.getName());
        Assert.assertEquals("LAST GROUP", property.getGroup());
        Assert.assertEquals("", property.getDescription());
        Assert.assertEquals("hello there, world", property.getDefaultValue());
        Assert.assertNull(property.getInstallerValue());
        Assert.assertNull(property.getConfiguredValue());
        Assert.assertEquals(PropertyType.STRING, property.getType());
        Assert.assertFalse(property.isList());
        Assert.assertNull(property.getMin());
        Assert.assertNull(property.getMax());

        property = configParser.getProperties().get("noMultiline");
        Assert.assertEquals("noMultiline", property.getName());
        Assert.assertEquals("LAST GROUP", property.getGroup());
        Assert.assertEquals("", property.getDescription());
        Assert.assertEquals("double\\", property.getDefaultValue());
        Assert.assertNull(property.getInstallerValue());
        Assert.assertNull(property.getConfiguredValue());
        Assert.assertEquals(PropertyType.STRING, property.getType());
        Assert.assertFalse(property.isList());
        Assert.assertNull(property.getMin());
        Assert.assertNull(property.getMax());

        property = configParser.getProperties().get("multilineOddBackslash");
        Assert.assertEquals("multilineOddBackslash", property.getName());
        Assert.assertEquals("LAST GROUP", property.getGroup());
        Assert.assertEquals("", property.getDescription());
        Assert.assertEquals("first\\\\and second", property.getDefaultValue());
        Assert.assertNull(property.getInstallerValue());
        Assert.assertNull(property.getConfiguredValue());
        Assert.assertEquals(PropertyType.STRING, property.getType());
        Assert.assertFalse(property.isList());
        Assert.assertNull(property.getMin());
        Assert.assertNull(property.getMax());

        Assert.assertEquals(23, configParser.getProperties().size());
    }

    @Ignore("Depends on static init, can only be single-run")
    @Test
    public void testCascade() {
        System.setProperty(Nxt.NXT_DEFAULT_PROPERTIES, "test/java/nxt/configuration/test-default.properties");
        System.setProperty(Nxt.NXT_INSTALLER_PROPERTIES, "test/java/nxt/configuration/test-installer.properties");
        System.setProperty(Nxt.NXT_PROPERTIES, "test/java/nxt/configuration/test.properties");
        ConfigParser configParser = ConfigParser.getInstance();

        Map<String, ConfigProperty> properties = configParser.getProperties();

        // symbols: D = value defined in nxt-default.properties, I = same for nxt-installer.properties, U = nxt.properties

        // property only on nxt-default.properties: default = D , installer = null, configured = null
        ConfigProperty property = properties.get("nxt.version");
        Assert.assertEquals(Nxt.VERSION, property.getDefaultValue());
        Assert.assertNull(property.getInstallerValue());
        Assert.assertNull(property.getConfiguredValue());

        //property modified on nxt-installer.properties: default = D , installer = I , configured = null
        property = properties.get("noMetadata");
        Assert.assertEquals("hello world", property.getDefaultValue());
        Assert.assertEquals("from installer", property.getInstallerValue());
        Assert.assertNull(property.getConfiguredValue());

        // property modified on all files: default = D , installer = I , configured = U
        property = properties.get("basic");
        Assert.assertEquals("¡hola!", property.getDefaultValue());
        Assert.assertEquals("installer", property.getInstallerValue());
        Assert.assertEquals("user", property.getConfiguredValue());

        // property on default and nxt.properties: default = D , installer = null, configured = U
        property = properties.get("listOfString");
        Assert.assertEquals("one;two", property.getDefaultValue());
        Assert.assertNull(property.getInstallerValue());
        Assert.assertEquals("three", property.getConfiguredValue());

        // empty on default, set on installer only: default = "" , installer = I , configured = null
        property = properties.get("noValue");
        Assert.assertEquals("", property.getDefaultValue());
        Assert.assertEquals("inst", property.getInstallerValue());
        Assert.assertNull(property.getConfiguredValue());

        // only set on nxt.properties: default = "" , configured = U
        property = properties.get("user");
        Assert.assertEquals("", property.getDefaultValue());
        Assert.assertNull(property.getInstallerValue());
        Assert.assertEquals("custom", property.getConfiguredValue());
    }
}
