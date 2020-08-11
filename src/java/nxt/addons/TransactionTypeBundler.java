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

package nxt.addons;

import nxt.blockchain.Bundler;
import nxt.blockchain.ChildTransaction;
import nxt.blockchain.TransactionType;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TransactionTypeBundler implements Bundler.Filter {

    private List<TransactionType> types;

    @Override
    public boolean ok(Bundler bundler, ChildTransaction childTransaction) {
        return types.contains(childTransaction.getType());
    }

    @Override
    public String getName() {
        return "TransactionTypeBundler";
    }

    @Override
    public String getDescription() {
        return "Bundles only transaction types provided as parameter. The parameter must be comma separated string, " +
                "which contains <type>:<subtype> pairs specifying the \"type\" and \"subtype\" identifier of the" +
                "whitelisted transaction types. The \"type\" and \"subtype\" identifiers can be found in the " +
                "transactionSubTypes field of the getConstants result.";
    }

    @Override
    public void setParameter(String parameter) {
        types = Arrays.stream(parameter.split(",")).map(s -> {
            String[] pair = s.split(":");
            return TransactionType.findTransactionType(Byte.parseByte(pair[0].trim()), Byte.parseByte(pair[1].trim()));
        }).collect(Collectors.toList());

    }

    @Override
    public String getParameter() {
        return types.stream().map(transactionType ->
                transactionType.getType() + ":" + transactionType.getSubtype())
                .collect(Collectors.joining(","));
    }


}
