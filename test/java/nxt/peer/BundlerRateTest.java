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
package nxt.peer;

import nxt.BlockchainTest;
import nxt.blockchain.Chain;
import nxt.blockchain.ChildChain;
import nxt.blockchain.FxtChain;
import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;

public class BundlerRateTest {
    @Test
    public void testUnknownChain() {
        BundlerRate sourceRate = new BundlerRate(ChildChain.GPS, 123456789L, ChildChain.GPS.ONE_COIN,
                FxtChain.FXT.ONE_COIN * 100);
        ByteBuffer byteBuffer = ByteBuffer.allocate(sourceRate.getLength());
        sourceRate.getBytes(byteBuffer);
        byteBuffer.rewind();
        int maxChainId = ChildChain.getAll().stream().map(Chain::getId).max(Integer::compareTo).orElse(0);
        byteBuffer.putInt(maxChainId + 1);
        byteBuffer.rewind();
        BundlerRate parsedRate = new BundlerRate(byteBuffer);
        Assert.assertNull(parsedRate.getChain());
    }

}