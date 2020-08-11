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

package nxt.crypto;

import nxt.util.Convert;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Random;

public class CurveConversionTest {

    private static final String ED1 = "e30d5571d2c3f07691120792e5d3ead0f60f8a4504bf3c57f182f558e6243940";
    private static final String CURVE1 = "4d50b735fd45d22640675816f8ee2d56bf2caebebf76f7786183dcbd9022dd1f";
    private static final String ED2 = "f512b34816473c23e50ee5beeed8be5d9862de8e3c25f4221c8cd831fdd4760b";
    private static final String CURVE2 = "7f889c1a278462b06a67f054dae9ea2648863fe2345a7af1ab4c070c976b4931";
    private static final String ED3 = "ff7e3e79b77f0d605fe70d3fcd28e44242e710a1fe4be6837aef02c3e2be3f1a";
    private static final String ED4 = "ed7aae34c992646f077152e256e2ef0d134201941bf69ac717ac7fdb83a518fe";

    @Test
    public void sampleOne() {
        byte[] ed25519PublicKeyBytes = Convert.parseHexString(ED1);
        byte[] x25519PublicKeyBytes = CurveConversion.ed25519ToCurve25519(ed25519PublicKeyBytes);
        Assert.assertArrayEquals(Convert.parseHexString(CURVE1), x25519PublicKeyBytes);
    }

    @Test
    public void sampleTwo() {
        byte[] ed25519PublicKeyBytes = Convert.parseHexString(ED2);
        byte[] x25519PublicKeyBytes = CurveConversion.ed25519ToCurve25519(ed25519PublicKeyBytes);
        Assert.assertArrayEquals(Convert.parseHexString(CURVE2), x25519PublicKeyBytes);
    }

    @Test
    public void sampleThree() {
        byte[] ed25519PublicKeyBytes = Convert.parseHexString(ED3);
        byte[] x25519PublicKeyBytes = CurveConversion.ed25519ToCurve25519(ed25519PublicKeyBytes);
        byte[] expected = new byte[32];
        Curve25519.keygen(expected, null, Convert.parseHexString("b8df6599aea1f04cbd88fafb916768b27c69466812e3753d7f5fcfd0227a9248"));
        Assert.assertEquals(Convert.toHexString(expected), Convert.toHexString(x25519PublicKeyBytes));
    }

    @Test
    public void sample4() {
        byte[] ed25519PublicKeyBytes = Convert.parseHexString(ED4);
        byte[] x25519PublicKeyBytes = CurveConversion.ed25519ToCurve25519(ed25519PublicKeyBytes);
        byte[] expected = new byte[32];
        Curve25519.keygen(expected, null, Convert.parseHexString("b8df6599aea1f04cbd88fafb916768b27c69466812e3753d7f5fcfd0227a9248"));
        Assert.assertArrayEquals(Convert.parseHexString("b1076021f365fa77ea0c2fec3ab11cff21ce383a0b1e9d70fccd1d8e15807a68"), x25519PublicKeyBytes);
    }

    /**
     * Compare the results returned by the Java code to the results returned by the original C code we use in the ledger app.
     * This C code origin is https://www.dlbeer.co.nz/oss/c25519.html
     */
    @Test
    public void cToJavaComparison() {
        Assume.assumeTrue("only runs on Windows", System.getProperty("os.name").startsWith("Windows"));
        Random random = new Random();
        for (int i=1; i<10; i++) {
            byte[] seed = new byte[32];
            random.nextBytes(seed);
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command("./test/c/curveConversion.exe", Convert.toHexString(seed));
            Process process;
            try {
                process = processBuilder.start();
                BufferedReader br=new BufferedReader(new InputStreamReader(process.getInputStream()));
                String cResult = null;
                String line;
                while((line=br.readLine()) !=null) {
                    cResult = line;
                }
                String javaResult = Convert.toHexString(CurveConversion.ed25519ToCurve25519(seed));
                Assert.assertEquals("failed for seed " + Convert.toHexString(seed), cResult, javaResult.toUpperCase());
            } catch (IOException e) {
                Assert.fail(e.getMessage());
            }
        }
    }
}
