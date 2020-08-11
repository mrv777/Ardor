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
import org.junit.Test;

public class LedgerComparisonTest {

    private static final String MNEMONIC = "van library enough young maximum jelly solar elder rose omit journey violin lazy someone second stay scale mosquito tennis peasant brand stay blouse almost";

    private static final String[] TESTNET_PUBLIC_KEYS = new String[] {
            "8b9a595b0941aec8784586f2a19bf313277fd26bf9aa51f4bd9f34159ba4a22c",
            "2930a1f10d53fb87cddc11a232537b25d7f646f57edb747d82bc7d27f8a9b536",
            "5386acb16f049b56e75a11edf0d649336237694ede5a52a5f3895334430b0d58",
            "9c67c16ce9b2aa8660ac71269990fbc9f2e6420d545603c911cc9872c1517530",
            "15e8c8704df4e977798515d2aaf4ed0224f5f294b61a9f510837256a2337134c",
            "643ed363ab777f042ccb63a8e83bbd4080d15898a22462cfa22a34c64ea04553",
            "9b0e4ee3248d03c59e6e7149ba19f4697a20a94542a1bb908094a72a0e720b13",
            "765a130eb46c47206a94cd44b25151acde6c13fbdbf75b9144e4465742ce774a",
            "33bdab3865546fe0f6c555e7bebcaedec9b278d5e7b296b480768b5dcd2e021c",
            "c2fe3bd0b224a1ec8305d400c039089604007e9da82e552de54f9f9683599d76",
            "95c6324f4ea1cbcf475de75525eb09c1e4c5afc6df493370994504b1ca217e71",
            "658c7978e99e11c4ab75c3f12d14dfbf8ce2fd773721fd4f752c64300090f434",
            "4fc611c1f9944a75521ed6fd333db711729c2203f1c4b4c12019da94ff252536",
            "96856612b108aef00a9b0929c7f78e6d36745c28143137b29259d2e2a6747931",
            "a4b9539c531e0dee5d8e9c61ed0d48613162a56a994d474f29e2af6fbc265427",
            "1fb8fe56be7f981381ee1f134564290ab66fd6b9e2acaa3bd25f6cedfb5f0261",
            "fd05f6b95c08443ce98590f195f11ea062846ec5ab341a73a134785c261cbf5b",
            "3486196c37e1ed38f4d8edfa097e300224658137abe1db79c7bf788d02a19353",
            "3785c44e6166eea81d35f35d26ed5c7c2a8d0f312c2e313c32d58b535b14471e",
            "6129c99482e7bb64799593100af1dad43f2049c5853a6c4ac28b02c81ae94b5c"
    };

    private static final String[] MAINNET_PUBLIC_KEYS = new String[] {
            "99326ac72fcd8b484e29c8c674cffa55eb29a93d8356639b6be5d763b1abbe57",
            "e5da2d35b3e51b465553ed2a57f30935acfe4cec3173b41aa8278dcaf0be7068",
            "22d52e3eae55e417789f4d828758f513fa9ee03f1810834b08e4a3a886dca574",
            "0fbf3fc41acb343be49201c8ff78bcbf0b445d8da453cf754ea1cedad2c3d62e",
            "8786bbbf28fd9d28c5671b94d70cc06b2dbb10f8ae5a7490d1f29f5be922f205",
            "213ff0490e8937da58c24502d6ea0e9e2e962e52a235d245ac65bc8d68912979",
            "5fb37f86b0f4ff109263fbd9b3b1d23118e817f9d7e18f11762346a4da507b08",
            "38e7598952fab2112633ee8a62937b17e0db4d89fb0448950855b3a1b1319104",
            "bc7b84afe3fa31da2d036edf49c12ee5dec66523020b09961617fb8c0eeef144",
            "9a18ab949bc9ab139898c908453d45ec817f41cf412d837c1ef278e77aa9a978",
            "bcc5458726da23b73cc7faec17b76b7f049b05e619e81a0291a5ab53a982733e",
            "0af187cc69d826e78d37dac008549e20a34009899e11e2eca3cbafabd361c579",
            "f2180cd154553e943e343cd38c705e8daded210ce871a4010ae03018f2914833",
            "9eeda961a718981d12774e21891ce49838612c41230969dfc08cc95965ffac32",
            "2684a1f6148fe999ba427dd9fe62d81b8b4a208aecbec830e7ceecbb8913d445",
            "d01f375dd75131cc38bab70dbcb92c4267758f000999a7292ace737e434a4956",
            "ce0cdfe4a0efd1032b018407a2fd07d95bac73330ef37449f6c99bdcf2eb9b74",
            "96196f7f2033b0e5b755b252e056ccf9937d31451333db0751fb803465e92139",
            "eac75d218ecfc87bf5c8d600bc775a93cdc88b127439ce33751bd3eba81b5833",
            "2aadffe75587d12c0059913150907752ff6d0f1d8f8d9609b9a5fd398d17e24a"
    };

