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
(isNode ? client : NRS).onSiteBuildDone().then(() => {
	NRS = (function (NRS, $) {
		var encryptionPrivateKey;
		var decryptionPrivateKey;
		var _decryptedTransactions;
		var _encryptedNote;

		NRS.resetEncryptionState = function () {
			encryptionPrivateKey = null;
			decryptionPrivateKey = null;
			_decryptedTransactions = {};
			_encryptedNote = null;
		};
		NRS.resetEncryptionState();

		NRS.generatePublicKey = function(privateKey) {
			if (!privateKey) {
				if (NRS.rememberPassword) {
					privateKey = encryptionPrivateKey;
				} else {
					throw { message: $.t("error_generate_public_key_no_password") };
				}
			}

			return NRS.getPublicKeyFromPrivateKey(privateKey);
		};

		NRS.getPublicKeyFromAccountId = async function(id) {
			let response = await NRS.sendRequestAndWait("getAccountPublicKey", { "account": id });
			if (!response.publicKey) {
				throw $.t("error_no_public_key");
			} else {
				return response.publicKey;
			}
		};

		NRS.getPublicKeyFromSecretPhrase = function(secretPhrase) {
			var secretPhraseBytes = converters.hexStringToByteArray(converters.stringToHexString(secretPhrase));
			var digest = simpleHash(secretPhraseBytes);
			return converters.byteArrayToHexString(curve25519.keygen(digest).p);
		};

		NRS.getPublicKeyFromPrivateKey = function(privateKey) {
			return converters.byteArrayToHexString(curve25519.keygen(converters.hexStringToByteArray(privateKey)).p);
		};

		/**
		 * Identify if the secret does not represent a private key and if so converts it from a passphrase to a private key.
		 * @param secret passphrase or private key
		 * @returns {string|*} private key
		 */
		NRS.getPrivateKey = function(secret) {
			if (!(typeof secret === "string")) {
				throw "Secret must be a string";
			}
			if (secret.match("^[0-9a-fA-F]{64}$")) {
				return secret;
			}

			// In the unlikely case that someone already has a passphrase composed of exactly 64 hex characters
			// they can prepend the prefix below when typing so that it is not considered a private key.
			let prefix = "Passphrase:";
			if (secret.match("^" + prefix + "[0-9a-fA-F]{64}$")) {
				secret = secret.substring(prefix.length, prefix.length + 64);
			}
			if (secret === "") {
				throw "Cannot get private key for empty secret phrase. Report to developers."
			}
			let bytes = simpleHash(converters.stringToByteArray(secret));
			curve25519.clamp(bytes);
			return converters.byteArrayToHexString(bytes);
		};

		NRS.secretToPrivateKey = function(secret) {
			if (secret.match("^[0-9a-fA-F]{64}$")) {
				return secret;
			}

			// In the unlikely case that someone already has a passphrase composed of exactly 64 hex characters
			// they can prepend the prefix below when typing so that it is not considered a private key.
			let prefix = "Passphrase:";
			if (secret.match("^" + prefix + "[0-9a-fA-F]{64}$")) {
				secret = secret.substring(prefix.length, prefix.length + 64);
			}
			return NRS.getPrivateKey(secret);
		};

		NRS.getAccountId = function (privateKey, isRsFormat) {
			return NRS.getAccountIdFromPublicKey(NRS.getPublicKeyFromPrivateKey(privateKey), isRsFormat);
		};

		NRS.getAccountIdFromPublicKey = function(publicKey, isRsFormat) {
			var hex = converters.hexStringToByteArray(publicKey);
			var account = simpleHash(hex);
			account = converters.byteArrayToHexString(account);
			var slice = (converters.hexStringToByteArray(account)).slice(0, 8);
			var accountId = byteArrayToBigInteger(slice).toString();
			if (isRsFormat) {
				return NRS.convertNumericToRSAccountFormat(accountId);
			} else {
				return accountId;
			}
		};

		NRS.getEncryptionKeys = async function (options) {
			if (options.sharedKey) {
				return options;
			} else {
				if (!options.privateKey && !NRS.isHardwareEncryptionEnabled()) {
					if (!NRS.rememberPassword) {
						throw {
							"message": $.t("error_encryption_passphrase_required"),
							"errorCode": 1
						};
					}
					options.privateKey = converters.hexStringToByteArray(encryptionPrivateKey);
				} else if (typeof options.privateKey == "string") {
					options.privateKey = converters.hexStringToByteArray(options.privateKey);
				}

				if (!options.publicKey) {
					if (!options.account) {
						throw {
							"message": $.t("error_account_id_not_specified"),
							"errorCode": 2
						};
					}

					try {
						options.publicKey = converters.hexStringToByteArray(await NRS.getPublicKeyFromAccountId(options.account));
					} catch (err) {
						var nxtAddress = new NxtAddress();

						if (!nxtAddress.set(options.account)) {
							throw {
								"message": $.t("error_invalid_account_id"),
								"errorCode": 3
							};
						} else {
							throw {
								"message": $.t("error_public_key_not_specified"),
								"errorCode": 4
							};
						}
					}
				} else if (typeof options.publicKey == "string") {
					options.publicKey = converters.hexStringToByteArray(options.publicKey);
				}
				return options;
			}
		};

		NRS.encryptNote = async function(message, options) {
			try {
				options = await NRS.getEncryptionKeys(options);
				var encrypted = encryptData(converters.stringToByteArray(message), options);
				return {
					"message": converters.byteArrayToHexString(encrypted.data),
					"nonce": converters.byteArrayToHexString(encrypted.nonce)
				};
			} catch (err) {
				if (err.errorCode && err.errorCode < 5) {
					throw err;
				} else {
					throw {
						"message": $.t("error_message_encryption"),
						"errorCode": 5,
						"stack": err.stack
					};
				}
			}
		};

		NRS.decryptNote = async function(message, options) {
			try {
				if (!options.sharedKey) {
					if (!options.privateKey && !NRS.isHardwareDecryptionEnabled()) {
						let privateKey;
						if (NRS.rememberPassword) {
							privateKey = encryptionPrivateKey;
						} else if (decryptionPrivateKey) {
							privateKey = decryptionPrivateKey;
						} else {
							throw {
								"message": $.t("error_decryption_passphrase_required"),
								"errorCode": 1
							};
						}
						options.privateKey = converters.hexStringToByteArray(privateKey);
					} else if (typeof options.privateKey == "string") {
						options.privateKey = converters.hexStringToByteArray(options.privateKey);
					}

					if (!options.publicKey) {
						if (!options.account) {
							throw {
								"message": $.t("error_account_id_not_specified"),
								"errorCode": 2
							};
						}

						options.publicKey = await NRS.getPublicKeyFromAccountId(options.account);
					}
				}
				if (NRS.isHardwareDecryptionEnabled()) {
					return await NRS.decryptUsingHardwareWallet(message, options);
				} else {
					if (options.nonce) {
						options.nonce = converters.hexStringToByteArray(options.nonce);
					}
					if (options.publicKey) {
						options.publicKey = converters.hexStringToByteArray(options.publicKey);
					}
					return decryptData(converters.hexStringToByteArray(message), options);
				}
			} catch (err) {
				if (err.errorCode && err.errorCode < 3) {
					throw err;
				} else {
					let message = (typeof err === "string") ? err : err.message;
					NRS.logConsole(message);
					throw {
						"message": $.t("error_message_decryption"),
						"errorCode": 3
					};
				}
			}
		};

		/**
		 * Create a promise for signing locally or using a hardware wallet
		 * @param unsignedTransactionBytes transaction bytes as hex string
		 * @param privateKey private key as hex string
		 * @returns {Promise<unknown>} promise to wait for signing completion
		 */
		NRS.getSigningPromise = async function(unsignedTransactionBytes, privateKey) {
			return new Promise(async function(resolve) {
				let signature = await NRS.signBytesWrapper(unsignedTransactionBytes, privateKey);
				resolve(signature);
			});
		};

		NRS.signBytesWrapper = async function(unsignedTransactionBytes, privateKey) {
			let signature;
			if (NRS.isHardwareTransactionSigningEnabled()) {
				signature = await NRS.signBytesUsingHardwareWallet(unsignedTransactionBytes);
			} else {
				signature = NRS.signBytes(unsignedTransactionBytes, privateKey);
			}
			return signature;
		};

		NRS.signBytesWithPrivateKey = function (messageBytes, privateKey) {
			var s = curve25519.keygen(privateKey).s;
			var m = simpleHash(messageBytes);
			var x = simpleHash(m, s);
			var y = curve25519.keygen(x).p;
			var h = simpleHash(m, y);
			var v = curve25519.sign(h, x, s);
			// todo: would be cleaner to return byte array here and convert it outside if needed
			return converters.byteArrayToHexString(v.concat(h));
		};

		NRS.signBytes = function(message, privateKey) {
			if (!privateKey) {
				if (NRS.rememberPassword) {
					privateKey = encryptionPrivateKey;
				} else {
					throw {
						"message": $.t("error_signing_passphrase_required"),
						"errorCode": 1
					};
				}
			}
			var messageBytes = converters.hexStringToByteArray(message);
			return NRS.signBytesWithPrivateKey(messageBytes, converters.hexStringToByteArray(privateKey));
		};

		NRS.verifySignature = function(signature, message, publicKey, callback) {
			var signatureBytes = converters.hexStringToByteArray(signature);
			var messageBytes = converters.hexStringToByteArray(message);
			var publicKeyBytes = converters.hexStringToByteArray(publicKey);
			var v = signatureBytes.slice(0, 32);
			var h = signatureBytes.slice(32);
			var y = curve25519.verify(v, h, publicKeyBytes);
			var m = simpleHash(messageBytes);
			var h2 = simpleHash(m, y);
			if (!areByteArraysEqual(h, h2)) {
				if (callback) {
					callback({
						"errorCode": 1,
						"errorDescription": $.t("error_signature_verification_client")
					}, message);
				}
				return false;
			}
			return true;
		};

		NRS.setEncryptionPrivateKey = function(privateKey) {
			encryptionPrivateKey = privateKey;
		};

		NRS.setDecryptionPrivateKey = function(privateKey) {
			decryptionPrivateKey = privateKey;
		};

		NRS.tryToDecryptMessage = async function(message) {
			if (_decryptedTransactions && _decryptedTransactions[message.fullHash]) {
				if (_decryptedTransactions[message.fullHash].encryptedMessage) {
					return _decryptedTransactions[message.fullHash].encryptedMessage; // cache is saved differently by the info modal vs the messages table
				}
			}
			try {
				if (!message.attachment.encryptedMessage.data) {
					return { message: $.t("message_empty") };
				} else {
					var decoded = await NRS.decryptNote(message.attachment.encryptedMessage.data, {
						"nonce": message.attachment.encryptedMessage.nonce,
						"account": (message.recipient == NRS.account ? message.sender : message.recipient),
						"isText": message.attachment.encryptedMessage.isText,
						"isCompressed": message.attachment.encryptedMessage.isCompressed
					});
				}
				return decoded;
			} catch (err) {
				throw err;
			}
		};

		NRS.tryToDecrypt = async function(transaction, fields, account, options) {
			var showDecryptionForm = false;
			if (!options) {
				options = {};
			}
			var nrFields = Object.keys(fields).length;
			var formEl = (options.formEl ? NRS.escapeRespStr(options.formEl) : "#transaction_info_output_bottom");
			var outputEl = (options.outputEl ? NRS.escapeRespStr(options.outputEl) : "#transaction_info_output_bottom");
			var output = "";
			var identifier = (options.identifier ? transaction[options.identifier] : transaction.fullHash);

			//check in cache first..
			if (_decryptedTransactions && _decryptedTransactions[identifier]) {
				var decryptedTransaction = _decryptedTransactions[identifier];
				$.each(fields, function(key, title) {
					if (typeof title != "string") {
						title = title.title;
					}
					if (key in decryptedTransaction) {
						output += formatMessageArea(title, nrFields, decryptedTransaction[key], options, transaction);
					} else {
						//if a specific key was not found, the cache is outdated..
						output = "";
						delete _decryptedTransactions[identifier];
						return false;
					}
				});
			}

			if (!output) {
				for (let key in fields) {
					if (!fields.hasOwnProperty(key)) {
						continue;
					}
					let title = fields[key];
					var data = {};
					var encrypted = "";
					var nonce = "";
					var nonceField = (typeof title != "string" ? title.nonce : key + "Nonce");

					if (key == "encryptedMessage" || key == "encryptToSelfMessage") {
						encrypted = transaction.attachment[key].data;
						nonce = transaction.attachment[key].nonce;
					} else if (transaction.attachment && transaction.attachment[key]) {
						encrypted = transaction.attachment[key];
						nonce = transaction.attachment[nonceField];
					} else if (transaction[key] && typeof transaction[key] == "object") {
						encrypted = transaction[key].data;
						nonce = transaction[key].nonce;
					} else if (transaction[key]) {
						encrypted = transaction[key];
						nonce = transaction[nonceField];
					} else {
						encrypted = "";
					}

					if (encrypted) {
						if (typeof title != "string") {
							title = title.title;
						}
						try {
							var decryptOptions = {};
							if (options.sharedKey) {
								decryptOptions = { "sharedKey": converters.hexStringToByteArray(options.sharedKey) }
							} else {
								decryptOptions.nonce = nonce;
								decryptOptions.account = (key == "encryptToSelfMessage") ? NRS.account : account;
							}
							if (transaction.goodsIsText) {
								decryptOptions.isText = transaction.goodsIsText;
							} else if (transaction.attachment && transaction.attachment.goodsIsText) {
								decryptOptions.isText = transaction.attachment.goodsIsText;
							} else {
								decryptOptions.isText = transaction.attachment[key].isText;
								decryptOptions.isCompressed = transaction.attachment[key].isCompressed;
							}
							data = await NRS.decryptNote(encrypted, decryptOptions);
						} catch (err) {
							if (err.errorCode && err.errorCode == 1) {
								showDecryptionForm = true;
								break;
							} else {
								if (title) {
									var translatedTitle = NRS.getTranslatedFieldName(title).toLowerCase();
									if (!translatedTitle) {
										translatedTitle = NRS.escapeRespStr(title).toLowerCase();
									}

									data.message = $.t("error_could_not_decrypt_var", {
										"var": translatedTitle
									}).capitalize();
								} else {
									data.message = $.t("error_could_not_decrypt");
								}
							}
						}
						output += formatMessageArea(title, nrFields, data, options, transaction);
					}
				}
			}

			if (showDecryptionForm) {
				_encryptedNote = {
					"transaction": transaction,
					"fields": fields,
					"account": account,
					"options": options,
					"identifier": identifier
				};
				if (NRS.isHdWalletPrivateKeyAvailable()) {
					$("#decrypt_note_secret_phrase_div").hide();
					$("#decrypt_note_shared_key_div").hide();
					$("#decrypt_note_form_container").find(".callout").show();
				} else {
					if (_encryptedNote.account) {
						$("#decrypt_note_secret_phrase_div").show();
						$("#decrypt_note_form_container").find(".callout").hide();
					} else {
						$("#decrypt_note_secret_phrase_div").hide();
						$("#decrypt_note_form_container").find(".callout").show();
						$("#decrypt_note_form_password").val("");
					}
				}
				$("#decrypt_note_form_container").detach().appendTo(formEl);
				$("#decrypt_note_form_container, " + formEl).show();
			} else {
				NRS.removeDecryptionForm();
				$(outputEl).append(output).show();
			}
		};

		NRS.removeDecryptionForm = function($modal) {
			var noteFormContainer = $("#decrypt_note_form_container");
			if (($modal && $modal.find("#decrypt_note_form_container").length) || (!$modal && noteFormContainer.length)) {
				noteFormContainer.find("input").val("");
				noteFormContainer.hide().detach().appendTo("body");
			}
		};

		var formatMessageArea = function (title, nrFields, data, options, transaction) {
			var outputStyle = (!options.noPadding && title ? "padding-left:5px;" : "");
			var labelStyle = (nrFields > 1 ? " style='margin-top:5px'" : "");
			var label = (title ? "<label" + labelStyle + "><i class='far fa-unlock'></i> " + String(title).escapeHTML() + "</label>" : "");
			var msg;
			if (NRS.isTextMessage(transaction)) {
				msg = String(data.message).autoLink().nl2br();
			} else {
				msg = $.t("binary_data");
			}
			var sharedKeyField = "";
			var downloadLink = "";
			if (data.sharedKey) {
				sharedKeyField = "<div><label>" + $.t('shared_key') + "</label><br><span>" + data.sharedKey + "</span></div><br>";
				if (!NRS.isTextMessage(transaction) && transaction.block) {
					downloadLink = NRS.getMessageDownloadLink(transaction.fullHash, data.sharedKey) + "<br>";
				}
			}
			return "<div style='" + outputStyle + "'>" + label + "<div>" + msg + "</div>" + sharedKeyField + downloadLink + "</div>";
		};

		NRS.decryptNoteFormSubmit = async function() {
			var $form = $("#decrypt_note_form_container");
			if (!_encryptedNote) {
				$form.find(".callout").html($.t("error_encrypted_note_not_found")).show();
				return;
			}

			let secret = $form.find("input[name=secretPhrase]").val();
			let privateKey;
			let accountId;
			if (secret !== "") {
				privateKey = NRS.getPrivateKey(secret);
				accountId = NRS.getAccountId(privateKey);
			}
			let sharedKey = $form.find("input[name=sharedKey]").val();
			let useSharedKey = false;
			if (!NRS.isHardwareDecryptionEnabled()) {
				if (!privateKey) {
					if (NRS.rememberPassword) {
						privateKey = encryptionPrivateKey;
					} else if (decryptionPrivateKey) {
						privateKey = decryptionPrivateKey;
					} else if (!sharedKey) {
						$form.find(".callout").html($.t("error_passphrase_or_shared_key_required")).show();
						return;
					}
					useSharedKey = true;
				}

				if (accountId != NRS.account && !useSharedKey) {
					$form.find(".callout").html($.t("error_incorrect_passphrase")).show();
					return;
				}
			}

			let rememberPassword = $form.find("input[name=rememberPassword]").is(":checked");
			let output = "";
			let decryptionError = false;
			let decryptedFields = {};
			let nrFields = Object.keys(_encryptedNote.fields).length;

			for (let key in _encryptedNote.fields) {
				if (!_encryptedNote.fields.hasOwnProperty(key)) {
					continue;
				}
				let title = _encryptedNote.fields[key];
				let data = {};
				let encrypted = "";
				let nonce = "";
				let nonceField = (typeof title != "string" ? title.nonce : key + "Nonce");
				let otherAccount = _encryptedNote.account;
				if (key == "encryptedMessage" || key == "encryptToSelfMessage") {
					if (key == "encryptToSelfMessage") {
						otherAccount = accountId;
					}
					encrypted = _encryptedNote.transaction.attachment[key].data;
					nonce = _encryptedNote.transaction.attachment[key].nonce;
				} else if (_encryptedNote.transaction.attachment && _encryptedNote.transaction.attachment[key]) {
					encrypted = _encryptedNote.transaction.attachment[key];
					nonce = _encryptedNote.transaction.attachment[nonceField];
				} else if (_encryptedNote.transaction[key] && typeof _encryptedNote.transaction[key] == "object") {
					encrypted = _encryptedNote.transaction[key].data;
					nonce = _encryptedNote.transaction[key].nonce;
				} else if (_encryptedNote.transaction[key]) {
					encrypted = _encryptedNote.transaction[key];
					nonce = _encryptedNote.transaction[nonceField];
				} else {
					encrypted = "";
				}

				if (encrypted) {
					if (typeof title != "string") {
						title = title.title;
					}
					try {
						let options = {};
						options.privateKey = privateKey;
						if (useSharedKey) {
							options.sharedKey = converters.hexStringToByteArray(sharedKey);
						} else {
							options.nonce = nonce;
							options.account = otherAccount;
						}
						if (_encryptedNote.transaction.goodsIsText) {
							options.isText = _encryptedNote.transaction.goodsIsText;
						} else if (_encryptedNote.transaction.attachment && _encryptedNote.transaction.attachment.goodsIsText) {
							options.isText = _encryptedNote.transaction.attachment.goodsIsText;
						} else {
							options.isText = _encryptedNote.transaction.attachment[key].isText;
							options.isCompressed = _encryptedNote.transaction.attachment[key].isCompressed;
						}
						data = await NRS.decryptNote(encrypted, options);
						decryptedFields[key] = data;
					} catch (err) {
						if (useSharedKey) {
							data = { message: $.t("error_could_not_decrypt_message") };
							decryptedFields[key] = data;
						} else {
							decryptionError = true;
							let message = String(err.message ? err.message : err);
							$form.find(".callout").html(message.escapeHTML());
							NRS.logConsole(message);
							break;
						}
					}
					output += formatMessageArea(title, nrFields, data, _encryptedNote.options, _encryptedNote.transaction);
				}
			}
			if (decryptionError) {
				return;
			}
			_decryptedTransactions[_encryptedNote.identifier] = decryptedFields;

			//only save 150 decrypted messages in cache...
			let decryptionKeys = Object.keys(_decryptedTransactions);
			if (decryptionKeys.length > 150) {
				delete _decryptedTransactions[decryptionKeys[0]];
			}
			NRS.removeDecryptionForm();
			let outputEl = (_encryptedNote.options.outputEl ? NRS.escapeRespStr(_encryptedNote.options.outputEl) : "#transaction_info_output_bottom");
			$(outputEl).append(output).show();
			_encryptedNote = null;
			if (rememberPassword) {
				decryptionPrivateKey = privateKey;
			}
		};

		NRS.decryptAllMessages = async function(messages, privateKey, sharedKey) {
			var useSharedKey = false;
			if (!privateKey) {
				if (!sharedKey) {
					throw {
						"message": $.t("error_passphrase_required"),
						"errorCode": 1
					};
				}
				useSharedKey = true;
			} else {
				var accountId = NRS.getAccountId(privateKey);
				if (accountId != NRS.account) {
					throw {
						"message": $.t("error_incorrect_passphrase"),
						"errorCode": 2
					};
				}
			}

			var success = 0;
			var error = 0;
			for (var i = 0; i < messages.length; i++) {
				var message = messages[i];
				if (message.attachment.encryptedMessage && !_decryptedTransactions[message.fullHash]) {
					try {
						var otherUser = (message.sender == NRS.account ? message.recipient : message.sender);
						var options = {};
						options.privateKey = privateKey;
						if (useSharedKey) {
							options.sharedKey = converters.hexStringToByteArray(sharedKey);
						} else {
							options.nonce = message.attachment.encryptedMessage.nonce;
							options.account = otherUser;
						}
						if (_encryptedNote && _encryptedNote.transaction &&
							(_encryptedNote.transaction.goodsIsText || _encryptedNote.transaction.attachment.goodsIsText)) {
							options.isText = message.goodsIsText;
						} else {
							options.isText = message.attachment.encryptedMessage.isText;
							options.isCompressed = message.attachment.encryptedMessage.isCompressed;
						}
						var decoded = await NRS.decryptNote(message.attachment.encryptedMessage.data, options);
						_decryptedTransactions[message.fullHash] = {
							encryptedMessage: decoded
						};
						success++;
					} catch (err) {
						if (!useSharedKey) {
							_decryptedTransactions[message.fullHash] = {
								"message": $.t("error_decryption_unknown")
							};
						}
						error++;
					}
				}
			}

			//noinspection RedundantIfStatementJS
			if (success || !error) {
				return true;
			} else {
				return false;
			}
		};

		function simpleHash(b1, b2) {
			var sha256 = CryptoJS.algo.SHA256.create();
			sha256.update(converters.byteArrayToWordArray(b1));
			if (b2) {
				sha256.update(converters.byteArrayToWordArray(b2));
			}
			var hash = sha256.finalize();
			return converters.wordArrayToByteArrayImpl(hash, false);
		}

		function areByteArraysEqual(bytes1, bytes2) {
			if (bytes1.length !== bytes2.length) {
				return false;
			}
			for (var i = 0; i < bytes1.length; ++i) {
				if (bytes1[i] !== bytes2[i]) {
					return false;
				}
			}
			return true;
		}

		function byteArrayToBigInteger(byteArray) {
			var value = new BigInteger("0", 10);
			var temp1, temp2;
			for (var i = byteArray.length - 1; i >= 0; i--) {
				temp1 = value.multiply(new BigInteger("256", 10));
				temp2 = temp1.add(new BigInteger(byteArray[i].toString(10), 10));
				value = temp2;
			}
			return value;
		}

		function aesEncrypt(payload, options) {
			var ivBytes = getRandomBytes(16);

			// CryptoJS likes WordArray parameters
			var wordArrayPayload = converters.byteArrayToWordArray(payload);
			var sharedKey;
			if (!options.sharedKey) {
				sharedKey = getSharedSecret(options.privateKey, options.publicKey);
			} else {
				sharedKey = options.sharedKey.slice(0); //clone
			}
			if (options.nonce !== undefined) {
				for (var i = 0; i < 32; i++) {
					sharedKey[i] ^= options.nonce[i];
				}
			}
			var key = CryptoJS.SHA256(converters.byteArrayToWordArray(sharedKey));
			var encrypted = CryptoJS.AES.encrypt(wordArrayPayload, key, {
				iv: converters.byteArrayToWordArray(ivBytes)
			});
			var ivOut = converters.wordArrayToByteArray(encrypted.iv);
			var ciphertextOut = converters.wordArrayToByteArray(encrypted.ciphertext);
			return ivOut.concat(ciphertextOut);
		}

		NRS.aesEncrypt = function(plaintext, options) {
			return aesEncrypt(converters.stringToByteArray(plaintext), options);
		};

		NRS.aesDecrypt = function(cipherBytes, options) {
			return aesDecrypt(cipherBytes, options);
		};

		function aesDecrypt(ivCiphertext, options) {
			if (ivCiphertext.length < 16 || ivCiphertext.length % 16 != 0) {
				throw {
					name: "invalid ciphertext"
				};
			}

			var iv = converters.byteArrayToWordArray(ivCiphertext.slice(0, 16));
			var ciphertext = converters.byteArrayToWordArray(ivCiphertext.slice(16));

			// shared key is use for two different purposes here
			// (1) if nonce exists, shared key represents the shared secret between the private and public keys
			// (2) if nonce does not exists, shared key is the specific key needed for decryption already xored
			// with the nonce and hashed
			var sharedKey;
			if (!options.sharedKey) {
				sharedKey = getSharedSecret(options.privateKey, options.publicKey);
			} else {
				sharedKey = options.sharedKey.slice(0); //clone
			}

			var key;
			if (options.nonce) {
				for (var i = 0; i < 32; i++) {
					sharedKey[i] ^= options.nonce[i];
				}
				key = CryptoJS.SHA256(converters.byteArrayToWordArray(sharedKey));
			} else {
				key = converters.byteArrayToWordArray(sharedKey);
			}

			var encrypted = CryptoJS.lib.CipherParams.create({
				ciphertext: ciphertext,
				iv: iv,
				key: key
			});

			var decrypted = CryptoJS.AES.decrypt(encrypted, key, {
				iv: iv
			});

			return {
				decrypted: converters.wordArrayToByteArray(decrypted),
				sharedKey: converters.wordArrayToByteArray(key)
			};
		}

		NRS.encryptDataRoof = function(data, options) {
			return encryptData(data, options);
		};

		function encryptData(plaintext, options) {
			options.nonce = getRandomBytes(32);
			if (!options.sharedKey) {
				options.sharedKey = getSharedSecret(options.privateKey, options.publicKey);
			}
			var compressedPlaintext = pako.gzip(new Uint8Array(plaintext));
			var data = aesEncrypt(compressedPlaintext, options);
			return {
				"nonce": options.nonce,
				"data": data
			};
		}

		NRS.decryptDataRoof = function(data, options) {
			return decryptData(data, options);
		};

		function decryptData(data, options) {
			if (!options.sharedKey) {
				options.sharedKey = getSharedSecret(options.privateKey, options.publicKey);
			}

			var result = aesDecrypt(data, options);
			var binData = new Uint8Array(result.decrypted);
			if (!(options.isCompressed === false)) {
				binData = pako.inflate(binData);
			}
			var message;
			if (!(options.isText === false)) {
				message = converters.byteArrayToString(binData);
			} else {
				message = converters.byteArrayToHexString(binData);
			}
			return { message: message, sharedKey: converters.byteArrayToHexString(result.sharedKey) };
		}

		function getSharedSecret(key1, key2) {
			return converters.shortArrayToByteArray(curve25519_(converters.byteArrayToShortArray(key1), converters.byteArrayToShortArray(key2), null));
		}

		NRS.sharedSecretToSharedKey = function (sharedSecret, nonce) {
			for (var i = 0; i < 32; i++) {
				sharedSecret[i] ^= nonce[i];
			}
			return simpleHash(sharedSecret);
		};

		NRS.getSharedKey = function (privateKey, publicKey, nonce) {
			var sharedSecret = getSharedSecret(privateKey, publicKey);
			return NRS.sharedSecretToSharedKey(sharedSecret, nonce);
		};

		function readFileAsync(file) {
			return new Promise((resolve, reject) => {
				let reader = new FileReader();
				reader.onload = () => {
					resolve(reader.result);
				};
				reader.onerror = reject;
				reader.readAsArrayBuffer(file);
			})
		}

		NRS.encryptFile = async function(file, options) {
			let bytes = await readFileAsync(file);
			options.isText = false;
			if (NRS.isHardwareEncryptionEnabled()) {
				let encryptedResult = await NRS.encryptUsingHardwareWallet(converters.byteArrayToHexString(options.publicKey), new Uint8Array(bytes));
				return { data: converters.hexStringToByteArray(encryptedResult.message), nonce: converters.hexStringToByteArray(encryptedResult.nonce) };
			} else {
				return encryptData(bytes, options);
			}
		};

		NRS.getRandomBytes = function(length) {
			return getRandomBytes(length);
		}

		function getRandomBytes(length) {
			if (isNode) {
				return crypto.randomBytes(length);
			}
			if (!window.crypto && !window.msCrypto && !crypto) {
				throw {
					"errorCode": -1,
					"message": $.t("error_encryption_browser_support")
				};
			}
			var bytes = new Uint8Array(length);
			if (window.crypto) {
				//noinspection JSUnresolvedFunction
				window.crypto.getRandomValues(bytes);
			} else if (window.msCrypto) {
				//noinspection JSUnresolvedFunction
				window.msCrypto.getRandomValues(bytes);
			} else {
				bytes = crypto.randomBytes(length);
			}
			return bytes;
		}

		/**
		 * This function is invoked from the Node JS module
		 * TODO add support for passing a private key
		 */
		NRS.encryptMessage = async function(NRS, text, senderSecretPhrase, recipientPublicKey, isMessageToSelf) {
			var encrypted = await NRS.encryptNote(text, {
				"publicKey": recipientPublicKey,
				"privateKey": NRS.getPrivateKey(senderSecretPhrase)
			});
			if (isMessageToSelf) {
				return {
					encryptToSelfMessageData: encrypted.message,
					encryptToSelfMessageNonce: encrypted.nonce,
					messageToEncryptToSelfIsText: "true"
				}
			} else {
				return {
					encryptedMessageData: encrypted.message,
					encryptedMessageNonce: encrypted.nonce,
					messageToEncryptIsText: "true"
				}
			}
		};

		return NRS;
	}(isNode ? client : NRS || {}, jQuery));

	if (isNode) {
		module.exports = NRS;
	}
});