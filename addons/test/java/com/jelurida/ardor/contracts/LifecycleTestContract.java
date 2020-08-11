/*
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

package com.jelurida.ardor.contracts;

import nxt.addons.AbstractContract;
import nxt.addons.InitializationContext;
import nxt.addons.ShutdownContext;

public class LifecycleTestContract extends AbstractContract<Void,Void> {
    @Override
    public void init(InitializationContext context) {
        ContractLifecycleTest.initCounter.incrementAndGet();
    }

    @Override
    public void shutdown(ShutdownContext context) {
        ContractLifecycleTest.shutdownCounter.incrementAndGet();
    }
}
