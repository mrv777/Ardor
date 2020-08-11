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

/**
 * @depends {nrs.js}
 */
NRS.onSiteBuildDone().then(() => {
    NRS = (function(NRS, $) {
        var loginOptions = {};
        NRS.bip32Accounts = [];
        NRS.newlyCreatedAccount = false;

        let $accountPhraseCustomPanel = $("#account_phrase_custom_panel");
        const $secretGeneratorWordlistLength = $('#secret_generator_wordlist_length');
        const $loginSeed = $("#login_seed");

        function showLoginScreen() {
            $("#account_phrase_custom_panel, #account_phrase_generator_panel").hide();
            $accountPhraseCustomPanel.find(":input:not(:button):not([type=submit])").val("");
            $("#account_phrase_generator_panel").find(":input:not(:button):not([type=submit])").val("");
            $("#login_account_other").mask(NRS.getAccountMask("*"));
            $("#login_panel").show();
            $loginSeed.focus();
            setTimeout(function() {
                NRS.tours.login();
            }, 50);
        }

        $("#login_account_other").on("keydown", (event) => {
            if (event.which === 13) {
                $("#login_btn").click();
            }
        });

        $(".create-passphrase-next").click(function(e) {
            e.preventDefault();
            if ($("#confirm_passphrase_warning").is(":checked")) {
                $('.step_2').hide();
                $('.step_3').show();
                let secretEntity = $("#entity_creation_type").val();
                $("#step_3_secret_write_below").html($.t("secret_write_below", { entity: secretEntity }));
                $("#step_3_incorrectly_typed_secret").html($.t("incorrectly_typed_secret", { entity: secretEntity }));
                $("#step_3_confirm_secret_header").html($.t("confirm_secret", {entity: NRS.initialCaps(secretEntity) }));
                $("#step_3_confirm_secret").focus();
            } else {
                $("#confirm_passphrase_warning_container").css("background-color", "red");
            }
        });

        $("#custom_passphrase_link").click(function(e) {
            e.preventDefault();
            $("#account_phrase_generator_panel, #login_panel").hide();
            $("#account_phrase_generator_panel").find(":input:not(:button):not([type=submit])").val("");
            var accountPhraseCustomPanel = $("#account_phrase_custom_panel");
            accountPhraseCustomPanel.find(":input:not(:button):not([type=submit])").val("");
            accountPhraseCustomPanel.show();
            $("#registration_password").focus();
        });

        function generateMnemonic(numberOfWords) {
            let crypto = window.crypto || window.msCrypto;
            return crypto ? BIP39.generateMnemonic((numberOfWords * 11 * 32) / 33, NRS.constants.SECRET_WORDS) : null;
        }

        function loadMnemonic() {
            $("#login_panel").hide();
            if (NRS.activeTour) {
                NRS.activeTour.end();
            }
            let $container = $("#account_phrase_generator_panel");
            $container.show();
            $container.find(".step_3 .callout").hide();
            $container.find(".account_phrase_generator_steps").hide();
            $container.find("textarea").val("");
            $container.find(".step_2").show();
            if (!$secretGeneratorWordlistLength.val()) {
                $secretGeneratorWordlistLength.val('18');
            }
            const helpText = $.t('memorize_passphrase_help', {count: $secretGeneratorWordlistLength.val()});
            $container.find('.step_2 .callout.memorize_passphrase_help').text(helpText);
            let secret = generateMnemonic($secretGeneratorWordlistLength.val());
            if (secret === null) {
                $container.find(".step_2 textarea").val($.t("unavailable")).prop("readonly", true);
                $.growl($.t("error_encryption_browser_support"));
                return;
            }
            $container.find(".step_2 textarea").val(secret).prop("readonly", true);
            let $accountAddress = $container.find("#step_2_account");
            let entityCreationType = $("#entity_creation_type").val();
            $(".entity_creation_header").text($.t("create_your_entity", { entity: entityCreationType.capitalize() }));
            $(".entity_generated_secret").text($.t("generated_entity", { entity: entityCreationType.capitalize() }));
            $(".entity_warning_disclose").text($.t("entity_warning_disclose", { entity: entityCreationType }));
            $(".confirm_entity_warning").text($.t("confirm_entity_warning", { entity: entityCreationType }));
            if (entityCreationType === "seed") {
                $container.find("#passphrase_specific_widgets").hide();
                $accountAddress.val("");
            } else {
                $container.find("#passphrase_specific_widgets").show();
                $accountAddress.val(NRS.getAccountId(NRS.getPrivateKey(secret), true));
            }
            // noinspection JSUnusedAssignment
            secret = ""; // wipe from memory
            $("#confirm_passphrase_warning").prop('checked', false);
            $("#confirm_passphrase_warning_container").css("background-color", "");
        }

        $secretGeneratorWordlistLength.on('change', () => loadMnemonic());

        $("#generator_paper_wallet_link").click(function(e) {
            e.preventDefault();
            NRS.printPaperWallet($("#account_phrase_generator_panel").find(".step_2 textarea").val(), 3, 2);
        });

        $(".verify-generated-mnemonic").click(function(e) {
            e.preventDefault();
            let $accountPhraseGeneratorPanel = $("#account_phrase_generator_panel");
            let mnemonic = $.trim($accountPhraseGeneratorPanel.find(".step_3 textarea").val());
            let previouslyEnteredMnemonic = $accountPhraseGeneratorPanel.find(".step_2 textarea").val();
            if (mnemonic != previouslyEnteredMnemonic) {
                $accountPhraseGeneratorPanel.find(".step_3 .callout").show();
            } else {
                $accountPhraseGeneratorPanel.find("textarea").val("");
                $accountPhraseGeneratorPanel.find(".step_3 .callout").hide();
                if ($("#entity_creation_type").val() === "seed") {
                    $loginSeed.val(mnemonic);
                    showLoginScreen();
                } else {
                    NRS.loginWithOptions({ isPassphraseLogin: true, id: mnemonic });
                }
            }
        });

        let $loginPassword = $("#login_password");
        $loginPassword.on("input", async function() {
            let mnemonic = $loginPassword.val();
            let $loginPasswordAccount = $("#login_password_account");
            if (mnemonic !== "") {
                let account = NRS.getAccountId(NRS.getPrivateKey(mnemonic));
                NRS.sendRequest("getAccountPublicKey", {account: account}, function (response) {
                    if (response.publicKey) {
                        $loginPasswordAccount.val(NRS.convertNumericToRSAccountFormat(account));
                    } else {
                        $("#login_password_account").val($.t("unknown"));
                    }
                });
            } else {
                $loginPasswordAccount.val("");
            }
        });

        $loginSeed.on("input", async function() {
            const $seedWalletInfo = $("#seed_wallet_info");
            const $masterPublicKey = $("#login_master_public_key");
            $masterPublicKey.val("");
            const mnemonic = $loginSeed.val();
            const words = mnemonic.split(" ");
            if (!BIP39.isValidMnemonic(words)) {
                $seedWalletInfo.text($.t("bip39_warn_invalid_checksum"));
                if (mnemonic === "") {
                    $masterPublicKey.attr("readonly", false);
                } else {
                    $masterPublicKey.attr("readonly", true);
                }
                return;
            }
            let $body = $("body");
            $body.css("cursor", "progress");
            try {
                $seedWalletInfo.text("");
                $masterPublicKey.attr("readonly", true);
                let response = await NRS.deriveAccountFromSeed(NRS.getBip32CalculatorType(), {
                    mnemonic: mnemonic,
                    passphrase: "",
                    bip32Path: BIPPath.fromPathArray(NRS.constants.BIP32_PATH_PREFIX).toString()
                });
                $masterPublicKey.val(response.serializedMasterPublicKey);
            } catch (e) {
                NRS.logException(e);
            } finally {
                $body.css("cursor", "default");
            }
        });

        $(".create-custom-passphrase-next").click(function(e) {
            e.preventDefault();

            let password = $("#registration_password").val();
            let repeat = $("#registration_password_repeat").val();
            let error = "";

            if (password.length < 35) {
                error = $.t("error_passphrase_length");
            } else if (password.length < 50 && (!password.match(/[A-Z]/) || !password.match(/[0-9]/))) {
                error = $.t("error_passphrase_strength");
            } else if (password != repeat) {
                error = $.t("error_passphrase_match");
            }

            if (error) {
                $("#account_phrase_custom_panel").find(".callout").first().removeClass("callout-info").addClass("callout-danger").html(error);
            } else {
                $("#registration_password, #registration_password_repeat").val("");
                NRS.loginWithOptions({ isPassphraseLogin: true, id: password });
            }
        });

        NRS.listAccounts = function() {
            var loginAccount = $('#login_account');
            loginAccount.empty();
            if (NRS.getStrItem("savedNxtAccounts") && NRS.getStrItem("savedNxtAccounts") != ""){
                $('#login_account_container_selector').show();
                var accounts = NRS.getStrItem("savedNxtAccounts").split(";");
                $.each(accounts, function (index, account) {
                    if (account != '') {
                        account = NRS.deviceSettings.account_prefix + account.substr(account.indexOf("-"));
                        $('#login_account').append($("<li></li>")
                            .append($("<a></a>").attr("href", "#").attr("class", "selectAccount").attr("data-account", account).text(account)
                                .append($("<a></a>").attr("data-account", account).attr("class", "removeAccount").text(" x"))
                            )
                        );
                    }
                });
            } else{
                $('#login_account_container_selector').hide();
            }
        };

        async function listBip32Accounts(useHardware, mnemonic, serializedMasterPublicKey) {
            let spinner = new Spinner().spin();
            let spinnerContainer = $(".spinner_container")[0];
            spinnerContainer.appendChild(spinner.el);
            try {
                let $calloutBox = useHardware ? $("#hardware_wallet_info") : $("#seed_wallet_info");
                if (useHardware && !NRS.isHardwareWalletConnectionAllowed()) {
                    $calloutBox.text($.t("hardware_wallet_connection_requires_https"));
                    return;
                }
                NRS.bip32Accounts = [];
                let $container = useHardware ? $("#hardware_wallet_container") : $("#seed_wallet_container");
                let $accountSelect = useHardware ? $("#login_hardware_account") : $("#login_seed_account");
                $accountSelect.empty();
                let from = parseInt($container.find(".from_child").val());
                let to = parseInt($container.find(".to_child").val());
                if (from < 0 || to < from) {
                    $calloutBox.text($.t("wrong_account_index"));
                    return;
                }
                if (to - from >= 20) {
                    $calloutBox.text($.t("cannot_load_more_than_x_accounts"));
                    return;
                }
                let index = from;
                let stopAtNewAccount = $container.find(".stop_at_new_account").is(":checked");
                let parentNode;
                let isSeedLogin = mnemonic !== "";
                if (NRS.useClientSideKeyDerivation() && !useHardware && isSeedLogin) {
                    parentNode = KeyDerivation.deriveMnemonic(BIPPath.fromPathArray(NRS.constants.BIP32_PATH_PREFIX).toString(), mnemonic);
                }
                while (index <= to) {
                    let path = NRS.constants.BIP32_PATH_PREFIX.slice();
                    path.push(index);
                    let bip32PathStr = BIPPath.fromPathArray(path).toString();
                    let msg = $.t("loading_account", { account: bip32PathStr });
                    NRS.logConsole(msg);
                    $calloutBox.text(msg);
                    let privateKey = null;
                    let publicKey;
                    let provider;
                    if (useHardware) {
                        provider = NRS.BIP32_PROVIDER.LEDGER_HARDWARE;
                        let publicKeyBytes = await NRS.getPublicKeyFromHardwareWallet(bip32PathStr, false, $calloutBox);
                        if (publicKeyBytes.length === 0) {
                            return;
                        }
                        publicKey = converters.byteArrayToHexString(publicKeyBytes);
                    } else {
                        provider = NRS.BIP32_PROVIDER.SOFTWARE;
                        let node = null;
                        if (NRS.useClientSideKeyDerivation()) {
                            if (isSeedLogin) {
                                node = KeyDerivation.deriveChildPrivateKey(parentNode, index);
                                privateKey = converters.byteArrayToHexString(node.getPrivateKeyLeft());
                                publicKey = converters.byteArrayToHexString(node.getPublicKey());
                            } else {
                                node = KeyDerivation.deriveChildPublicKeyFromSerializedMasterPublicKey(
                                    converters.hexStringToByteArray(serializedMasterPublicKey), index);
                                publicKey = converters.byteArrayToHexString(node.getPublicKey());
                            }
                        } else {
                            if (isSeedLogin) {
                                let response = await NRS.sendRequestAndWait("deriveAccountFromSeed", {
                                    mnemonic: mnemonic,
                                    bip32Path: bip32PathStr
                                });
                                node = KeyDerivation.createBipNodeFromResponse(response);
                                privateKey = converters.byteArrayToHexString(node.getPrivateKeyLeft());
                                publicKey = converters.byteArrayToHexString(node.getPublicKey());
                            } else {
                                let response = await NRS.sendRequestAndWait("deriveAccountFromMasterPublicKey", {
                                    serializedMasterPublicKey: serializedMasterPublicKey,
                                    childIndex: index
                                });
                                publicKey = response.publicKey;
                            }
                        }
                    }
                    NRS.logConsole("Public key " + publicKey);
                    let account = NRS.getAccountIdFromPublicKey(publicKey, true);
                    let response = await NRS.sendRequestAndWait("getAccountPublicKey", { account: account });
                    let isNewAccount = !response.publicKey || response.publicKey !== publicKey;
                    let bip32Account = NRS.createBip32Account(provider, privateKey, publicKey, account, bip32PathStr, !isNewAccount);
                    let content = `#${index} ${bip32Account.getAccount()} ${isNewAccount ? $.t("new") : ""}`;
                    let $option = $("<option>");
                    $option.attr("offset", index - from).html(content);
                    $accountSelect.append($option);
                    NRS.bip32Accounts.push(bip32Account);
                    if (isNewAccount && stopAtNewAccount) {
                        break;
                    }
                    index++;
                }
                $calloutBox.text($.t("done_loading_accounts"));
            } catch(e) {
                NRS.logException(e);
            } finally {
                spinnerContainer.removeChild(spinner.el);
                spinner.stop();
            }
        }

        let $loginAccount = $('#login_account');
        $loginAccount.on("click", ".selectAccount", function(){
            let account = $(this).data().account;
            $("#login_account_other").val(account);
        });

        $loginAccount.on("click", ".removeAccount", function(e){
            e.stopPropagation();
            let account = $(this).data().account;
            NRS.removeAccount(account);
        });

        NRS.switchAccount = function(account, chainId) {
            // Reset other functional state
            $("#account_balance, #account_balance_sidebar, #account_nr_assets, #account_assets_balance, #account_currencies_balance, #account_nr_currencies, #account_purchase_count, #account_pending_sale_count, #account_completed_sale_count, #account_message_count, #account_alias_count").html("0");
            $("#id_search").find("input[name=q]").val("");
            $('#transactions_type_navi').empty();
            $("#account_leasing").hide();
            delete NRS.accountInfo.currentLeasingHeightFrom; // Force refresh of leasing status
            NRS.resetEncryptionState();
            NRS.resetAssetExchangeState();
            NRS.resetPollsState();
            NRS.resetMessagesState();
            NRS.forgingStatus = NRS.constants.UNKNOWN;
            NRS.isAccountForging = false;
            NRS.selectedContext = null;

            // Reset plugins state
            NRS.activePlugins = false;
            NRS.numRunningPlugins = 0;
            $.each(NRS.plugins, function(pluginId) {
                NRS.determinePluginLaunchStatus(pluginId);
            });
            NRS.deviceSettings.chain = chainId;

            // Return to the dashboard and notify the user
            NRS.goToPage("dashboard");

            // Reset security related state only when switching account not when switching chain
            var chainDescription = NRS.constants.CHAIN_PROPERTIES[chainId].name + " " + NRS.getChainDescription(chainId);
            let options;
            if (account != NRS.accountRS) {
                NRS.setAccountDetailsPassword(null);
                NRS.setAccountDetailsPrivateKey(null);
                NRS.setServerPrivateKey(null);
                NRS.setEncryptionPrivateKey(null);
                NRS.setAdvancedModalPrivateKey(null);
                NRS.setApprovalModelsPrivateKey(null);
                NRS.setTokenPrivateKey(null);
                NRS.rememberPassword = false;
                NRS.account = "";
                NRS.accountRS = "";
                NRS.publicKey = "";
                NRS.bip32Account = undefined;
                NRS.accountInfo = {};
                options = { isPassphraseLogin: false, id: account, isAccountSwitch: true, chain: chainId };
                for (var i=0; i<NRS.bip32Accounts.length; i++) {
                    if (account === NRS.bip32Accounts[i].getAccount()) {
                        options.bip32Account = NRS.bip32Accounts[i].toJsonString();
                    }
                }
                return NRS.loginWithOptions(options, function() {
                    $.growl($.t("switched_to_account", { account: account, chain: chainDescription }));
                });
            } else {
                options = {
                    isPassphraseLogin: loginOptions.isPassphraseLogin, id: loginOptions.id, isAccountSwitch: true, chain: chainId
                };
                if (NRS.bip32Account !== undefined) {
                    options["bip32Account"] = NRS.bip32Account.toJsonString();
                }
                return NRS.loginWithOptions(options, function() {
                    $.growl($.t("switched_to_chain", { chain: chainDescription }));
                });
            }
        };

        let $loginButtons = $("#login_buttons");
        $loginButtons.find("li").click(function (e) {
            e.preventDefault();
            if (NRS.activeTour && NRS.activeTour._options.name !== "login") {
                NRS.activeTour.end();
            }
            let type = $(this).data("tab");
            let activeTab = $(this).siblings(".active");
            let readerId = "login_" + activeTab.data("tab") + "_reader";
            if ($("#" + readerId).is(':visible')) {
                NRS.scanQRCode(readerId, function() {}); // turn off scanning
            }
            activeTab.removeClass("active");
            $(this).addClass("active");
            $(".login_page_content").hide();
            let $loginError = $("#login_error");
            $loginError.hide();
            let content = $("#login_" + type + "_container");
            content.show();

            if (type === "account") {
                $('#login_account_container').show();
                $('#login_password_container').hide();
                $('#hardware_wallet_container').hide();
                $('#seed_wallet_container').hide();
                $("#login_account_other").focus();
                NRS.listAccounts();
            } else if (type === "passphrase") {
                $('#login_account_container').hide();
                $('#login_password_container').show();
                $('#hardware_wallet_container').hide();
                $('#seed_wallet_container').hide();
                $("#login_password").focus();
            } else if (type === "hardware") {
                NRS.showHardwareWalletContainer();
            } else if (type === "seed") {
                $('#login_account_container').hide();
                $('#login_password_container').hide();
                $('#hardware_wallet_container').hide();
                $('#seed_wallet_container').show();
                $loginSeed.focus();
            }
        });

        NRS.showHardwareWalletContainer = function() {
            $('#login_account_container').hide();
            $('#login_password_container').hide();
            $('#hardware_wallet_container').show();
            $('#seed_wallet_container').hide();
            if (NRS.isAndroidWebView()) {
                $("#hardware_wallet_info").html($.t("no_hardware_wallet_in_webview", {
                    'href': window.location.href
                }));
                $("#login_hardware_load_accounts").prop('disabled', true);
            } else if (!NRS.isHardwareWalletConnectionAllowed()) {
                $("#hardware_wallet_info").text($.t("hardware_wallet_connection_requires_https"));
                $("#login_hardware_load_accounts").prop('disabled', true);
            } else {
                $("#login_hardware_load_accounts").prop('disabled', false);
            }

            $(".show_advanced_load").focus();
        };

        NRS.removeAccount = function(account) {
            var accounts = NRS.getStrItem("savedNxtAccounts").replace(account+';','');
            if (accounts == '') {
                NRS.removeItem('savedNxtAccounts');
            } else {
                NRS.setStrItem("savedNxtAccounts", accounts);
            }
            NRS.listAccounts();
        };

        function rememberAccount(account) {
            var accountsStr = NRS.getStrItem("savedNxtAccounts");
            if (!accountsStr) {
                NRS.setStrItem("savedNxtAccounts", account + ";");
                return;
            }
            var accounts = accountsStr.split(";");
            if (accounts.indexOf(account) >= 0) {
                return;
            }
            NRS.setStrItem("savedNxtAccounts", accountsStr + account + ";");
        }

        $("#login_btn").on("click", function() {
            let type = NRS.getLoginType();
            let $loginError = $("#login_error");
            $loginError.hide();
            if (type === "account") {
                let id = $("#login_account_other").val();
                NRS.loginWithOptions({ isPassphraseLogin: false, id: id });
            } else if (type === "passphrase") {
                let id = $("#login_password").val();
                NRS.loginWithOptions({ isPassphraseLogin: true, id: id, isPreventLoginToNewAccount: true });
            } else if (type === "hardware") {
                let $selectedOption = $("#login_hardware_account").children("option:selected");
                let offset = $selectedOption.attr("offset");
                let bip32Account;
                if (offset !== undefined) {
                    bip32Account = NRS.bip32Accounts[parseInt(offset)];
                }
                if (!bip32Account) {
                    $loginError.html($.t("error_hardware_required_login"));
                    $loginError.show();
                    return;
                }
                NRS.loginWithOptions({ bip32Account: bip32Account.toJsonString() });
            } else if(type === "seed") {
                let $selectedOption = $("#login_seed_account").children("option:selected");
                let offset = $selectedOption.attr("offset");
                let bip32Account;
                if (offset !== undefined) {
                    bip32Account = NRS.bip32Accounts[parseInt(offset)];
                }
                if (!bip32Account) {
                    $loginError.html($.t("error_seed_required_login"));
                    $loginError.show();
                    return;
                }
                NRS.loginWithOptions({ bip32Account: bip32Account.toJsonString() });
            }
        });

        NRS.getLoginType = function() {
            return $loginButtons.children(".active").data("tab");
        };

        $("#login_seed_load_accounts").on("click", async function(e) {
            e.preventDefault();
            $(this).prop("disabled", true);
            try {
                const mnemonic = $loginSeed.val();
                const $seedWalletInfo = $("#seed_wallet_info");
                if (mnemonic === "") {
                    const serializedMasterPublicKey = $("#login_master_public_key").val();
                    if (serializedMasterPublicKey === "") {
                        $seedWalletInfo.text($.t("either_mnemonic_or_master_public_key"));
                        return;
                    }
                    if (serializedMasterPublicKey.length !== 136) {
                        $seedWalletInfo.text($.t("invalid_master_public_key"));
                        return;
                    }
                    $seedWalletInfo.text($.t("loading_accounts"));
                    await listBip32Accounts(false, "", serializedMasterPublicKey);
                    return;
                }
                const words = mnemonic.split(" ");
                if (!BIP39.isValidMnemonic(words)) {
                    $seedWalletInfo.text($.t("bip39_warn_invalid_checksum"));
                    return;
                }
                $seedWalletInfo.text($.t("loading_accounts"));
                await listBip32Accounts(false, mnemonic);
            } finally {
                $(this).prop("disabled", false);
            }
        });

        $("#login_hardware_load_accounts").on("click", async function(e) {
            e.preventDefault();
            $(this).prop("disabled", true);
            try {
                await listBip32Accounts(true);
            } finally {
                $(this).prop("disabled", false);
            }
        });

        const $showAdvancedLoadButton = $("#login_panel button.show_advanced_load");
        $showAdvancedLoadButton.on("click", function(e) {
            e.preventDefault();
            $showAdvancedLoadButton.find("span").text(
                $(".advanced_load").toggleClass("hidden").hasClass("hidden") ? $.t("advanced") : $.t("basic")
            )
        });

        $(".creation_link").on("click", function(e) {
            e.preventDefault();
            let creationType = $(this).data("entityType");
            $("#entity_creation_type").val(creationType);
            loadMnemonic();
            $("#secret_generator_wordlist_length").focus();
        });

        $(".create-mnemonic-cancel").on("click", function(e) {
            e.preventDefault();
            showLoginScreen();
        });

        let $chainSelector = $("#login_panel .chainSelector");

        function getSelectedChain() {
            return $chainSelector.find("span.currentChain").data("chain");
        }

        function setSelectedChain(chainId) {
            $chainSelector.find("span.currentChain").data("chain", chainId).html(
                $chainSelector.find(`li:nth-child(${chainId}) a span`).html()
            );
        }

        $chainSelector.on('click', 'li', function (e) {
            e.preventDefault();
            setSelectedChain($(this).data('chain'));
        });

        NRS.createChainSelect = function() {
            const $list = $chainSelector.find("ul");
            $list.empty();
            $.each(NRS.constants.CHAIN_PROPERTIES, function(id, chain) {
                const chainName = NRS.getChainDisplayName(chain.name);
                const $li = $('<li></li>');
                $li.data('chain', id);
                const chainIconAndName = `<img src="img/chains/${id}.png" alt="${chainName}"> ${chainName}`;
                $li.html(`<a href="#"><span>${chainIconAndName}</span> ${NRS.getChainDescription(id)}</a>`);
                $list.append($li);
                if (id === NRS.getActiveChainId()) {
                    // $('#login_panel .chainSelector span.currentChain').data('chain', id).html(chainIconAndName);
                    setSelectedChain(id);
                }
            });
        };

        NRS.loginWithOptions = async function(options, callback) {
            NRS.spinner.spin($(".spinner_container")[0]);
            let asyncCallsDefer = $.Deferred();
            let $loginError = $("#login_error");
            $loginError.hide();
            if (!options) {
                options = {};
            }
            let isPassphraseLogin = false, id, bip32Account, privateKey, isBip32Login = false, isBip32PrivateKeyLogin = false;
            if (options.bip32Account !== undefined) {
                isBip32Login = true;
                let bip32Obj = JSON.parse(options.bip32Account);
                bip32Account = NRS.createBip32Account(bip32Obj.provider, bip32Obj.privateKey, bip32Obj.publicKey, bip32Obj.account, bip32Obj.path, undefined);
                if (bip32Account.getProvider() === NRS.BIP32_PROVIDER.SOFTWARE) {
                    if (bip32Account.getPrivateKey() !== null) {
                        isBip32PrivateKeyLogin = true;
                        id = bip32Account.getPrivateKey();
                    } else {
                        id = bip32Account.getAccount();
                    }
                } else {
                    id = bip32Account.getAccount();
                }
            } else {
                isPassphraseLogin = !!options.isPassphraseLogin;
                id = options.id;
            }
            NRS.logConsole("login isPassphraseLogin = " + isPassphraseLogin +
                ", isAccountSwitch = " + options.isAccountSwitch +
                ", bip32Account = " + (isBip32Login ? bip32Account.toString() : "N/A") +
                ", isPreventLoginToNewAccount = " + options.isPreventLoginToNewAccount);
            if (isPassphraseLogin){
                let loginCheckPasswordLength = $("#login_check_password_length");
                if (!id.length) {
                    $loginError.html($.t("error_passphrase_required_login_v2"));
                    $loginError.show();
                    NRS.spinner.stop();
                    return asyncCallsDefer.resolve();
                } else if (id.length < 12 && loginCheckPasswordLength.val() == 1) {
                    loginCheckPasswordLength.val(0);
                    $loginError.html($.t("error_passphrase_login_length"));
                    $loginError.show();
                    NRS.spinner.stop();
                    return asyncCallsDefer.resolve();
                }

                $("#login_password, #login_password_account, #registration_password, #registration_password_repeat").val("");
                loginCheckPasswordLength.val("1");
            }
            if (!options.chain) {
                options.chain = getSelectedChain();
            } else {
                setSelectedChain(options.chain);
            }
            NRS.setActiveChain(options.chain);
            console.log("login calling getBlockchainStatus, active chain is " + NRS.getActiveChainName());
            let response = await NRS.sendRequestAndWait("getBlockchainStatus", {});
            if (response.errorCode) {
                NRS.connectionError(response.errorDescription, response.errorCode);
                NRS.spinner.stop();
                console.log("getBlockchainStatus returned error");
                return asyncCallsDefer.resolve();
            }
            console.log("getBlockchainStatus response received");
            NRS.state = response;
            let accountRequest;
            let requestVariable;
            if (isPassphraseLogin || isBip32PrivateKeyLogin) {
                accountRequest = "getAccountId"; // Processed locally, not submitted to server
                privateKey = NRS.getPrivateKey(id);
                requestVariable = { privateKey: privateKey };
            } else {
                accountRequest = "getAccount";
                if (id.length > 1 && id.charAt(0) === '@') {
                    let result = await NRS.getAccountAlias(id.substring(1));
                    if (result.error) {
                        $.growl(result.error, {
                            "type": "danger",
                            "offset": 10
                        });
                        NRS.spinner.stop();
                        return asyncCallsDefer.resolve(result.error);
                    }
                    id = result.id;
                }
                if (id.split("-").length === 5) {
                    NRS.deviceSettings.account_prefix = id.split("-")[0];
                }
                requestVariable = {account: id};
            }
            console.log("calling " + accountRequest);
            response = await NRS.sendRequestAndWait(accountRequest, requestVariable);
            let data = response.requestData;
            console.log(accountRequest + " response received");
            if (!response.errorCode) {
                NRS.account = NRS.escapeRespStr(response.account);
                NRS.accountRS = NRS.escapeRespStr(response.accountRS);
                if (isPassphraseLogin) {
                    NRS.publicKey = NRS.getPublicKeyFromPrivateKey(privateKey);
                } else {
                    if (isBip32Login) {
                        if (response.publicKey !== undefined && bip32Account.getPublicKey() !== response.publicKey) {
                            $.growl($.t("error_hardware_wallet_public_key_mismatch"), {
                                "type": "danger",
                                "offset": 10
                            });
                            return;
                        }
                        NRS.publicKey = bip32Account.getPublicKey();
                        NRS.bip32Account = bip32Account;
                    } else if (response.publicKey !== undefined) {
                        NRS.publicKey = NRS.escapeRespStr(response.publicKey);
                        NRS.bip32Account = undefined;
                    }
                }
            }
            if (response.errorCode == 5) {
                if (isPassphraseLogin && options.isPreventLoginToNewAccount) {
                    let privateKey = NRS.getPrivateKey(id);
                    let accountRS = NRS.getAccountId(privateKey, true);
                    $loginError.html($.t("passphrase_login_to_new_account", { account: accountRS }));
                    $loginError.show();
                    NRS.spinner.stop();
                    return asyncCallsDefer.resolve();
                } else {
                    NRS.account = NRS.escapeRespStr(response.account);
                    NRS.accountRS = NRS.escapeRespStr(response.accountRS);
                    if (isPassphraseLogin) {
                        NRS.publicKey = NRS.getPublicKeyFromPrivateKey(privateKey);
                    } else {
                        if (isBip32Login) {
                            NRS.publicKey = bip32Account.getPublicKey();
                            NRS.bip32Account = bip32Account;
                        }
                    }
                }
            }
            if (response.errorCode == 19 || response.errorCode == 21) {
                $.growl($.t("light_client_connecting_to_network"), {
                    "type": "danger",
                    "offset": 10
                });
                NRS.spinner.stop();
                return asyncCallsDefer.resolve();
            }
            if (!NRS.account) {
                $loginError.html($.t("error_find_account_id", { accountRS: (data && data.account ? String(data.account).escapeHTML() : "") }));
                $loginError.show();
                NRS.spinner.stop();
                return asyncCallsDefer.resolve();
            } else if (!NRS.accountRS) {
                $.growl($.t("error_generate_account_id"), {
                    "type": "danger",
                    "offset": 10
                });
                NRS.spinner.stop();
                return asyncCallsDefer.resolve();
            }

            response = await NRS.sendRequestAndWait("getAccountPublicKey", { "account": NRS.account });
            if (response && response.publicKey) {
                if (isPassphraseLogin && response.publicKey !== NRS.generatePublicKey(privateKey) || isBip32Login && response.publicKey !== bip32Account.getPublicKey()) {
                    $.growl($.t("error_account_taken"), {
                        "type": "danger",
                        "offset": 10
                    });
                    NRS.spinner.stop();
                    return asyncCallsDefer.resolve();
                }
            }

            let $rememberMe = $("#remember_me");
            if ($rememberMe.is(":checked") && isPassphraseLogin || isBip32PrivateKeyLogin) {
                NRS.rememberPassword = true;
                NRS.setPassword(isPassphraseLogin && id !== privateKey ? id : "", privateKey);
                $(".secret_phrase, .show_secret_phrase").hide();
                $(".hide_secret_phrase").show();
            } else {
                NRS.rememberPassword = false;
                NRS.setPassword("", "");
                if (NRS.isHdWalletPrivateKeyAvailable()) {
                    $(".secret_phrase, .show_secret_phrase").hide();
                } else {
                    $(".secret_phrase, .show_secret_phrase").show();
                }
                $(".hide_secret_phrase").hide();
            }
            NRS.disablePluginsDuringSession = $("#disable_all_plugins").is(":checked");
            $("#chain_name").html(NRS.getActiveChainName());
            $("#sidebar_account_id").html(String(NRS.accountRS).escapeHTML());
            $("#sidebar_account_link").html(NRS.getAccountLink(NRS, "account", NRS.accountRS, "details", false, "btn btn-default btn-xs"));
            if (NRS.lastBlockHeight == 0 && NRS.state.numberOfBlocks) {
                NRS.checkBlockHeight(NRS.state.numberOfBlocks - 1);
            }
            if (NRS.lastBlockHeight == 0 && NRS.lastProxyBlockHeight) {
                NRS.checkBlockHeight(NRS.lastProxyBlockHeight);
            }
            $("#sidebar_block_link").html(NRS.getBlockLink(NRS.lastBlockHeight));

            var passwordNotice = "";

            if (id.length < 35 && isPassphraseLogin) {
                passwordNotice = $.t("error_passphrase_length_secure");
            } else if (isPassphraseLogin && id.length < 50 && (!id.match(/[A-Z]/) || !id.match(/[0-9]/))) {
                passwordNotice = $.t("error_passphrase_strength_secure");
            }

            if (passwordNotice) {
                $.growl("<strong>" + $.t("warning") + "</strong>: " + passwordNotice, {
                    "type": "danger"
                });
            }
            var startForgingDefer = $.Deferred();
            var createDBDefer = $.Deferred();
            $.when(startForgingDefer, createDBDefer).then(function() {
                asyncCallsDefer.resolve();
            });
            NRS.getAccountInfo(true, async function() {
                if (NRS.accountInfo.currentLeasingHeightFrom) {
                    NRS.isLeased = (NRS.lastBlockHeight >= NRS.accountInfo.currentLeasingHeightFrom && NRS.lastBlockHeight <= NRS.accountInfo.currentLeasingHeightTo);
                } else {
                    NRS.isLeased = false;
                }
                NRS.updateForgingTooltip($.t("forging_unknown_tooltip"));
                await NRS.updateForgingStatus(privateKey);
                if (NRS.isForgingSafe() && privateKey) {
                    var forgingIndicator = $("#forging_indicator");
                    NRS.sendRequest("startForging", {
                        "privateKey": privateKey
                    }, function (response) {
                        if ("deadline" in response) {
                            forgingIndicator.addClass("forging");
                            forgingIndicator.find("span").html($.t("forging")).attr("data-i18n", "forging");
                            NRS.forgingStatus = NRS.constants.FORGING;
                            NRS.updateForgingTooltip(NRS.getForgingTooltip);
                        } else {
                            forgingIndicator.removeClass("forging");
                            forgingIndicator.find("span").html($.t("not_forging")).attr("data-i18n", "not_forging");
                            NRS.forgingStatus = NRS.constants.NOT_FORGING;
                            NRS.updateForgingTooltip(response.errorDescription);
                        }
                        forgingIndicator.show();
                        startForgingDefer.resolve();
                    });
                } else {
                    startForgingDefer.resolve();
                }
            }, options.isAccountSwitch);
            NRS.initSidebarMenu();
            NRS.initHeader();
            NRS.unlock();
            if (NRS.activeTour) {
                NRS.activeTour.end();
            }
            NRS.tours.dashboard();

            if (NRS.isOutdated) {
                $.growl($.t("nrs_update_available"), {
                    "type": "danger"
                });
            }

            if (!NRS.downloadingBlockchain) {
                NRS.checkIfOnAFork();
            }
            NRS.logConsole("User Agent: " + String(navigator.userAgent));
            if (navigator.userAgent.indexOf('Safari') != -1 &&
                navigator.userAgent.indexOf('Chrome') == -1 &&
                navigator.userAgent.indexOf('JavaFX') == -1) {
                // Don't use account based DB in Safari due to a buggy indexedDB implementation (2015-02-24)
                NRS.createDatabase("NRS_USER_DB").then(function() {
                    createDBDefer.resolve();
                });
                $.growl($.t("nrs_safari_no_account_based_db"), {
                    "type": "danger"
                });
            } else {
                NRS.createDatabase("NRS_USER_DB_" + String(NRS.account)).then(function() {
                    createDBDefer.resolve();
                });
            }
            if (callback !== undefined) {
                callback();
            }
            $.each(NRS.pages, function(key) {
                if(key in NRS.setup) {
                    NRS.setup[key]();
                }
            });

            $(".sidebar .treeview").tree();
            $('#dashboard_link').find('a').addClass("ignore").click();

            if ($("#bookmark_account").is(":checked") || $rememberMe.is(":checked") || NRS.newlyCreatedAccount) {
                rememberAccount(NRS.accountRS);
            }

            $("[data-i18n]").i18n();

            /* Add accounts to dropdown for quick switching */
            let $accountIdDropdown = $("#account_id_dropdown");
            $accountIdDropdown.find(".dropdown-menu .switchAccount, .divider").remove();
            let disableDropdown = true;
            for (let i=0; i < NRS.bip32Accounts.length; i++) {
                disableDropdown = false;
                let accountObj = NRS.bip32Accounts[i];
                $accountIdDropdown.find('.dropdown-menu').append($("<li class='switchAccount'></li>")
                    .append($("<a></a>")
                        .attr("href","#")
                        .attr("onClick","NRS.switchAccount('" + accountObj.getAccount() + "','" + NRS.getActiveChainId() + "')")
                        .text(accountObj.getAccount())
                    )
                )
            }
            if (NRS.bip32Accounts.length > 0) {
                $accountIdDropdown.find('.dropdown-menu').append($("<li role='presentation' class='divider'></li>"));
            }
            if (NRS.getStrItem("savedNxtAccounts") && NRS.getStrItem("savedNxtAccounts")!=""){
                disableDropdown = false;
                let accounts = NRS.getStrItem("savedNxtAccounts").split(";");
                $.each(accounts, function(index, account) {
                    if (account != '') {
                        $accountIdDropdown.find('.dropdown-menu')
                            .append($("<li class='switchAccount'></li>")
                                .append($("<a></a>")
                                    .attr("href","#")
                                    .attr("onClick","NRS.switchAccount('" + account + "','" + NRS.getActiveChainId() + "')")
                                    .text(account)
                                )
                            );
                    }
                });
            }
            if (disableDropdown) {
                $accountIdDropdown.find('button').prop('disabled', true);
            }

            /* Add chains to dropdown for quick switching */
            let chainDropdown = $("#chain_dropdown");
            chainDropdown.find(".dropdown-menu .switchAccount").remove();
            let chainDropdownMenu = chainDropdown.find(".dropdown-menu");
            for (let chainId in NRS.constants.CHAIN_PROPERTIES) {
                if (!NRS.constants.CHAIN_PROPERTIES.hasOwnProperty(chainId)) {
                    continue;
                }
                let chain = NRS.constants.CHAIN_PROPERTIES[chainId];
                chainDropdownMenu.append($("<li class='switchAccount'></li>")
                    .append($("<a></a>")
                        .attr("href", "#")
                        .attr("onClick", "NRS.switchAccount('" + NRS.accountRS + "','" + chain.id + "')")
                        .text(NRS.getChainDisplayName(chain.name) + " " + NRS.getChainDescription(chain.id))
                    )
                );
            }
            // Used to switch to another chain
            loginOptions.isPassphraseLogin = isPassphraseLogin;
            loginOptions.id = id;
            loginOptions.bip32Account = options.bip32Account;
            NRS.updateApprovalRequests();
            return asyncCallsDefer;
        };

        $("#logout_button_container").on("show.bs.dropdown", function() {
            if (NRS.forgingStatus != NRS.constants.FORGING) {
                $(this).find("[data-i18n='logout_stop_forging']").hide();
            }
        });

        NRS.initPluginWarning = function() {
            if (NRS.activePlugins) {
                var html = "";
                html += "<div style='font-size:13px;'>";
                html += "<div style='background-color:#e6e6e6;padding:12px;'>";
                html += "<span data-i18n='following_plugins_detected'>";
                html += "The following active plugins have been detected:</span>";
                html += "</div>";
                html += "<ul class='list-unstyled' style='padding:11px;border:1px solid #e0e0e0;margin-top:8px;'>";
                $.each(NRS.plugins, function(pluginId, pluginDict) {
                    if (pluginDict["launch_status"] == NRS.constants.PL_PAUSED) {
                        html += "<li style='font-weight:bold;'>" + pluginDict["manifest"]["name"] + "</li>";
                    }
                });
                html += "</ul>";
                html += "</div>";

                $('#lockscreen_active_plugins_overview').popover({
                    "html": true,
                    "content": html,
                    "trigger": "hover"
                });

                html = "";
                html += "<div style='font-size:13px;padding:5px;'>";
                html += "<p data-i18n='plugin_security_notice_full_access'>";
                html += "Plugins are not sandboxed or restricted in any way and have full accesss to your client system including your Nxt passphrase.";
                html += "</p>";
                html += "<p data-i18n='plugin_security_notice_trusted_sources'>";
                html += "Make sure to only run plugins downloaded from trusted sources, otherwise ";
                html += "you can loose your funds! In doubt don't run plugins with accounts ";
                html += "used to store larger amounts of funds now or in the future.";
                html += "</p>";
                html += "</div>";

                $('#lockscreen_active_plugins_security').popover({
                    "html": true,
                    "content": html,
                    "trigger": "hover"
                });

                $("#lockscreen_active_plugins_warning").show();
            } else {
                $("#lockscreen_active_plugins_warning").hide();
            }
        };

        NRS.showLockScreen = function() {
            NRS.listAccounts();
            showLoginScreen();
            $('#progress-container').hide();
            $('html, body, #lockscreen').removeClass('loading');
            if (!NRS.isShowDummyCheckbox()) {
                $("#dummyCheckbox").hide();
            }
        };

        NRS.unlock = function() {
            $("#lockscreen").hide();
            $("body, html").removeClass("lockscreen");
            $("#login_error").html("").hide();
            $(document.documentElement).scrollTop = 0;
            NRS.spinner.stop();
        };

        NRS.logout = function(stopForging) {
            if (stopForging && NRS.forgingStatus == NRS.constants.FORGING) {
                var stopForgingModal = $("#stop_forging_modal");
                stopForgingModal.find(".show_logout").show();
                stopForgingModal.modal("show");
            } else {
                NRS.setEncryptionPrivateKey("");
                NRS.setDecryptionPrivateKey("");
                NRS.setPassword("");
                //window.location.reload();
                if (NRS.isAndroidWebView()) {
                    androidWebViewInterface.reload();
                } else {
                    window.location.href = window.location.pathname;
                }

            }
        };

        NRS.logoutAndClear = function () {
            NRS.showConfirmModal($.t('warning'), $.t('confirm_logout_clear_user_data_msg'), $.t('continue'),
                () => {
                    if (NRS.database) {
                        //noinspection JSUnresolvedFunction
                        indexedDB.deleteDatabase(NRS.database.name);
                    }
                    if (NRS.legacyDatabase) {
                        //noinspection JSUnresolvedFunction
                        indexedDB.deleteDatabase(NRS.legacyDatabase.name);
                    }
                    NRS.removeItem("logged_in");
                    NRS.removeItem("savedNxtAccounts");
                    NRS.removeItem("language");
                    NRS.removeItem("savedPassphrase");
                    NRS.localStorageDrop("data");
                    NRS.localStorageDrop("polls");
                    NRS.localStorageDrop("contacts");
                    NRS.localStorageDrop("assets");
                    Object.keys(NRS.tours).forEach((tourName) => {
                        NRS.removeItem(tourName + "_current_step");
                        NRS.removeItem(tourName + "_end");
                    });
                    NRS.logout();
                });
        };

        NRS.setPassword = function(secretPhrase, privateKey) {
            NRS.setEncryptionPrivateKey(privateKey);
            NRS.setServerPrivateKey(privateKey);
            NRS.setAccountDetailsPrivateKey(privateKey);
            NRS.setAccountDetailsPassword(secretPhrase);
            NRS.setAdvancedModalPrivateKey(privateKey);
            NRS.setApprovalModelsPrivateKey(privateKey);
            NRS.setTokenPrivateKey(privateKey);
        };
        return NRS;


    }(NRS || {}, jQuery));
});
