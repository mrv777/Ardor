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

const BIP39 = (function (){
    // encapsulation of BIP39 helper functions (adapted from https://github.com/bitcoinjs/bip39)
    const INVALID_ENTROPY = 'Invalid entropy';
    const INVALID_MNEMONIC = 'Invalid mnemonic';
    const INVALID_CHECKSUM = 'Invalid mnemonic checksum';

    // bytes: Array of unsigned integers, not a TypedArray!
    function bytesToBinary(bytes) {
        return bytes.map(x => x.toString(2).padStart(8, '0')).join('');
    }

    function binaryToByte(bin) {
        return parseInt(bin, 2);
    }

    // entropy: Uint8Array
    function deriveChecksumBits(entropy) {
        const ENT = entropy.length * 8;
        const CS = ENT / 32;
        const sha256 = CryptoJS.algo.SHA256.create();
        sha256.update(converters.byteArrayToWordArrayEx(entropy));
        const hash = sha256.finalize();
        return bytesToBinary(Array.from(converters.wordArrayToByteArrayEx(hash))).slice(0, CS);
    }

    function mnemonicToEntropy(mnemonic, wordlist) {
        wordlist = wordlist || NRS.constants.SECRET_WORDS;
        const words = mnemonic.split(' ');
        if (words.length % 3 !== 0)
            throw new Error(INVALID_MNEMONIC);
        // convert word indices to 11 bit binary strings
        const bits = words
            .map(word => {
                const index = wordlist.indexOf(word);
                if (index === -1)
                    throw new Error(INVALID_MNEMONIC);
                return index.toString(2).padStart(11, '0');
            })
            .join('');
        // split the binary string into ENT/CS
        const dividerIndex = Math.floor(bits.length / 33) * 32;
        const entropyBits = bits.slice(0, dividerIndex);
        const checksumBits = bits.slice(dividerIndex);
        // calculate the checksum and compare
        const entropyBytes = entropyBits.match(/(.{1,8})/g).map(binaryToByte);
        if (entropyBytes.length < 16)
            throw new Error(INVALID_ENTROPY);
        if (entropyBytes.length > 32)
            throw new Error(INVALID_ENTROPY);
        if (entropyBytes.length % 4 !== 0)
            throw new Error(INVALID_ENTROPY);
        const entropy = Uint8Array.from(entropyBytes);
        const newChecksum = deriveChecksumBits(entropy);
        if (newChecksum !== checksumBits)
            throw new Error(INVALID_CHECKSUM);
        return converters.byteArrayToHexString(entropy);
    }

    // entropy: Uint8Array of random values
    function entropyToMnemonic(entropy, wordlist) {
        wordlist = wordlist || NRS.constants.SECRET_WORDS;
        // 128 <= ENT <= 256
        if (entropy.length < 16) {
            throw new TypeError(INVALID_ENTROPY);
        }
        if (entropy.length > 32) {
            throw new TypeError(INVALID_ENTROPY);
        }
        if (entropy.length % 4 !== 0) {
            throw new TypeError(INVALID_ENTROPY);
        }
        const entropyBits = bytesToBinary(Array.from(entropy));
        const checksumBits = deriveChecksumBits(entropy);
        const bits = entropyBits + checksumBits;
        const chunks = bits.match(/(.{1,11})/g);
        const words = chunks.map(binary => wordlist[binaryToByte(binary)]);
        return words.join(' ');
    }

    function generateMnemonic(bits, wordlist) {
        wordlist = wordlist || NRS.constants.SECRET_WORDS;
        bits = bits || 128;
        if (bits % 8 !== 0) {
            throw new TypeError(INVALID_ENTROPY);
        }
        let bytes = NRS.getRandomBytes(bits / 8);
        return entropyToMnemonic(bytes, wordlist);
    }

    // Checks if a list of words is a valid BIP39 mnemonic.
    // It uses the english wordlist. It checks the number of words and it also verifies the checksum.
    function isValidMnemonic(words) {
        try {
            mnemonicToEntropy(words.join(' '));
        } catch (e) {
            return false;
        }
        return true;
    }

    return {
        generateMnemonic : generateMnemonic,
        entropyToMnemonic: entropyToMnemonic,
        mnemonicToEntropy: mnemonicToEntropy,
        isValidMnemonic  : isValidMnemonic
    };
}());

if (isNode) {
    module.exports = BIP39;
}