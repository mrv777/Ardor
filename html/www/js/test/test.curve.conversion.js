/******************************************************************************
 * Copyright © 2013-2016 The Nxt Core Developers.                             *
 * Copyright © 2016-2020 Jelurida IP B.V.                                     *
 *                                                                            *
 * See the LICENSE.txt file at the top-level directory of this distribution   *
 * for licensing information.                                                 *
 *                                                                            *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,*
 * no part of this software, including this file, may be copied, modified,    *
 * propagated, or distributed except according to the terms contained in the  *
 * LICENSE.txt file.                                                          *
 *                                                                            *
 * Removal or modification of this copyright notice is prohibited.            *
 *                                                                            *
 ******************************************************************************/

jQuery.t = function(text) {
    return text;
};

QUnit.module("nrs.curve.conversion");

const ED1 = "e30d5571d2c3f07691120792e5d3ead0f60f8a4504bf3c57f182f558e6243940";
const CURVE1 = "4d50b735fd45d22640675816f8ee2d56bf2caebebf76f7786183dcbd9022dd1f";
const ED2 = "f512b34816473c23e50ee5beeed8be5d9862de8e3c25f4221c8cd831fdd4760b";
const CURVE2 = "7f889c1a278462b06a67f054dae9ea2648863fe2345a7af1ab4c070c976b4931";
const ED3 = "ff7e3e79b77f0d605fe70d3fcd28e44242e710a1fe4be6837aef02c3e2be3f1a";
const ED4 = "ed7aae34c992646f077152e256e2ef0d134201941bf69ac717ac7fdb83a518fe";

QUnit.test("sampleOne", function (assert) {
    let ed25519PublicKeyBytes = converters.hexStringToByteArray(ED1);
    let x25519PublicKeyBytes = CurveConversion.ed25519ToCurve25519(new Uint8Array(ed25519PublicKeyBytes));
    assert.equal(converters.byteArrayToHexString(Array.from(x25519PublicKeyBytes)), CURVE1)
});

QUnit.test("sampleTwo", function (assert) {
    let ed25519PublicKeyBytes = converters.hexStringToByteArray(ED2);
    let x25519PublicKeyBytes = CurveConversion.ed25519ToCurve25519(new Uint8Array(ed25519PublicKeyBytes));
    assert.equal(converters.byteArrayToHexString(Array.from(x25519PublicKeyBytes)), CURVE2)
});

QUnit.test("sampleThree", function (assert) {
    let ed25519PublicKeyBytes = converters.hexStringToByteArray(ED3);
    let x25519PublicKeyBytes = CurveConversion.ed25519ToCurve25519(new Uint8Array(ed25519PublicKeyBytes));
    let expected = curve25519.keygen(converters.hexStringToByteArray("b8df6599aea1f04cbd88fafb916768b27c69466812e3753d7f5fcfd0227a9248")).p;
    assert.equal(converters.byteArrayToHexString(Array.from(x25519PublicKeyBytes)), converters.byteArrayToHexString(expected));
});

QUnit.test("sampleFour", function (assert) {
    let ed25519PublicKeyBytes = converters.hexStringToByteArray(ED4);
    let x25519PublicKeyBytes = CurveConversion.ed25519ToCurve25519(new Uint8Array(ed25519PublicKeyBytes));
    assert.equal(converters.byteArrayToHexString(Array.from(x25519PublicKeyBytes)), "b1076021f365fa77ea0c2fec3ab11cff21ce383a0b1e9d70fccd1d8e15807a68");
});