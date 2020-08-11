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
    NRS = (function (NRS, $, undefined) {

        const PERMISSIONS = {
            MASTER_ADMIN: "MASTER_ADMIN",
            CHAIN_ADMIN: "CHAIN_ADMIN",
            CHAIN_USER: "CHAIN_USER",
            BLOCKED_CHAIN_USER: "BLOCKED_CHAIN_USER",
            BLOCKED_CHAIN_ADMIN: "BLOCKED_CHAIN_ADMIN"
        }
        NRS.PERMISSIONS = PERMISSIONS;

        const PERMISSIONS_TO_TRANSLATION_KEYS = {};
        PERMISSIONS_TO_TRANSLATION_KEYS[PERMISSIONS.MASTER_ADMIN] = "permission_master_admin";
        PERMISSIONS_TO_TRANSLATION_KEYS[PERMISSIONS.CHAIN_ADMIN] = "permission_chain_admin";
        PERMISSIONS_TO_TRANSLATION_KEYS[PERMISSIONS.BLOCKED_CHAIN_ADMIN] = "permission_blocked_chain_admin";
        PERMISSIONS_TO_TRANSLATION_KEYS[PERMISSIONS.CHAIN_USER] = "permission_chain_user";
        PERMISSIONS_TO_TRANSLATION_KEYS[PERMISSIONS.BLOCKED_CHAIN_USER] = "permission_blocked_chain_user";

        NRS.PERMISSIONS_TO_TRANSLATION_KEYS = PERMISSIONS_TO_TRANSLATION_KEYS;

        var onlyGrantedByMe;
        var checkRecipientHandler;

        NRS.pages.permissions_control = function () {
            const view = NRS.simpleview.get('permissions_control_page', {
                isLoading: true
            });
            view.render();
            let requestData = {
                "chain": NRS.getActiveChainId(),
                "firstIndex": 0,
                "lastIndex": 100
            }
            if (onlyGrantedByMe) {
                requestData.granter = NRS.accountRS;
            }
            NRS.sendRequest("getChainPermissions", requestData, function (chainPermissions) {
                delete chainPermissions.requestProcessingTime
                NRS.getAccountPermissionsPromise().then(function(accountPermissions) {
                    renderPage(accountPermissions, chainPermissions);
                    registerViewEventHandlers();
                    NRS.pageLoaded();
                });
            });
        };

        NRS.setup.permissions_control = function () {
            var options = {
                "id": 'sidebar_permissions_control',
                "titleHTML": '<i class="fa fa-users"></i> <span data-i18n="permissions_control">Permissions Control</span>',
                "page": 'permissions_control',
                "desiredPosition": 120,
                "depends": { tags: [ NRS.constants.API_TAGS.CHILD_CHAIN_CONTROL ] }
            };
            NRS.addSimpleSidebarMenuItem(options);

            $(".modal[data-transaction-type]:not(#grant_permission_modal, #remove_permission_modal)").on("show.bs.modal", function(e) {
                const $modal = $(e.target);
                NRS.getAccountPermissionsPromise().then(function(accountPermissions) {
                    if (NRS.isActivePermissionPolicyChildChain()) {
                        const hasEffectivePermissions = accountPermissions.hasEffectivePermissions;
                        const hasUserPermission = hasEffectivePermissions.some(function(permissionObj) {
                            return permissionObj.permission === NRS.PERMISSIONS.CHAIN_USER;
                        })
                        if (!hasUserPermission) {
                            $modal.find(".error_message").html($.t("no_chain_user_permission")).show();
                        }
                    }
                });
            });
        };

        $("#grant_permission_modal").on("show.bs.modal", function (e) {
            const $invoker = $(e.relatedTarget);
            const $modal = $(e.target);
            const permissions = $invoker.data("permissions");
            var currentAccount = $invoker.data("account");
            checkRecipientHandler = function(event) {
                const account = $(event.target).val();
                if (account != currentAccount) {
                    filterOutExistingPermissionsByAccount(permissions, account, function(filteredPermissions) {
                        currentAccount = account;
                        renderPermissionsModal('grant_permission_permissions', filteredPermissions);
                    });
                }
            };
            if (currentAccount) {
                filterOutExistingPermissionsByAccount(permissions, currentAccount, function(filteredPermissions) {
                    renderPermissionsModal('grant_permission_permissions', filteredPermissions);
                });
            }
            $modal.find("#grant_permission_recipient").on("checkRecipient", checkRecipientHandler);
        });

        $("#remove_permission_modal").on("show.bs.modal", function (e) {
            const $invoker = $(e.relatedTarget);
            const $modal = $(e.target);
            const permissions = $invoker.data("permissions");
            const permission = $invoker.data("permission");
            var currentAccount = $invoker.data("account");
            checkRecipientHandler = function(event) {
                const account = $(event.target).val();
                if (account != currentAccount) {
                    filterOutMissingPermissionsByAccount(permissions, account, function(filteredPermissions) {
                        currentAccount = account;
                        renderPermissionsModal('remove_permission_permissions', filteredPermissions, permission);
                    });
                }
            };
            if (currentAccount) {
                filterOutMissingPermissionsByAccount(permissions, currentAccount, function(filteredPermissions) {
                    renderPermissionsModal('remove_permission_permissions', filteredPermissions, permission);
                });
            }
            $modal.find("#remove_permission_recipient").on("checkRecipient", checkRecipientHandler);
        });

        $("#remove_permission_modal").on("hide.bs.modal", function (e) {
            $(e.target).find("#remove_permission_recipient").off("checkRecipient", checkRecipientHandler);
        });

        $("#grant_permission_modal").on("hide.bs.modal", function (e) {
            $(e.target).find("#grant_permission_recipient").off("checkRecipient", checkRecipientHandler);
        });

        function renderPermissionsModal(simpleviewId, permissions, selectedPermission) {
            var view = NRS.simpleview.get(simpleviewId, {
                permissions: permissions
            });
            view.render();
            if (selectedPermission) {
                $("[name=permission]").val(selectedPermission);
            }
        }

        // filters out permissions this account already has;
        function filterOutExistingPermissionsByAccount(permissions, account, callback) {
            NRS.sendRequest("getAccountPermissions", {
                "account": account
            }, function (accountPermissions) {
                var filteredPermissions = permissions.filter(function(permission) {
                    return accountPermissions.hasPermissions && !accountPermissions.hasPermissions.some(function(accountPermission) {
                        return permission.value === accountPermission.permission;
                    });
                });
                callback(filteredPermissions);
            });
        }

        // filters out permissions this account doesn't have
        function filterOutMissingPermissionsByAccount(permissions, account, callback) {
            NRS.sendRequest("getAccountPermissions", {
                "account": account
            }, function (accountPermissions) {
                var filteredPermissions = permissions.filter(function(permission) {
                    return accountPermissions.hasPermissions && accountPermissions.hasPermissions.some(function(accountPermission) {
                        return permission.value === accountPermission.permission;
                    });
                });
                callback(filteredPermissions);
            });
        }

        function registerViewEventHandlers() {
            $("#permissions_granter_type .btn").on("click", function (e) {
                e.preventDefault();
                onlyGrantedByMe = $(this).data("type") === "you";
                NRS.pages.permissions_control();
            });
        }

        function renderPage(accountPermissions, chainPermissions) {
            const canGrantPermissions = accountPermissions.canGrantPermissions;
            const view = NRS.simpleview.get('permissions_control_page', {
                accounts: generateAccountsArray(chainPermissions, canGrantPermissions),
                onlyGrantedByMe: onlyGrantedByMe,
                permissionsStringified: JSON.stringify(canGrantPermissions.map(function(permissionName) {
                    return {
                        label: $.t(NRS.PERMISSIONS_TO_TRANSLATION_KEYS[permissionName]),
                        value: permissionName
                    }
                }))
            });
            view.render();
        }

        function generateAccountsArray(chainPermissions, canGrantPermissions) {
            return chainPermissions.permissions.map(function(accountPermissionObj)  {
                var account = Object.assign({}, accountPermissionObj, {
                    accountLink: NRS.getAccountLink({accountRS: accountPermissionObj.accountRS}, "account"),
                    granterLink: accountPermissionObj.granter !== "0" ? NRS.getAccountLink({accountRS: accountPermissionObj.granterRS}, "account") : $.t("genesis"),
                    heightLink: NRS.getBlockLink(accountPermissionObj.height > -1 ? accountPermissionObj.height : 0),
                    permissionLabel: $.t(NRS.PERMISSIONS_TO_TRANSLATION_KEYS[accountPermissionObj.permission]),
                    canGrant: canGrantPermissions.indexOf(accountPermissionObj.permission) > -1
                });
                account.permissionsStringified = JSON.stringify(account.permission);
                return account;
            });
        }

        return NRS;
    }(NRS || {}, jQuery));
});