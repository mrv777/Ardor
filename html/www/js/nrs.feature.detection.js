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

(isNode ? client : NRS).onSiteBuildDone().then(() => {
    NRS = (function (NRS) {
        let isDesktopApplication = navigator.userAgent.indexOf("JavaFX") >= 0;
        let isLocalHost = false;
        let hostName;
        let isLoadedOverHttps;
        const nativeBigInt = (typeof global !== 'undefined') && global.BigInt || (typeof window !== 'undefined') && window.BigInt;
        const isBigIntSupported = typeof nativeBigInt === 'function';

        function useHdWallet() {
            return NRS.bip32Account !== undefined;
        }

        function useHardwareWallet() {
            return useHdWallet() && NRS.bip32Account.getProvider() === NRS.BIP32_PROVIDER.LEDGER_HARDWARE;
        }

        // Must be here before assigning isLocalHost!
        NRS.isPrivateIP = function (ip) {
            if (!/^\d+\.\d+\.\d+\.\d+$/.test(ip)) {
                return false;
            }
            var parts = ip.split('.');
            return parts[0] === '10' || parts[0] == '127' || parts[0] === '172' && (parseInt(parts[1], 10) >= 16 && parseInt(parts[1], 10) <= 31) || parts[0] === '192' && parts[1] === '168';
        };

        if (isNode) {
            // TODO do we need to specify values here?
        } else {
            isLoadedOverHttps = "https:" == window.location.protocol;
            if (window.location && window.location.hostname) {
                hostName = window.location.hostname.toLowerCase();
                isLocalHost = hostName == "localhost" || hostName == "127.0.0.1" || NRS.isPrivateIP(hostName);
            }
        }

        NRS.isIndexedDBSupported = function () {
            return window.indexedDB !== undefined;
        };

        NRS.isExternalLinkVisible = function () {
            // When using JavaFX add a link to a web wallet except on Linux since on Ubuntu it sometimes hangs
            return !(isDesktopApplication && navigator.userAgent.indexOf("Linux") >= 0);
        };

        NRS.isWebWalletLinkVisible = function () {
            return isDesktopApplication && navigator.userAgent.indexOf("Linux") == -1;
        };

        NRS.isPollGetState = function () {
            // When using JavaFX do not poll the server unless it's working as a proxy
            return !isDesktopApplication || NRS.state && NRS.state.apiProxy;
        };

        NRS.isFileEncryptionSupported = function () {
            return NRS.isFileReaderSupported();
        };

        NRS.isShowDummyCheckbox = function () {
            return isDesktopApplication && navigator.userAgent.indexOf("Linux") >= 0; // Correct rendering problem of checkboxes on Linux
        };

        NRS.getRemoteNodeUrl = function () {
            if (!isNode) {
                return "";
            }
            return NRS.getModuleConfig().url;
        };

        NRS.getDownloadLink = function (url, link) {
            if (link) {
                link.attr("href", url);
                return;
            }
            return "<a href='" + url + "' class='btn btn-xs btn-default'>" + $.t("download") + "</a>";
        };

        NRS.isScanningAllowed = function () {
            return isLocalHost || NRS.isTestNet;
        };

        NRS.changeNow_url = function () {
            return NRS.settings.changeNow_url;
        };

        NRS.isForgingSupported = function () {
            return !(NRS.state && NRS.state.apiProxy);
        };

        NRS.isFundingMonitorSupported = function () {
            return !(NRS.state && NRS.state.apiProxy);
        };

        NRS.isShufflingSupported = function () {
            return !(NRS.state && NRS.state.apiProxy);
        };

        NRS.isConfirmResponse = function () {
            return NRS.state && NRS.state.apiProxy;
        };

        NRS.isShowClientOptionsLink = function () {
            return NRS.state && NRS.state.apiProxy;
        };

        NRS.getGeneratorAccuracyWarning = function () {
            if (isDesktopApplication) {
                return "";
            }
            return $.t("generator_timing_accuracy_warning");
        };

        NRS.isShowRemoteWarning = function () {
            return !isLocalHost && !NRS.isHdWalletPrivateKeyAvailable();
        };

        NRS.isForgingSafe = function () {
            return isLocalHost;
        };

        NRS.isPassphraseAtRisk = function () {
            return !isLocalHost || NRS.state && NRS.state.apiProxy;
        };

        NRS.isWindowPrintSupported = function () {
            return !isDesktopApplication && navigator.userAgent.indexOf("Firefox") == -1;
        };

        NRS.getAdminPassword = function () {
            if (isNode) {
                return NRS.getModuleConfig().adminPassword;
            }
            if (window.java) {
                return window.java.getAdminPassword();
            }
            return NRS.deviceSettings.admin_password;
        };

        NRS.isFileReaderSupported = function () {
            return (isDesktopApplication && window.java && window.java.isFileReaderSupported()) ||
                (!isDesktopApplication && !!(window.File && window.FileList && window.FileReader)); // https://github.com/Modernizr/Modernizr/blob/master/feature-detects/file/api.js
        };

        NRS.isVideoSupported = function () {
            return !isDesktopApplication;
        };

        NRS.isAnimationAllowed = function () {
            return !isDesktopApplication;
        };

        NRS.isCameraAccessSupported = function () {
            return !isDesktopApplication;
        };

        NRS.isHardwareWalletConnectionAllowed = function () {
            return isDesktopApplication || location.protocol === "https:" ||
                location.hostname === 'localhost' || location.hostname === '127.0.0.1';
        };

        NRS.useClientSideKeyDerivation = function () {
            return isBigIntSupported;
        };

        NRS.getBip32CalculatorType = function () {
            return isDesktopApplication || isLocalHost ? "server" : "client";
        };

        NRS.isBip32CalculatorDisableClientOption = function () {
            return isDesktopApplication;
        };

        NRS.isHdWalletPrivateKeyAvailable = function() {
            if (!useHdWallet()) {
                return false;
            }
            if (useHardwareWallet()) {
                return true;
            }
            return NRS.bip32Account.getPrivateKey() !== null;
        }

        NRS.isDisablePassphraseValidation = function() {
            return NRS.isHdWalletPrivateKeyAvailable();
        }

        NRS.isAndroidWebView = function() {
            return typeof androidWebViewInterface !== 'undefined';
        }

        NRS.isPublicKeyLoadedOnLogin = () => useHdWallet();
        NRS.isBip32PathAvailable = () => useHdWallet();

        NRS.isHardwareEncryptionEnabled = () => useHardwareWallet();
        NRS.isHardwareDecryptionEnabled = () => useHardwareWallet();
        NRS.isHardwareTransactionSigningEnabled = () => useHardwareWallet();
        NRS.isHardwareTokenSigningEnabled = () => useHardwareWallet();
        NRS.isHardwareShowAddressEnabled = () => useHardwareWallet();
        NRS.isPrivateKeyStoredOnHardware = () => useHardwareWallet();

        return NRS;
    }(isNode ? client : NRS || {}, jQuery));

    if (isNode) {
        module.exports = NRS;
    }
});