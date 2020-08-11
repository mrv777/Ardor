/*
 * Copyright © 2020 Jelurida IP B.V.
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

/**
 * This contract confirms fix to ContractLoader which allows usage of sub-class methods accepting sub-class instances
 * as parameters.
 */
public class ContractWithInnerClassParameter extends AbstractContract<Object, Object> {
    private IMethod a;

    @Override
    public void init(InitializationContext context) {
        a = new MethodWithParam();
    }

    public static class Param {
    }

    public interface IMethod {
        void doSomething(Param param);
    }

    public static class MethodWithParam implements IMethod {
        @Override
        public void doSomething(Param param) {
        }
    }
}
