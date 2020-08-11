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

package nxt.addons;

import nxt.configuration.ConfigPropertyBuilder;
import nxt.http.APIServlet;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public interface AddOn {

    default void init() {}

    /**
     * This method returns the configuration metadata for any properties used by the add-on.
     *
     * It must be an idempotent method in the sense that no side effects should happen on invocation. It should work
     * independently of initialization (init() method).
     *
     * @return the configuration metadata for this add-on
     */
    default Collection<ConfigPropertyBuilder> getConfigProperties() {
        return Collections.emptyList();
    }

    default void shutdown() {}

    default APIServlet.APIRequestHandler getAPIRequestHandler() {
        return null;
    }

    default String getAPIRequestType() {
        return null;
    }

    default Map<String, APIServlet.APIRequestHandler> getAPIRequests() { return null; }

}
