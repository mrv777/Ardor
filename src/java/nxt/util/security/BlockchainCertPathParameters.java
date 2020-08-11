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

import java.security.cert.CertPathParameters;

public class BlockchainCertPathParameters implements CertPathParameters {

    private final BlockchainCertificate certificate;

    public BlockchainCertPathParameters(BlockchainCertificate certificate) {
        this.certificate = certificate;
    }

    public BlockchainCertificate getCertificate() {
        return certificate;
    }

    @Override
    public Object clone() {
        try {
            return certificate.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
