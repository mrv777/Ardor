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

package nxt.util.security;

import java.security.Security;
import java.security.cert.CertificateFactory;

/**
 * Generate certificates based on blockchain transactions
 */
@SuppressWarnings("unused")
public class BlockchainCertificateFactory extends CertificateFactory {

    public BlockchainCertificateFactory() {
        super(new BlockchainCertificateFactorySpi(), Security.getProvider("Jelurida"), "Blockchain");
    }
}
