/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2018 Jelurida IP B.V.
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

package nxt.util;

import nxt.Tester;
import nxt.addons.JO;
import org.json.simple.JSONObject;
import org.junit.Assert;

import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class JSONAssert {
    private final JSONObject obj;

    public JSONAssert(JSONObject obj) {
        this.obj = obj;
    }

    public JSONAssert(JO jo) {
        this(jo.toJSONObject());
    }

    public JSONAssert subObj(String key) {
        return new JSONAssert(object(key, JSONObject.class));
    }

    public long unsignedLong(String key) {
        return Long.parseUnsignedLong(str(key));
    }

    public String str(String key) {
        return object(key, String.class);
    }

    public Boolean bool(String key) {
        return object(key, Boolean.class);
    }

    public <T> T object(String key, Class<T> clazz) {
        Object o = getObject(key);
        if (clazz.isInstance(o)) {
            return clazz.cast(o);
        }
        throw new AssertionError("Type of " + key + " is not " + clazz.getName() + ", but " + o.getClass());
    }

    public String fullHash() {
        return str("fullHash");
    }

    public String id() {
        return Tester.hexFullHashToStringId(fullHash());
    }

    public long integer(String key) {
        return object(key, Long.class);
    }

    public long amount(String key) {
        try {
            return Long.parseLong(str(key));
        } catch (AssertionError e) {
            throw new AssertionError("Type of " + key + " is not string. Amounts must be longs printed as strings");
        }
    }

    public <T> List<T> array(String key, Class<T> elementClass) {
        return (List<T>) object(key, List.class);
    }

    public List<JSONObject> array(String key) {
        return array(key, JSONObject.class);
    }

    public JSONObject getJson() {
        return obj;
    }

    private Object getObject(String key) {
        Object o = obj.get(key);
        Assert.assertNotNull("Missing " + key + " in " + obj.toJSONString(), o);
        return o;
    }

    public JSONAssert assertSuccess() {
        assertNull(obj.get("errorDescription"));
        assertNull(obj.get("errorCode"));
        return this;
    }

    public JSONAssert assertError() {
        assertNotNull(obj.get("errorDescription"));
        assertNotNull(obj.get("errorCode"));
        return this;
    }
}
