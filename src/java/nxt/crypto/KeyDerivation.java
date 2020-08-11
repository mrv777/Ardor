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

import net.i2p.crypto.eddsa.Utils;
import net.i2p.crypto.eddsa.math.Curve;
import net.i2p.crypto.eddsa.math.FieldElement;
import net.i2p.crypto.eddsa.math.GroupElement;
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable;
import nxt.util.Bip32Path;
import nxt.util.Convert;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

public class KeyDerivation {

    private static final String PASSPHRASE_PREFIX = "mnemonic";
    private static final String ROOT_CHAIN_CODE = "ed25519 seed";
    private static final Curve CURVE_ED_25519 = EdDSANamedCurveTable.getByName(EdDSANamedCurveTable.ED_25519).getCurve();
    static final GroupElement GENERATOR = new GroupElement(CURVE_ED_25519, Convert.switchEndian(Utils.hexToBytes("6666666666666666666666666666666666666666666666666666666666666658")), true);
    private static final BigInteger GROUP_ORDER = BigInteger.ONE.shiftLeft(252).add(new BigInteger("27742317777372353535851937790883648493"));

    private static final BigInteger TWO_POWER_256 = new BigInteger("2").pow(256);
    private static final BigInteger EIGHT = new BigInteger("8");
    static final byte[] EIGHT_BYTES;
    static {
        EIGHT_BYTES = new byte[32];
        System.arraycopy(EIGHT.toByteArray(), 0, EIGHT_BYTES, 0, EIGHT.toByteArray().length);
    }
    private static final GroupElement GENERATOR_MUL_8_POINT = CURVE_ED_25519.createPoint(GENERATOR.scalarMultiply(EIGHT_BYTES).toByteArray(), true);
    private static final String ZEROS_64 = "0000000000000000000000000000000000000000000000000000000000000000";

    /**
     * When encoding a point on the ed25519 curve there are two options:
     * Only use the Y coordinate - we need this to later transform the ed25519 Y point into curve25519 X point which represents the Ardor public key
     * Encode the sign of the X coordinate into the Y coordinate. We need this to generate a valid ed25519 public which we later use as a parent key
     * from which to derive more child public keys.
     * The method is inspired by the GroupElement toByteArray() with an option not to encode the X coordinate.
     * @param point the ed25519 curve to encode
     * @param encodeXsignIntoY if to encode the X coordinate or not
     * @return the byte representation of the point
     */
    private static byte[] pointToByteArray(GroupElement point, boolean encodeXsignIntoY) {
        switch (point.getRepresentation()) {
            case P2:
            case P3:
                FieldElement recip = point.getZ().invert();
                FieldElement x = point.getX().multiply(recip);
                FieldElement y = point.getY().multiply(recip);
                byte[] s = y.toByteArray();
                if (encodeXsignIntoY) {
                    s[s.length-1] |= (x.isNegative() ? (byte) 0x80 : 0);
                }
                return s;
            default:
                return pointToByteArray(point.toP2(), encodeXsignIntoY);
        }
    }

    private static byte[] bigIntegerToBytes(BigInteger bigInteger) {
        String str = bigInteger.toString(16);
        if (str.length() < 64) {
            str = ZEROS_64.substring(0, 64 - str.length()) + str;
        }
        byte[] bigEndianBytes = Convert.parseHexString(str);
        return Convert.switchEndian(bigEndianBytes);
    }

    private static BigInteger bytesToBigInteger(byte[] bytes) {
        return new BigInteger(Convert.toHexString(Convert.switchEndian(Arrays.copyOf(bytes, bytes.length))), 16);
    }

    /**
     * @param path bip32 path
     * @param mnemonic bip39 words
     * @return derived seed
     */
    public static Bip32Node deriveMnemonic(String path, String mnemonic) {
        byte[] seed = mnemonicToSeed(mnemonic);
        return deriveSeed(path, seed);
    }

    public static byte[] mnemonicToSeed(String mnemonic) {
        return mnemonicToSeed(mnemonic, "");
    }