    private String[][] MAINNET_DATA = {
            new String[]{
                    "99326ac72fcd8b484e29c8c674cffa55eb29a93d8356639b6be5d763b1abbe57", // curve25519PublicKey
                    "c665192b88e9f0ad5efe7094ddad7d34e5dd232768e5889404c3eb51432cdd5e", // ed25519MasterKey
                    "13eafb8487cc1fc446d6c29fdf630361ee484e2e5eeba83e7ca7406238f18aa9", // Chain Code
                    "58cd98d7e78e309f2fb86479d11c326c5a7f441d5d42252dc85bf1a960351450e0b44e935bafea859fc26f181dfbdddbfeb5c878251fdc12dbe19774e4e6c7d9" // KL,KR
            },
            new String[]{
                    "e5da2d35b3e51b465553ed2a57f30935acfe4cec3173b41aa8278dcaf0be7068",
                    "f8a05d6aeb6153a14c6fa400537dc8157d0b6727c7ccc2e8fbb0cf0d6f7592a8",
                    "c6ac197f5f28c0a2fd90114201ccf627835f8fc151a7c61a0ef76c413ccc62b0",
                    "88d76d0b99439b9a5a3c9accdc83928b13e8584e5eac2be26990985b6035145011299bfb3d4535fa55a68db4b5a4521e42d31db1352b36e8bacb42dc73c2c77d"
            },
            new String[]{
                    "22d52e3eae55e417789f4d828758f513fa9ee03f1810834b08e4a3a886dca574",
                    "08a1bce202966e5c48931bdc3a1d87af90683a29bb60b564e1fb121493047e34",
                    "e1f13fff5fef5dd6ebd643eb46b407b005f9206c73daf51b9b8cfdf1eee2035a",
                    "2015629f3068cefc82c09c730e47918eb45ed6b3578ca175539d3f3a5d35145015ba2280a8ae3e34189b1c742f91f99b8ef8f18b5552129c03f6770a6a401741"
            },
            new String[]{
                    "0fbf3fc41acb343be49201c8ff78bcbf0b445d8da453cf754ea1cedad2c3d62e",
                    "51473db2d65cf28d7a6d326e02824c53e9941b28cd1854e73e8984e4ff268956",
                    "8b8cab1cca8ec718779d7ef8e95f17b8c4b822ba736cf6b5866ed1ce4ab54205",
                    "b8b73595aa2ba27fdad18f290f29972e5b9d58ae7f94b7adcfad97545a351450684af439c84113f52a66cf6d4087e0b45e8fb788501121c5d0927a16588ca4c1"
            },
            new String[]{
                    "8786bbbf28fd9d28c5671b94d70cc06b2dbb10f8ae5a7490d1f29f5be922f205",
                    "d3cf8a7f534685fd7f5cedc8eac015500d43dd09214eb1e60955ed00c4394baf",
                    "39df1462982360e3a8bd34def53e60aa7f6377091a55e0e2df8d21bf1d62c740",
                    "c0f672358b456a492128c597eec920c92c4b600f83f7a2bc4fbbcd2c5f35145058c4efa4f6a2c26f3c94ba39feac0b043142c93b85c8c1b802bca86b5b6f74ff"
            },
            new String[]{
                    "213ff0490e8937da58c24502d6ea0e9e2e962e52a235d245ac65bc8d68912979",
                    "fe48abf05b64eea75df844dcc989543cb067641a0cc3a905f1f814e70c4e81b8",
                    "519174e8406de24d5aa243b65195127e9e59c906bf417b4a86dd7b754f7f1236",
                    "d0dd140d751b9f6b26e283b626309a75b998eca99e2fc4d57ba2013b5f3514506ed979e956832350fa7edb948eddeb8ad0f7a6e53f5be7556b52a4cb70398c7e"
            },
            new String[]{
                    "5fb37f86b0f4ff109263fbd9b3b1d23118e817f9d7e18f11762346a4da507b08",
                    "754d33a35bcd5c96401cdfc9fb0a5bbc6db58fd9d589c23e1f2db896fc3ab943",
                    "8aaafda2780a04ac764ce1ccc9e98884ce0b1cc01b98decb34cca8a621314502",
                    "e8e2622eca149a4867af72114a0008729972f3ae5ab369920aa49bb05b35145055bb3c6cbccaa3f913e69ca074ab6b3f2e1a26ce6a37f0bbef55d43cd6884f26"},
            new String[]{
                    "38e7598952fab2112633ee8a62937b17e0db4d89fb0448950855b3a1b1319104",
                    "38a2e2704949283c4ff285b43dc5acc1173ae0d48c45bb8415922c20c1745d90",
                    "0773622a38ce3f48f744b9eb6a3fe8a564cd3748162f45c3c4e6156645645e0e",
                    "0849adb0651346536e4b404afa18834b96ea59587c05325841079b115e35145091ca2008908b728f4c0eb5639ecde790bf96d213d9f5a9737f3687bd30bb751a"},
            new String[]{
                    "bc7b84afe3fa31da2d036edf49c12ee5dec66523020b09961617fb8c0eeef144",
                    "abfcd03cb153ea505182650310c1b4fbefaf8f7e729ecd8c130a20accbda39a8",
                    "2938cabeafe278c46e69149d54946c43aef0725a2cfe77bbeb59441a0fbbc8fc",
                    "5870f0332c129283ba3f566998fe6eb93fd02a789b885d53d5fb37e75e351450cae14be26cda61cf8b910539144c14d12ba93f2dc6fc8ddda1b2c80936ce361d"},
            new String[]{
                    "9a18ab949bc9ab139898c908453d45ec817f41cf412d837c1ef278e77aa9a978",
                    "68fca9024c62290699b152f974916517780657ba8fa56c5eec945296d403dc78",
                    "ff4e281663ac8b1d7db1c43e40a6f501f322ee7f8ef370dea77fa58259b01ff9",
                    "2802e301e40279b09e4eab0f00f3488791c53c792fdd196ae876e1cb5c351450f3be897ff58ceac7d209dd79d48859661a2b4a2711495b13368e49012f89c453"},
            new String[]{
                    "bcc5458726da23b73cc7faec17b76b7f049b05e619e81a0291a5ab53a982733e",
                    "aeb5f707f400cab664b9af1f606c41523140735e4fa308563dcf0b9280b8530c",
                    "83d6bfdc410d07e6bd5cb088fa6a4c4f3b9a6b058b8fd323579e6414137dc8ac",
                    "c05b0e0a2d7728b14550d7a6b9be91690d05995eea7ba1a61e9667c95a35145046397221704cac97829d3c5888ee47d5c33044baeecda1f9ec537b66d7d5071b"},
            new String[]{
                    "0af187cc69d826e78d37dac008549e20a34009899e11e2eca3cbafabd361c579",
                    "33ec014bf555c070d26ac0dadb04a5fea30e251879bb586b68769879911e6099",
                    "0a0a4d204c9d481e6e42fd5361d3510b17afe2f57cf32e64845e5076ef808394",
                    "10eabdd155a08c8d92c68c0e9cabc2c8335c2303dabf72dfdd1e919d5c35145022ea9632e62b1d01de1be8a4a83d1de3e5341def524a2165667d54f6c35fe805"},
            new String[]{
                    "f2180cd154553e943e343cd38c705e8daded210ce871a4010ae03018f2914833",
                    "3d3f791e17f38d33d7c9811c1f49820e30c66aeaa76baf515f1ea4de7ec1ad1b",
                    "c3f955a885f07a6c8bb2613e7c5c3411f8c9b6d6fc42c3f2d57d6cf85b0dd0d6",
                    "a8bf5536d5e47b597b0fe83d2213a92739c0ec41697d262e2b81fdb75e35145079ae297a9d12fbc135556603a60b90373c0baebf1a0001b2dc66c40349be86d1"},
            new String[]{
                    "9eeda961a718981d12774e21891ce49838612c41230969dfc08cc95965ffac32",
                    "1b76034a25ea590e9449de9fd520a21a6f8ec116f81a7be15777f4d14bcf2c6d",
                    "cd266fded740a51f7c3fb6f15a3ddfa603d533cb599068784b037abe2be64f06",
                    "207e1ed83f94998cdc3c6bf4b02acec092de4c4a8e8edc817369cacf5a351450e90e0d1b5fe15ca3ba8fbd5e28c15fc40c2f1fc39c337ed51bc425a5d20e9b98"},
            new String[]{
                    "2684a1f6148fe999ba427dd9fe62d81b8b4a208aecbec830e7ceecbb8913d445",
                    "f24ca20a2015dab25995ac70beccc179be6e81e119dc2393ed4d3a75082fc27f",
                    "d35e91ce39cfdcad2b73c7d7668b56b7898d2d19a4482775718a2eff6bac4c3b",
                    "1881a4183cff575fa3dbaecaee7f73c729fc6192d0a5d11849697eca60351450a3597a55ce7031fef3022fb8a6a7cd95e3cff7f4a8da04cc3fd756ff7e394303"},
            new String[]{
                    "d01f375dd75131cc38bab70dbcb92c4267758f000999a7292ace737e434a4956",
                    "a7e3de12c141f603ba4c51b12749f021b50177de1b1872f5ef83eb5143f785a6",
                    "df5867a15d919584a80880aa394c22a4d5a98168b9ff94d747d4d443cd439deb",
                    "20676d8ba2f9b89ad6cad47ddaa616c7c77f41fb6919ffa4182022f260351450b4be7c676b099f6d2b82f168862f2b3e9a4d7128bc9cba1582f4c4a15b62c51d"},
            new String[]{
                    "ce0cdfe4a0efd1032b018407a2fd07d95bac73330ef37449f6c99bdcf2eb9b74",
                    "16967774f49dec191cdc90ce25e5c86caeb7312a291bdd187e6c95622d92516f",
                    "63a07ecc405adb1a25a23915c6a0ff1b65bfc3472d3faca026e5a45533832848",
                    "10a3cc655c32b68eba35403b0d7b985b82c9813f62be9a1585157e415a351450cf47685771ce857f9d8d7dd4d59ab7b14c865321392766438069e34878a2b616"},
            new String[]{
                    "96196f7f2033b0e5b755b252e056ccf9937d31451333db0751fb803465e92139",
                    "0ba0aa66620101707a9e253ce1d8e3a984e07198a851989c54b31ec611eeeebe",
                    "21f42c5137843a2206a0b813b03de94195f97a9e93bc785896f221d029dbedf7",
                    "f8aadd1409cf8f3a443c2a329abd3b8c87164067d2f9a73e2ced43c2613514508f330832f539ebcc78e1c085f09d80a5a4f39a3c044956765196120f40a95855"},
            new String[]{
                    "eac75d218ecfc87bf5c8d600bc775a93cdc88b127439ce33751bd3eba81b5833",
                    "630c9b607f3676625b8d555c6044c1cd34eaf72ae0e67176fcef8a610e21a693",
                    "0301dbb87799038b0161377a3741b6ce9c1a3200fe536ba51274786be386a0d2",
                    "b83dcf608402ef0472189a26ea365f0649a4739b367f8f2aaf0700615a35145064d658839007bc802ecfbd397be3327015a7cdd1857e0b4c069fe671a1734114"},
            new String[]{
                    "2aadffe75587d12c0059913150907752ff6d0f1d8f8d9609b9a5fd398d17e24a",
                    "3ed11fc151b9dc898f4c6d0be27323d6345746c1988cfca1e61468f146cf0104",
                    "072202ae8b72fc816b0ae5a6a6fe8ba4902c960d40e8e1c4a5bde93cfc637336",
                    "d0532939b5811d7481b55db9d03398a066a9ca7cb22d50f54c9da687603514504bf87263aa99bff1c6684d9a27d0d0b806bcdbc3daba58f3c34d44491160fcb8"}};

