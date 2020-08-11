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
		$("#hash_modal").on("show.bs.modal", function(e) {
			$("#hash_calculation_output").html("").hide();
			$("#hash_modal_button").data("form", "calculate_hash_form");
		});

		NRS.forms.hash = function($modal) {
			var data = $.trim($("#calculate_hash_data").val());
			if (!data) {
				$("#hash_calculation_output").html("").hide();
				return {
					"error": "Data is a required field."
				};
			} else {
				return {};
			}
		};

		NRS.forms.hashComplete = function(response, data) {
			$("#hash_modal").find(".error_message").hide();

			if (response.hash) {
				$("#hash_calculation_output").html($.t("calculated_hash_is") + "<br/><br/>" +
					"<textarea style='width:100%' rows='3'>" + NRS.escapeRespStr(response.hash) + "</textarea>").show();
			} else {
				$.growl($.t("error_calculate_hash"), {
					"type": "danger"
				});
			}
		};

		NRS.forms.hashError = function() {
			$("#hash_calculation_output").hide();
		};

		let $bip32CalculationsModal = $("#bip32_calculations_modal");

		function updateModal($modal, calculationType) {
			let $passphraseWarningContainer = $modal.find(".passphrase_warning_container");
			let children = $modal.find(".seed_entry").find(":input");
			if (calculationType === "client") {
				$passphraseWarningContainer.hide();
				children.prop("disabled", false);
			} else if (calculationType === "server") {
				$passphraseWarningContainer.show();
				children.prop("disabled", false);
			} else {
				$passphraseWarningContainer.hide();
				children.prop("disabled", true);
			}
			$modal.find(".btn-group").find(".btn").each(function () {
				let type = $(this).data("type");
				if (type === calculationType) {
					$(this).addClass("active");
				} else {
					$(this).removeClass("active");
				}
			});
		}

		$bip32CalculationsModal.find(".btn-group").find(".btn").each(function() {
			$(this).bind("click", function() {
				let calculationType = $(this).data("type");
				$("input[name=calculation_type]").val(calculationType);
				if (NRS.isBip32CalculatorDisableClientOption() && calculationType === "client") {
					$.growl($.t("not_supported_in_desktop_wallet"));
					return;
				}
				let $modal = $(this).closest(".modal");
				updateModal($modal, calculationType);
			});
		});

		$bip32CalculationsModal.on("show.bs.modal", function() {
			let $modal = $(this);
			let calculationType = NRS.getBip32CalculatorType();
			$modal.find("input[name=calculation_type]").val(calculationType);
			$modal.find("input[name=bip32Path]").val(BIPPath.fromPathArray(NRS.constants.BIP32_PATH_PREFIX).toString());
			updateModal($modal, calculationType);
		});

		NRS.deriveAccountFromSeed = async function(calculationType, data) {
			if (calculationType === "server") {
				delete data.calculation_type;
				return await NRS.sendRequestAndWait("deriveAccountFromSeed", data);
			}
			let bip32Path = data["bip32Path"];
			let mnemonic = data["mnemonic"];
			let passphrase = data["passphrase"];
			let seed = KeyDerivation.mnemonicAndPassphraseToSeed(mnemonic, passphrase);
			let bip32Node = KeyDerivation.deriveSeed(bip32Path, seed);
			let publicKeyHex = converters.byteArrayToHexString(bip32Node.getPublicKey());
			let accountId = NRS.getAccountIdFromPublicKey(publicKeyHex);
			let accountRS = NRS.convertNumericToRSAccountFormat(accountId);
			return {
				seed: converters.byteArrayToHexString(seed),
				privateKey: converters.byteArrayToHexString(bip32Node.getPrivateKeyLeft()),
				masterPublicKey: converters.byteArrayToHexString(bip32Node.getMasterPublicKey()),
				serializedMasterPublicKey: converters.byteArrayToHexString(bip32Node.getSerializedMasterPublicKey()),
				chainCode: converters.byteArrayToHexString(bip32Node.getChainCode()),
				publicKey: publicKeyHex,
				accountRS: accountRS,
				account: accountId
			};
		};

		NRS.forms.deriveAccountFromSeed = async function($modal) {
			let data = NRS.getFormData($modal.find("form:first"));
			let calculationType = data["calculation_type"];
			delete data.calculation_type;
			if (calculationType === "server") {
				return { data: data };
			}
			let bip32Path = data["bip32Path"];
			if (!BIPPath.validateString(bip32Path)) {
				return { stop: true, keepOpen: true,  errorMessage: $.t("invalid_bip32_path")};
			}
			let publicKeyHex;
			if (calculationType === "client") {
				let response = await NRS.deriveAccountFromSeed(calculationType, data);
				NRS.forms.deriveAccountFromSeedComplete(response);
				return { stop: true, keepOpen: true };
			} else {
				// hardware
				let response = await NRS.getPublicKeyFromHardwareWallet(bip32Path, true);
				$("#seed_private_key").val($.t("unavailable"));
				if (response.length == 96) {
					$("#seed_master_public_key").val(converters.byteArrayToHexString(KeyDerivation.computeSerializedMasterPublicKey(response.slice(32, 64), response.slice(64, 96))));
					publicKeyHex = converters.byteArrayToHexString(response.slice(0, 32));
					$("#seed_public_key").val(publicKeyHex);
					let accountId = NRS.getAccountIdFromPublicKey(publicKeyHex);
					let accountRS = NRS.convertNumericToRSAccountFormat(accountId);
					$("#seed_account").val(accountId);
					$("#seed_accountRS").val(accountRS);
				} else {
					$("#seed_master_public_key").val($.t("device_not_connected_or_locked"));
					$("#seed_public_key").val($.t("device_not_connected_or_locked"));
					$("#seed_account").val("");
					$("#seed_accountRS").val("");
				}
				return { stop: true, keepOpen: true };
			}
		};

		NRS.forms.deriveAccountFromSeedComplete = function(response) {
			let $modal = $("#bip32_calculations_modal");
			if (response.errorDescription) {
				$modal.find(".error_message").text(response.errorDescription);
				$modal.find(".error_message").show();
				return;
			}
			$modal.find(".error_message").hide();
			$modal.find("#seed_seed").val(response.seed);
			if (response.privateKey) {
				$modal.find("#seed_private_key").val(response.privateKey);
				$modal.find("#seed_master_public_key").val(response.serializedMasterPublicKey);
				$modal.find("#seed_public_key").val(response.publicKey);
				$modal.find("#seed_accountRS").val(response.accountRS);
				$modal.find("#seed_account").val(response.account);
			} else {
				$modal.find("#seed_private_key").val("");
				$modal.find("#seed_master_public_key").val("");
				$modal.find("#seed_public_key").val("");
				$modal.find("#seed_accountRS").val("");
				$modal.find("#seed_account").val("");
			}
		};

		return NRS;
	}(NRS || {}, jQuery));
});