    public static byte[] mnemonicToSeed(String mnemonic, String passphrase) {
        // Salt is a mandatory parameter for PBKDF2 key generation.
        // To generate the salt we append the passphrase to the default prefix used by BIP32
        byte[] salt = (PASSPHRASE_PREFIX + passphrase).getBytes(StandardCharsets.UTF_8);
        return mnemonicToSeed(mnemonic, salt);
    }

    private static byte[] mnemonicToSeed(String mnemonic, byte[] salt) {
        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
            PBEKeySpec spec = new PBEKeySpec(mnemonic.toCharArray(), salt, 2048, 512);
            SecretKey key = skf.generateSecret(spec);
            return key.getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * @param path bip32 string path to derive (eg 42'/1/2)
     * @param seed 512 bits seed
     * @return bip32 node representing the public/private key pair and chain code
     */
    public static Bip32Node deriveSeed(String path, byte[] seed) {
        Bip32Node node = getRootNode(seed);
        int[] pathComponents = Bip32Path.bip32StrToPath(path);
        for (int pathComponent : pathComponents) {
            node = deriveChildPrivateKey(node, pathComponent);
        }
        return node;
    }

    public static Bip32Node deriveChildPublicKey(PublicKeyDerivationInfo derivationInfo) {
        return deriveChildPublicKey(derivationInfo.getMasterPublicKey(), derivationInfo.getChainCode(), derivationInfo.getChildIndex());
    }

    public static Bip32Node deriveChildPublicKey(Bip32Node node, int childIndex) {
        return deriveChildPublicKey(node.getMasterPublicKey(), node.getChainCode(), childIndex);
    }

    public static Bip32Node deriveChildPublicKey(SerializedMasterPublicKey serializedMasterPublicKey, int childIndex) {
        return deriveChildPublicKey(serializedMasterPublicKey.getMasterPublicKey(), serializedMasterPublicKey.getChainCode(), childIndex);
    }

    public static Bip32Node deriveChildPublicKey(byte[] parentPublicKey, byte[] chainCode, int childIndex) {
        if (childIndex < 0) {
            throw new IllegalArgumentException("child index out of range, only non-hardened paths are supported");
        }

        // Represent the index as 4 bytes little endian
        ByteBuffer childBuffer = ByteBuffer.allocate(4);
        childBuffer.order(ByteOrder.LITTLE_ENDIAN);
        childBuffer.putInt(childIndex);
        byte[] childIndexBytes = childBuffer.array();

        // Calculate child commitment
        ByteBuffer messageBuffer = ByteBuffer.allocate(1 + parentPublicKey.length + 4);
        messageBuffer.put((byte)0x02);
        messageBuffer.put(parentPublicKey);
        messageBuffer.put(childIndexBytes);
        byte[] childCommitment = Crypto.getSha512Commitment(messageBuffer.array(), chainCode);

        // Calculate the chain code
        messageBuffer = ByteBuffer.allocate(1 + parentPublicKey.length + 4);
        messageBuffer.put((byte)0x03);
        messageBuffer.put(parentPublicKey);
        messageBuffer.put(childIndexBytes);
        byte[] childChainCode = Arrays.copyOfRange(Crypto.getSha512Commitment(messageBuffer.array(), chainCode), 32, 64);

        // Represent the parent public key as point
        GroupElement parentPublicKeyPoint = CURVE_ED_25519.createPoint(parentPublicKey, true);

        // Perform scalar multiplication z28 * 8 * GENERATOR
        byte[] z28 = new byte[32];
        System.arraycopy(childCommitment, 0, z28, 0, 28);
        GroupElement normalizedChildCommitment = GENERATOR_MUL_8_POINT.scalarMultiply(z28);

        // Add the public key to the calculated point
        GroupElement publicKeyPoint = parentPublicKeyPoint.add(normalizedChildCommitment.toCached());
        byte[] publicKey = pointToByteArray(publicKeyPoint, true);
        byte[] py = pointToByteArray(publicKeyPoint, false);
        return new Bip32Node(publicKey, childChainCode, py);
    }


    /**
     *  PROCESS:
     *  1. compute c = HMAC-SHA256(key=seedkey,0x01 || Data = S)
     *  2. compute I = HMAC-SHA512(key=seedkey, Data=S)
     *  3. split I = into two sequences of 32-bytes sequence kL,Kr
     *  4. if the third highest bit of the last byte ok kL is not zero:
     *      S = I
     *      goto step 1
     *  5. Set the bits in kL as follows:
     *      - the lowest 3 bits of the first byte of kL of are cleared
     *      - the highest bit of the last byte is cleared
     *      - the second highest bit of the last byte is set
     *  6. return (kL,kR), c
     *
     *  Note: the seed key is hardcoded, need to understand the implications
     *
     * @param seed the 512 bits seed from BIP39/BIP32
     * @return derived node
     */
    private static Bip32Node getRootNode(byte[] seed) {
        byte[] rootChainCode = ROOT_CHAIN_CODE.getBytes(StandardCharsets.UTF_8);
        return getRootNode(seed, rootChainCode);
    }

    private static Bip32Node getRootNode(byte[] seed, byte[] rootChainCode) {
        // root chain code
        byte[] message = new byte[seed.length + 1];
        message[0] = 0x01;
        System.arraycopy(seed, 0, message, 1, seed.length);
        byte[] chainCode = Crypto.getSha256Commitment(message, rootChainCode);

        // Calculate private key left and right
        byte[] rootCommitment = Crypto.getSha512Commitment(seed, rootChainCode);
        byte[] keyLeft = Arrays.copyOfRange(rootCommitment, 0, 32);
        byte[] keyRight = Arrays.copyOfRange(rootCommitment, 32, 64);
        while ((keyLeft[31] & 0x20) != 0) {
            rootCommitment = Crypto.getSha512Commitment(rootCommitment, rootChainCode);
            keyLeft = Arrays.copyOfRange(rootCommitment, 0, 32);
            keyRight = Arrays.copyOfRange(rootCommitment, 32, 64);
        }
        Crypto.clamp(keyLeft);

        // root public key
        GroupElement publicKeyPoint = GENERATOR.scalarMultiply(keyLeft);
        byte[] publicKey = pointToByteArray(publicKeyPoint, true);
        byte[] py = pointToByteArray(publicKeyPoint, false);
        return new Bip32Node(keyLeft, keyRight, publicKey, chainCode, py);
    }

    /**
     *  INPUT:
     *  <ul>
     *  <li>(kL,kR): 64 bytes private eddsa key</li>
     *  <li>A: 32 bytes public key (y coordinate only), optional as A = kR.G (y coordinate only)</li>
     *  <li>c: 32 bytes chain code</li>
     *  <li>i: child index to compute (hardened if &gt;= 0x80000000)</li>
     *  </ul>
     *
     *  OUTPUT:
     *  <ul>
     *  <li>(kL_i,kR_i): 64 bytes ith-child private eddsa key</li>
     *  <li>A_i: 32 bytes ith-child public key, A_i = kR_i.G (y coordinatte only)</li>
     *  <li>c_i: 32 bytes ith-child chain code</li>
     *  </ul>
     *
     *  PROCESS:
     *
     *  1. encode i 4-bytes little endian, il = encode_U32LE(i) <br>
     *  2. if i is less than 2^31<br>
     *     - compute Z   = HMAC-SHA512(key=c, Data=0x02 | A | il )<br>
     *     - compute c_  = HMAC-SHA512(key=c, Data=0x03 | A | il )<br>
     *     else<br>
     *     - compute Z   = HMAC-SHA512(key=c, Data=0x00 | kL | kR | il )<br>
     *     - compute c_  = HMAC-SHA512(key=c, Data=0x01 | kL | kR | il )<br>
     *  3. ci = lowest_32bytes(c_)<br>
     *  4. set ZL = highest_28bytes(Z)<br>
     *     set ZR = lowest_32bytes(Z)<br>
     *  5. compute kL_i:<br>
     *     zl_  = LEBytes_to_int(ZL)<br>
     *     kL_  = LEBytes_to_int(kL)<br>
     *     kLi_ = zl_*8 + kL_<br>
     *     if kLi_ % order == 0: child does not exist<br>
     *     kL_i = int_to_LEBytes(kLi_)<br>
     *  6. compute kR_i<br>
     *     zr_  = LEBytes_to_int(ZR)<br>
     *     kR_  = LEBytes_to_int(kR)<br>
     *     kRi_ = (zr_ + kRn_) % 2^256<br>
     *     kR_i = int_to_LEBytes(kRi_)<br>
     *  7. compute A<br>
     *     A = kLi_.G<br>
     *  8. return (kL_i,kR_i), A_i, c<br>
     *
     * @param node node
     * @param childIndex child index
     * @return derived node
     */
    public static Bip32Node deriveChildPrivateKey(Bip32Node node, int childIndex) {
        byte[] childIndexBytes = Convert.intToLittleEndian(childIndex);

        byte[] childKeyCommitment;
        byte[] bytes;
        if (childIndex >= 0) {
            // regular child
            ByteBuffer b = ByteBuffer.allocate(1 + node.getMasterPublicKey().length + 4);
            b.put((byte) 0x02);
            b.put(node.getMasterPublicKey());
            b.put(childIndexBytes);
            bytes = b.array();
            childKeyCommitment = Crypto.getSha512Commitment(bytes, node.getChainCode());
            bytes[0] = (byte) 0x03;
        } else {
            // hardened child
            ByteBuffer b = ByteBuffer.allocate(1 + node.getPrivateKeyLeft().length + node.getPrivateKeyRight().length + 4);
            b.put((byte) 0x00);
            b.put(node.getPrivateKeyLeft());
            b.put(node.getPrivateKeyRight());
            b.put(childIndexBytes);
            bytes = b.array();
            childKeyCommitment = Crypto.getSha512Commitment(bytes, node.getChainCode());
            bytes[0] = (byte) 0x01;
        }
        byte[] chainCodeCommitment = Crypto.getSha512Commitment(bytes, node.getChainCode());
        byte[] chainCode = Arrays.copyOfRange(chainCodeCommitment, 32, 64);

        byte[] childKeyCommitmentLeft = Arrays.copyOfRange(childKeyCommitment, 0, 28);
        byte[] childKeyCommitmentRight = Arrays.copyOfRange(childKeyCommitment, 32, 64);

        // compute private key left
        BigInteger childKeyCommitmentLeftNum = bytesToBigInteger(childKeyCommitmentLeft);
        BigInteger parentPrivateKeyLeftNum = bytesToBigInteger(node.getPrivateKeyLeft());
        BigInteger privateKeyLeftNum = childKeyCommitmentLeftNum.multiply(EIGHT).add(parentPrivateKeyLeftNum);
        if (privateKeyLeftNum.mod(GROUP_ORDER).equals(BigInteger.ZERO)) {
            throw new IllegalStateException("Identity point was derived"); // I assume this is a theoretical case which should never happen in practice, let's see
        }
        byte[] keyLeft = bigIntegerToBytes(privateKeyLeftNum);

        // compute private key right
        BigInteger childKeyCommitmentRightNum = bytesToBigInteger(childKeyCommitmentRight);
        BigInteger parentPrivateKeyRightNum = bytesToBigInteger(node.getPrivateKeyRight());
        BigInteger privateKeyRightNum = childKeyCommitmentRightNum.add(parentPrivateKeyRightNum).mod(TWO_POWER_256);
        byte[] keyRight = bigIntegerToBytes(privateKeyRightNum);

        // compute public key
        GroupElement publicKeyPoint = GENERATOR.scalarMultiply(keyLeft);
        byte[] publicKey = pointToByteArray(publicKeyPoint, true);
        byte[] py = pointToByteArray(publicKeyPoint, false);
        return new Bip32Node(keyLeft, keyRight, publicKey, chainCode, py);
    }

    /**
     * Represents a node in the Bip32 derivation tree.
     * Use this node to:
     * Obtain the Curve25519 private/public key pair for signing and encryption
     * Derive child nodes with both private and public data
     * Derive child nodes with only public data
     *
     * Security considerations:
     * Given a master public key and chain code of a non-hardened parent node and any child private key, anyone can easily reproduce the parent node
     * private key and all child private keys. Therefore maintain the pair master public key + chain code private
     */
    public static class Bip32Node {
        private final byte[] privateKeyLeft;
        private final byte[] privateKeyRight;
        private final byte[] masterPublicKey;
        private final byte[] serializedMasterPublicKey;
        private final byte[] chainCode;
        private final byte[] publicKey;

        Bip32Node(byte[] publicKey, byte[] chainCode, byte[] py) {
            this(null, null, publicKey, chainCode, py);
        }

        public Bip32Node(byte[] privateKeyLeft, byte[] privateKeyRight, byte[] masterPublicKey, byte[] chainCode, byte[] masterPublicKeyY) {
            if (privateKeyLeft != null && privateKeyLeft.length != 32 && privateKeyRight != null && privateKeyRight.length < 32) {
                throw new IllegalArgumentException(String.format("Incorrect private key length %s,%s", Convert.toHexString(privateKeyLeft), Convert.toHexString(privateKeyRight)));
            }
            if (masterPublicKey.length != 32 || chainCode.length != 32 || masterPublicKeyY != null && masterPublicKeyY.length != 32) {
                throw new IllegalArgumentException(String.format("Incorrect public key length %d %d %d", masterPublicKey.length, chainCode.length, masterPublicKeyY.length));
            }
            this.privateKeyLeft = privateKeyLeft;
            this.privateKeyRight = privateKeyRight;
            this.masterPublicKey = masterPublicKey;
            this.chainCode = chainCode;
            this.publicKey = masterPublicKeyY != null ? CurveConversion.ed25519ToCurve25519(masterPublicKeyY) : null;
            this.serializedMasterPublicKey = new SerializedMasterPublicKey(masterPublicKey, chainCode).getSerializedMasterPublicKey();
        }

        /**
         * @return the ed25519/curve25519 private key associated with the node as used for signing and encryption
         */
        public byte[] getPrivateKeyLeft() {
            return privateKeyLeft;
        }

        /**
         * @return the right side of the key is used for hardened derivation of child nodes
         */
        public byte[] getPrivateKeyRight() {
            return privateKeyRight;
        }

        /**
         * @return the ed25519 public key used only for non-hardened child key derivation
         */
        public byte[] getMasterPublicKey() {
            return masterPublicKey;
        }

        /**
         * @return the 68 bytes combined master public key + chain code + CRC32 checksum
         */
        public byte[] getSerializedMasterPublicKey() {
            return serializedMasterPublicKey;
        }

        /**
         * @return the chain code is additional entropy needed to prevent someone just holding the private/public key pair
         * from deriving more child nodes
         */
        public byte[] getChainCode() {
            return chainCode;
        }

        /**
          * @return the real Curve25519 public key used for signing and encryption
         */
        public byte[] getPublicKey() {
            return publicKey;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Bip32Node bip32Node = (Bip32Node) o;
            return Arrays.equals(privateKeyLeft, bip32Node.privateKeyLeft) &&
                    Arrays.equals(privateKeyRight, bip32Node.privateKeyRight) &&
                    Arrays.equals(masterPublicKey, bip32Node.masterPublicKey) &&
                    Arrays.equals(chainCode, bip32Node.chainCode) &&
                    Arrays.equals(publicKey, bip32Node.publicKey);
        }

        @Override
        public int hashCode() {
            int result = Arrays.hashCode(privateKeyLeft);
            result = 31 * result + Arrays.hashCode(privateKeyRight);
            result = 31 * result + Arrays.hashCode(masterPublicKey);
            result = 31 * result + Arrays.hashCode(chainCode);
            result = 31 * result + Arrays.hashCode(publicKey);
            return result;
        }

        @Override
        public String toString() {
            return "Bip32Node{" +
                    "privateKeyLeft=" + Convert.toHexString(privateKeyLeft) +
                    ", privateKeyRight=" + Convert.toHexString(privateKeyRight) +
                    ", masterPublicKey=" + Convert.toHexString(masterPublicKey) +
                    ", chainCode=" + Convert.toHexString(chainCode) +
                    ", serializedMasterPublicKey=" + Convert.toHexString(serializedMasterPublicKey) +
                    ", publicKey=" + Convert.toHexString(publicKey) +
                    '}';
        }
    }
}