    @Test
    public void compareTestnetPublicKeys() {
        for (int i = 0; i < TESTNET_PUBLIC_KEYS.length; i++) {
            String path = "m/44'/16754'/0/1/" + i;
            KeyDerivation.Bip32Node bip32Node = KeyDerivation.deriveMnemonic(path, MNEMONIC);
            byte[] curve25519PublicKey = bip32Node.getPublicKey();
            Assert.assertEquals(TESTNET_PUBLIC_KEYS[i], Convert.toHexString(curve25519PublicKey));

            byte[] publicKey = new byte[32];
            Curve25519.keygen(publicKey, null, bip32Node.getPrivateKeyLeft());
            Assert.assertArrayEquals(curve25519PublicKey, publicKey);
        }
    }

    @Test
    public void compareMainnetPublicKeys() {
        for (int i = 0; i < MAINNET_PUBLIC_KEYS.length; i++) {
            String path = "m/44'/16754'/0/0/" + i;
            KeyDerivation.Bip32Node bip32Node = KeyDerivation.deriveMnemonic(path, MNEMONIC);
            byte[] curve25519PublicKey = bip32Node.getPublicKey();
            Assert.assertEquals(MAINNET_PUBLIC_KEYS[i], Convert.toHexString(curve25519PublicKey));

            byte[] publicKey = new byte[32];
            Curve25519.keygen(publicKey, null, bip32Node.getPrivateKeyLeft());
            Assert.assertArrayEquals(curve25519PublicKey, publicKey);
        }
    }

