/* global module,global */
/******************************************************************************
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

let KeyDerivation = function () {

    const nativeBigInt = (typeof global !== 'undefined') && global.BigInt || (typeof window !== 'undefined') && window.BigInt;
    const isBigIntSupported = typeof nativeBigInt === 'function';

    const PASSPHRASE_PREFIX = "mnemonic";
    const ROOT_CHAIN_CODE = "ed25519 seed";

    // The desktop wallet does not support the native BigInt object so it should never use the client side key derivation.
    // This code has to be protected otherwise the desktop wallet throws exception here and sporadically fails to load
    if (isBigIntSupported) {
        var TWO_POWER_31 = BigInt("2147483648"); // 2^31
        var TWO_POWER_32 = BigInt("4294967296"); // 2^32;
        var TWO_POWER_256 = BigInt("115792089237316195423570985008687907853269984665640564039457584007913129639936"); // 2^256
        var EIGHT = BigInt("8");
    }

    const ZEROS_64 = "0000000000000000000000000000000000000000000000000000000000000000";

    function mnemonicToSeed(mnemonic) {
        return mnemonicAndPassphraseToSeed(mnemonic, "");
    }

    function mnemonicAndPassphraseToSeed(mnemonic, passphrase) {
        return mnemonicAndSaltToSeed(mnemonic, PASSPHRASE_PREFIX + passphrase);
    }

    function mnemonicAndSaltToSeed(mnemonic, salt) {
        let seedWords = CryptoJS.PBKDF2(mnemonic, salt, {
            hasher: CryptoJS.algo.SHA512,
            keySize: 512 / 32,
            iterations: 2048
        });
        return converters.wordArrayToByteArrayImpl(seedWords);
    }

    function scalarMultiplicationCurve25519Base(k) {
        const n = byteArrayToBigint(k.slice());
        return Ed25519.BASE_POINT.multiply(n);
    }

    function clamp(k) {
        k[31] &= 0x7F;
        k[31] |= 0x40;
        k[0] &= 0xF8;
    }

    function switchEndian(b) {
        for (let i = 0; i < b.length / 2; i++) {
            let temp = b[i];
            b[i] = b[b.length - i - 1];
            b[b.length - i - 1] = temp;
        }
        return b;
    }

    function bigIntToByteArray(num, bytesLength) {
        let str = converters.bigIntToHexString(num);
        if (str.length < 2 * bytesLength) {
            str = ZEROS_64.substring(0, 2 * bytesLength - str.length) + str;
        }
        return switchEndian(converters.hexStringToByteArray(str));
    }

    function byteArrayToBigint(b) {
        return BigInt("0x" + converters.byteArrayToHexString(switchEndian(b.slice())));
    }

    function getSha256Commitment(message, key) {
        return converters.wordArrayToByteArrayImpl(CryptoJS.HmacSHA256(converters.byteArrayToWordArrayEx(message), converters.byteArrayToWordArrayEx(key)));
    }

    function getSha512Commitment(message, key) {
        return converters.wordArrayToByteArrayImpl(CryptoJS.HmacSHA512(converters.byteArrayToWordArrayEx(message), converters.byteArrayToWordArrayEx(key)));
    }

    function deriveMnemonic(path, mnemonic) {
        let seed = mnemonicToSeed(mnemonic);
        return deriveSeed(path, seed);
    }

    function deriveSeed(path, seed) {
        let node = getRootNode(seed);
        let pathSplit = path.split("/");
        for (let i = 0; i < pathSplit.length; i++) {
            let pathComponent = pathSplit[i];
            let childIndex;
            if (pathComponent.toLowerCase() === "m") {
                continue;
            }
            if (pathComponent.endsWith("'")) {
                childIndex = BigInt(pathComponent.substring(0, pathComponent.length - 1)) + TWO_POWER_31;
            } else {
                childIndex = BigInt(pathComponent);
            }
            node = deriveChildPrivateKey(node, childIndex);
        }
        return node;
    }

    function getRootNode(seed) {
        let rootChainCode = converters.stringToByteArray(ROOT_CHAIN_CODE);
        return getRootNodeImpl(seed, rootChainCode);
    }

    function getRootNodeImpl(seed, rootChainCode) {
        // root chain code
        let message = [0x01];
        message = message.concat(seed);
        let chainCode = getSha256Commitment(message, rootChainCode);

        // Calculate private key left and right
        let rootCommitment = getSha512Commitment(seed, rootChainCode);
        let keyLeft = rootCommitment.slice(0, 32);
        let keyRight = rootCommitment.slice(32, 64);
        while ((keyLeft[31] & 0x20) != 0) {
            rootCommitment = getSha512Commitment(rootCommitment, rootChainCode);
            keyLeft = rootCommitment.slice(0, 32);
            keyRight = rootCommitment.slice(32, 64);
        }
        clamp(keyLeft);

        // root public key
        let publicKeyPoint = scalarMultiplicationCurve25519Base(keyLeft);
        let publicKey = [].slice.call(publicKeyPoint.encode());
        let py = [].slice.call(publicKeyPoint.encode(true));
        return new Bip32Node(keyLeft, keyRight, publicKey, chainCode, py);
    }

    function deriveChildPrivateKey(node, childIndex) {
        if (node == null) {
            throw new Error("Node not specified");
        }
        if (childIndex < 0 || childIndex >= TWO_POWER_32) {
            throw new Error("Path component not in range " + childIndex);
        }
        let childIndexBytes = bigIntToByteArray(childIndex, 4);
        let childKeyCommitment;
        let bytes;
        if (childIndex < TWO_POWER_31) {
            // regular child
            bytes = [0x02];
            bytes = bytes.concat(node.getMasterPublicKey()).concat(childIndexBytes);
            childKeyCommitment = getSha512Commitment(bytes, node.getChainCode());
            bytes[0] = 0x03;
        } else {
            // hardened child
            bytes = [0x00];
            bytes = bytes.concat(node.getPrivateKeyLeft()).concat(node.getPrivateKeyRight()).concat(childIndexBytes);
            childKeyCommitment = getSha512Commitment(bytes, node.getChainCode());
            bytes[0] = 0x01;
        }
        let childKeyCommitmentLeft = childKeyCommitment.slice(0, 28);
        let childKeyCommitmentRight = childKeyCommitment.slice(32, 64);
        let chainCodeCommitment = getSha512Commitment(bytes, node.getChainCode());
        let chainCode = chainCodeCommitment.slice(32, 64);

        // compute private key left
        let childKeyCommitmentLeftNum = byteArrayToBigint(childKeyCommitmentLeft);
        let parentPrivateKeyLeftNum = byteArrayToBigint(node.getPrivateKeyLeft().slice());
        let privateKeyLeftNum = childKeyCommitmentLeftNum * EIGHT + parentPrivateKeyLeftNum;
        if (privateKeyLeftNum % Ed25519.PRIME_ORDER === BigInt(0)) {
            throw "Identity point was derived"; // I assume this is a theoretical case which should never happen in practice, let's see
        }
        let keyLeft = bigIntToByteArray(privateKeyLeftNum, 32);

        // compute private key right
        let childKeyCommitmentRightNum = byteArrayToBigint(childKeyCommitmentRight);
        let parentPrivateKeyRightNum = byteArrayToBigint(node.getPrivateKeyRight().slice());
        let privateKeyRightNum = (childKeyCommitmentRightNum + parentPrivateKeyRightNum) % TWO_POWER_256;
        let keyRight = bigIntToByteArray(privateKeyRightNum, 32);

        // compute public key
        let publicKeyPoint = scalarMultiplicationCurve25519Base(keyLeft);
        return new Bip32Node(keyLeft, keyRight, [].slice.call(publicKeyPoint.encode()), chainCode, [].slice.call(publicKeyPoint.encode(true)));
    }

    function deriveChildPublicKeyFromNode(node, childIndex) {
        return deriveChildPublicKeyFromParent(node.getMasterPublicKey(), node.getChainCode(), childIndex);
    }

    function deriveChildPublicKeyFromParent(parentPublicKey, chainCode, childIndex) {
        if (childIndex < 0 || childIndex > TWO_POWER_31) {
            throw "child index out of range, only non-hardened paths are supported";
        }
        let childIndexBytes = bigIntToByteArray(childIndex, 4);
        let message = [0x02];
        message = message.concat(parentPublicKey).concat(childIndexBytes);
        let childCommitment = getSha512Commitment(message, chainCode);
        message[0] = 0x03;
        let childChainCode = getSha512Commitment(message, chainCode).slice(32, 64);

        // Represent the parent public key as point
        let parentPublicKeyPoint = Ed25519.Point.fromHex(converters.byteArrayToHexString(parentPublicKey));

        // Perform scalar multiplication z28 * 8 * GENERATOR
        let zl = Ed25519.BASE_POINT.multiply(EIGHT);
        let z28bytes = childCommitment.slice(0, 28);
        for (let i = 0; i < 4; i++) {
            z28bytes.push(0x00);
        }
        let normalizedChildCommitment = zl.multiply(byteArrayToBigint(z28bytes));

        // Add the public key to the calculated point
        let publicKeyPoint = parentPublicKeyPoint.add(normalizedChildCommitment);
        return new Bip32Node(null, null, [].slice.call(publicKeyPoint.encode()), childChainCode, [].slice.call(publicKeyPoint.encode(true)));
    }

    /**
     * Returns a Bip32Node from a serialized master public key and chain code along with the child index.
     *
     * @param serializedMasterPublicKey the serialized master public key as byte array
     * @param childIndex                the child index as integer
     */
    function deriveChildPublicKeyFromSerializedMasterPublicKey(serializedMasterPublicKey, childIndex) {
        if (!isValidSerializedMasterPublicKey(serializedMasterPublicKey)) {
            throw new Error("Invalid serialized master public key");
        }
        return deriveChildPublicKeyFromParent(
            serializedMasterPublicKey.slice(0, 32), serializedMasterPublicKey.slice(32, 64), childIndex);
    }

    /**
     * Returns a byte array with the computed serializedMasterPublicKey.
     *
     * 68 bytes: 32 master public key + 32 chain code + 4 CRC32
     *
     * @param masterPublicKey the master public key as byte array
     * @param chainCode       the chain code as byte array
     */
    function computeSerializedMasterPublicKey(masterPublicKey, chainCode) {
        if (!Array.isArray(masterPublicKey) || masterPublicKey.length !== 32) {
            throw new Error("Invalid master public key");
        }
        if (!Array.isArray(chainCode) || chainCode.length !== 32) {
            throw new Error("Invalid chain code");
        }
        const checksum = CRC32.buf(chainCode, CRC32.buf(masterPublicKey));
        return masterPublicKey.concat(chainCode, converters.int32ToBytes(checksum));
    }

    /**
     * Returns true if the first parameter is byte array containing a valid serialized master public key.
     *
     * @param serializedMasterPublicKey the serialized master public key as byte array
     */
    function isValidSerializedMasterPublicKey(serializedMasterPublicKey) {
        if (!Array.isArray(serializedMasterPublicKey) || serializedMasterPublicKey.length !== 68) {
            return false;
        }
        const masterPublicKey = serializedMasterPublicKey.slice(0, 32);
        const chainCode = serializedMasterPublicKey.slice(32, 64);
        const computed = computeSerializedMasterPublicKey(masterPublicKey, chainCode);
        for(let i = 64; i < 68; i++) {
            if (computed[i] !== serializedMasterPublicKey[i]) {
                return false;
            }
        }
        return true;
    }

    const Bip32Node = function(privateKeyLeft, privateKeyRight, masterPublicKey, chainCode, masterPublicKeyY, publicKey) {
        this.privateKeyLeft = privateKeyLeft;
        this.privateKeyRight = privateKeyRight;
        this.masterPublicKey = masterPublicKey;
        this.chainCode = chainCode;
        if (publicKey !== undefined) {
            this.publicKey = publicKey;
        } else {
            this.publicKey = CurveConversion.ed25519ToCurve25519(masterPublicKeyY);
        }
    };

    Bip32Node.prototype.getPrivateKeyLeft = function() {
        return this.privateKeyLeft;
    };

    Bip32Node.prototype.getPrivateKeyRight = function() {
        return this.privateKeyRight;
    };

    Bip32Node.prototype.getMasterPublicKey = function() {
        return this.masterPublicKey;
    };

    Bip32Node.prototype.getChainCode = function() {
        return this.chainCode;
    };

    Bip32Node.prototype.getPublicKey = function() {
        return this.publicKey;
    };

    Bip32Node.prototype.getSerializedMasterPublicKey = function () {
        return computeSerializedMasterPublicKey(this.masterPublicKey, this.chainCode);
    };

    Bip32Node.prototype.toString = function() {
        return "Bip32Node{" +
            "privateKeyLeft=" + converters.byteArrayToHexString(this.privateKeyLeft) +
            ", privateKeyRight=" + converters.byteArrayToHexString(this.privateKeyRight) +
            ", masterPublicKey=" + converters.byteArrayToHexString(this.masterPublicKey) +
            ", chainCode=" + converters.byteArrayToHexString(this.chainCode) +
            ", publicKey=" + converters.byteArrayToHexString(this.publicKey) +
            ", serializedMasterPublicKey=" + converters.byteArrayToHexString(this.getSerializedMasterPublicKey()) +
            '}';
    };

    // noinspection JSUnusedGlobalSymbols
    this.Bip32Node = Bip32Node;

    return {
        deriveChildPrivateKey: deriveChildPrivateKey,
        deriveChildPublicKeyFromNode: deriveChildPublicKeyFromNode,
        deriveChildPublicKeyFromParent: deriveChildPublicKeyFromParent,
        deriveChildPublicKeyFromSerializedMasterPublicKey: deriveChildPublicKeyFromSerializedMasterPublicKey,
        deriveMnemonic: deriveMnemonic,
        deriveSeed: deriveSeed,
        mnemonicToSeed: mnemonicToSeed,
        mnemonicAndPassphraseToSeed: mnemonicAndPassphraseToSeed,
        scalarMultiplicationCurve25519Base: scalarMultiplicationCurve25519Base,
        computeSerializedMasterPublicKey: computeSerializedMasterPublicKey,
        isValidSerializedMasterPublicKey: isValidSerializedMasterPublicKey,
        createBipNodeFromResponse: function(response) {
            return new Bip32Node(
                converters.hexStringToByteArray(response.privateKey),
                converters.hexStringToByteArray(response.privateKeyRight),
                converters.hexStringToByteArray(response.masterPublicKey),
                converters.hexStringToByteArray(response.chainCode),
                null,
                converters.hexStringToByteArray(response.publicKey))
        }
    };

}();

if (isNode) {
    module.exports = KeyDerivation;
}