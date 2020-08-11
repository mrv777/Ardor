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

package nxt.http.configuration;

import nxt.BlockchainTest;
import nxt.Nxt;
import nxt.addons.JA;
import nxt.addons.JO;
import nxt.http.callers.SetConfigurationCall;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

public class SetConfigurationTest extends BlockchainTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void test() throws IOException {
        // create temp file and use as nxt.properties, so I can rewrite it
        File nxtProperties = temporaryFolder.newFile("nxt.properties");

        Properties properties = new Properties();
        properties.setProperty("key1", "value1");
        properties.setProperty("key2", "value2");
        FileWriter fileWriter = new FileWriter(nxtProperties);
        properties.store(fileWriter, "nxt.properties for SetConfigurationTest");
        fileWriter.close();

        System.setProperty(Nxt.NXT_PROPERTIES, nxtProperties.getAbsolutePath());

        JA configChanges = new JA();
        JO updatedValue = new JO();
        updatedValue.put("property", "key1");
        updatedValue.put("value", "newvalue1");
        configChanges.add(updatedValue);
        JO newValue = new JO();
        newValue.put("property", "key3");
        newValue.put("value", "value3");
        configChanges.add(newValue);
        JO removedValue = new JO();
        removedValue.put("property", "key2");
        removedValue.put("value", "");
        configChanges.add(removedValue);
        SetConfigurationCall.create().propertiesJSON(configChanges.toJSONArray().toJSONString()).build().invokeNoError();

        properties.clear();
        FileReader fileReader = new FileReader(nxtProperties);
        properties.load(fileReader);
        fileReader.close();

        Assert.assertEquals(2, properties.size());
        Assert.assertEquals("newvalue1", properties.getProperty("key1"));
        Assert.assertEquals("value3", properties.getProperty("key3"));
    }
}