    @Test
    public void compareMainnetData() {
        String mainnetPath = "m/44'/16754'/0/0";
        KeyDerivation.Bip32Node parentNode = KeyDerivation.deriveMnemonic(mainnetPath, MNEMONIC);
        for (int i = 0; i < MAINNET_DATA.length; i++) {
            KeyDerivation.Bip32Node childNode = KeyDerivation.deriveChildPrivateKey(parentNode, i);
            Assert.assertEquals(MAINNET_DATA[i][0], Convert.toHexString(childNode.getPublicKey()));
            Assert.assertEquals(MAINNET_DATA[i][1], Convert.toHexString(childNode.getMasterPublicKey()));
            Assert.assertEquals(MAINNET_DATA[i][2], Convert.toHexString(childNode.getChainCode()));
            Assert.assertEquals(MAINNET_DATA[i][3].substring(0, 64), Convert.toHexString(childNode.getPrivateKeyLeft()));
            Assert.assertEquals(MAINNET_DATA[i][3].substring(64, 128), Convert.toHexString(childNode.getPrivateKeyRight()));

            KeyDerivation.Bip32Node childPublicKeyNode = KeyDerivation.deriveChildPublicKey(parentNode, i);
            byte[] curve25519PublicKey = childPublicKeyNode.getPublicKey();
            Assert.assertEquals(MAINNET_DATA[i][0], Convert.toHexString(curve25519PublicKey));
            Assert.assertEquals(MAINNET_DATA[i][1], Convert.toHexString(childPublicKeyNode.getMasterPublicKey()));

            byte[] publicKey = new byte[32];
            Curve25519.keygen(publicKey, null, childNode.getPrivateKeyLeft());
            Assert.assertArrayEquals(curve25519PublicKey, publicKey);
        }
    }

}
