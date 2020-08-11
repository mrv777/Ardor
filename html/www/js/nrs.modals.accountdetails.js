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
 * @depends {nrs.modals.js}
 */
NRS.onSiteBuildDone().then(() => {
	NRS = (function(NRS, $, undefined) {
		let _password = null;
		NRS.setAccountDetailsPassword = function(password) {
			_password = password;
		};

		let sessionPrivateKey = null;
		NRS.setAccountDetailsPrivateKey = function(privateKey) {
			sessionPrivateKey = privateKey;
		};

		let $accountDetailsModal = $("#account_details_modal");
		let $accountDetailsModalPrivateKeyShowButton = $accountDetailsModal.find("#account_details_modal_private_key_show_button");
		let $accountDetailsModalPrivateKeyText = $accountDetailsModal.find("#account_details_modal_private_key_text");

		$accountDetailsModal.on("show.bs.modal", function(e) {
			if (sessionPrivateKey) {
				$accountDetailsModalPrivateKeyText.hide();
				$accountDetailsModalPrivateKeyShowButton.show();
			} else {
				$accountDetailsModalPrivateKeyText.show();
				$accountDetailsModalPrivateKeyText.text($.t("private_key_not_available"));
				$accountDetailsModalPrivateKeyShowButton.hide();
			}
			if (_password) {
				$("#account_details_modal_account_display").show();
				$("#account_details_modal_passphrase_display").show();
				$("#account_details_modal_paperwallet_create").show();
				$("#account_details_modal_paperwallet_na").empty();
			} else {
				NRS.generateQRCode("#account_details_modal_account_qr_code", NRS.accountRS);
				$("#account_details_modal_account_display").hide();
				$("#account_details_modal_passphrase_display").hide();
				$("#account_details_modal_paperwallet_create").hide();
				$("#account_details_modal_passphrase_qr_code").html($.t("passphrase_not_available"));
				$("#account_details_modal_paperwallet_na").html($.t("passphrase_not_available"));
			}
			$("#account_details_modal_balance").show();

			var accountBalanceWarning = $("#account_balance_warning");
			if (NRS.accountInfo.errorCode && NRS.accountInfo.errorCode != 5) {
				$("#account_balance_table").hide();
				accountBalanceWarning.html(NRS.escapeRespStr(NRS.accountInfo.errorDescription)).show();
			} else {
				accountBalanceWarning.hide();
				var accountBalancePublicKey = $("#account_balance_public_key");
				if (NRS.accountInfo.errorCode && NRS.accountInfo.errorCode == 5) {
					$("#account_balance_balance, #account_balance_unconfirmed_balance").html("0 " + NRS.getActiveChainName());
					$("#account_balance_effective_balance, #account_balance_guaranteed_balance, #account_balance_forged_balance").html("0 " + NRS.getParentChainName());
					accountBalancePublicKey.html(NRS.escapeRespStr(NRS.publicKey));
					$("#account_balance_account_rs").html(NRS.getAccountLink(NRS, "account", undefined, undefined, true));
					$("#account_balance_account").html(NRS.escapeRespStr(NRS.account));
				} else {
					$("#account_balance_balance").html(NRS.formatAmount(new BigInteger(NRS.accountInfo.balanceNQT)) + " " + NRS.getActiveChainName());
					$("#account_balance_unconfirmed_balance").html(NRS.formatAmount(new BigInteger(NRS.accountInfo.unconfirmedBalanceNQT)) + " " + NRS.getActiveChainName());
					$("#account_balance_effective_balance").html(NRS.formatAmount(NRS.accountInfo.effectiveBalanceFXT, false, false, false, 0) + " " + NRS.getParentChainName());
					$("#account_balance_guaranteed_balance").html(NRS.formatAmount(new BigInteger(NRS.accountInfo.guaranteedBalanceFQT), false, false, false, NRS.getChain("1").decimals) + " " + NRS.getParentChainName());
					$("#account_balance_forged_balance").html(NRS.formatAmount(new BigInteger(NRS.accountInfo.forgedBalanceFQT), false, false, false, NRS.getChain("1").decimals) + " " + NRS.getParentChainName());

					accountBalancePublicKey.html(NRS.escapeRespStr(NRS.accountInfo.publicKey));
					$("#account_balance_account_rs").html(NRS.getAccountLink(NRS.accountInfo, "account", undefined, undefined, true));
					$("#account_balance_account").html(NRS.escapeRespStr(NRS.account));

					if (!NRS.accountInfo.publicKey) {
						accountBalancePublicKey.html("/");
						var warning = NRS.publicKey != 'undefined' ? $.t("public_key_not_announced_warning", { "public_key": NRS.publicKey }) : $.t("no_public_key_warning");
						accountBalanceWarning.html(warning + " " + $.t("public_key_actions")).show();
					}
				}
				if (NRS.isBip32PathAvailable()) {
					var pathStr = NRS.bip32Account.getPath();
					$("#account_balance_bip32_path").html(pathStr);
					$(".ledger_account_details").show();
					if (NRS.isHardwareShowAddressEnabled()) {
						$(".ledger_account_details .btn").show();
					} else {
						$(".ledger_account_details .btn").hide();
					}
				} else {
					$(".ledger_account_details").hide();
				}
				if (NRS.isActivePermissionPolicyChildChain()) {
					$("#account_permissions_row").show();
					NRS.getAccountPermissionsPromise().then(function(accountPermissions) {
						const activeAccountChainPermissions = accountPermissions.hasPermissions;
						if(activeAccountChainPermissions && activeAccountChainPermissions.length) {
							const chainPermissionsLabels = activeAccountChainPermissions.map(function (accountPermission) {
								return $.t(NRS.PERMISSIONS_TO_TRANSLATION_KEYS[accountPermission.permission]);
							});
							$("#account_permissions_details").html(NRS.escapeRespStr(chainPermissionsLabels.join(', ')));
						} else {
							$("#account_permissions_details").html($.t("none"));
						}
					});
				} else {
					$("#account_permissions_row").hide();
				}
			}

			NRS.setupChainWarning($("#lease_balance_link"), true);

			NRS.setupChainWarning($("a[data-target=\\#set_mandatory_approval_modal]"), false);

			var $invoker = $(e.relatedTarget);
			var tab = $invoker.data("detailstab");
			if (tab) {
				_showTab(tab)
			}
		});

		function _showTab(tab){
			var tabListItem = $accountDetailsModal.find("li[data-tab=" + tab + "]");
			tabListItem.siblings().removeClass("active");
			tabListItem.addClass("active");
			$(".account_details_modal_content").hide();
			var content = $("#account_details_modal_" + tab);
			if (tab == "leasing") {
				NRS.updateAccountLeasingStatus();
			}
			content.show();
		}

		$accountDetailsModal.find("ul.nav li").click(function(e) {
			e.preventDefault();
			var tab = $(this).data("tab");
			_showTab(tab);
		});

		$accountDetailsModal.on("hidden.bs.modal", function() {
			$(this).find(".account_details_modal_content").hide();
			$(this).find("ul.nav li.active").removeClass("active");
			$("#account_details_balance_nav").addClass("active");
			$("#account_details_modal_account_qr_code").empty();
			$("#account_details_modal_passphrase_qr_code").empty();
		});

		$accountDetailsModalPrivateKeyShowButton.on("click", function() {
			$accountDetailsModalPrivateKeyText.show();
			$accountDetailsModalPrivateKeyText.text(sessionPrivateKey);
			$accountDetailsModalPrivateKeyShowButton.hide();
		});

		$("#account_details_modal_account_display").on("click", function() {
			$("#account_details_modal_account_display").hide();
			$("#account_details_modal_passphrase_display").show();
			$("#account_details_modal_passphrase_qr_code").empty();
			NRS.generateQRCode("#account_details_modal_account_qr_code", NRS.accountRS);
		});

		$("#account_details_modal_passphrase_display").on("click", function() {
			$("#account_details_modal_passphrase_display").hide();
			$("#account_details_modal_account_display").show();
			$("#account_details_modal_account_qr_code").empty();
			NRS.generateQRCode("#account_details_modal_passphrase_qr_code", _password);
		});

		$("#create_paper_wallet_modal").on("show.bs.modal", function () {
			var $modal = $(this);
			var $enableSecretSharing = $modal.find("input[name=enableSecretSharing]");
			var $totalPieces = $modal.find("input[name=totalPieces]");
			var $requiredPieces = $modal.find("input[name=requiredPieces]");
			if ($totalPieces.val() === "") {
				$enableSecretSharing.prop("checked", true);
				$totalPieces.val("3");
				$requiredPieces.val("2");
			}
		});

		NRS.forms.createPaperWallet = function ($modal) {
			// Since we don't want to submit the secret in the form data since this exposes it in too many layers
			// we cannot control. We implemented this hack, if this modal originates from the account details modal the
			// _password will be available. Otherwise we assume that the secret was just generated from the create
			// passphrase or create seed wizard
			let secret;
			let secretType;
			if (_password) {
				secret = _password;
				secretType = "passphrase";
			} else {
				secret = $("#account_phrase_generator_panel").find(".step_2 textarea").val();
				secretType = NRS.getLoginType();
			}
			let data = NRS.getFormData($modal.find("form:first"));
			let isSecretSharingEnabled = data.enableSecretSharing;
			if (isSecretSharingEnabled) {
				let n = parseInt(data.totalPieces);
				let k = parseInt(data.requiredPieces);
				if (n > 0 && n < 10 && k > 0 && k < n) {
					NRS.printPaperWallet(secret, secretType, n, k);
					return { stop: true };
				} else {
					return { error: $.t("number_of_pieces") };
				}
			}
			NRS.printPaperWallet(secret, secretType);
			return { stop: true };
		};

		$("input[name=enableSecretSharing]").on("change", function () {
			var $form = $(this).closest("form");
			if ($(this).is(":checked")) {
				$form.find("input[name=totalPieces]").prop("disabled", false);
				$form.find("input[name=requiredPieces]").prop("disabled", false);
			} else {
				$form.find("input[name=totalPieces]").prop("disabled", true);
				$form.find("input[name=requiredPieces]").prop("disabled", true);
			}
		});

		$("#ledger_show_address_modal").on("shown.bs.modal", function () {
			$(this).find(".hardware-status").html($.t("hardware_wallet_waiting_for_commands"));
			$(this).find("input[name=verifyAddress]").val(NRS.accountRS);
			NRS.showAddressOnHardwareWallet(NRS.bip32Account.getPath());
			setTimeout(() => {
				$(this).find(".hardware-status").html($.t("address_is_displayed_on_device"));
			}, 2500);
		});

		$("#account_info_modal").on("show.bs.modal", function(e) {
			if (NRS.accountInfo.name !== undefined) {
				$("#account_info_name").val(NRS.unescapeRespStr(NRS.accountInfo.name));
			}
			if (NRS.accountInfo.description !== undefined) {
				$("#account_info_description").val(NRS.unescapeRespStr(NRS.accountInfo.description));
			}
		});

		return NRS;
	}(NRS || {}, jQuery));
});