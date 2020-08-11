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

        const $deviceSettingsModal = $("#device_settings_modal");
        $deviceSettingsModal.on("show.bs.modal", function() {
            if (NRS.activeTour) {
                NRS.activeTour.end();
            }
            $("#device_admin_password").val(NRS.deviceSettings.admin_password);
            if (NRS.deviceSettings.is_check_remember_me) {
                $("#device_is_check_remember_me").prop('checked', true);
            } else {
                $("#device_is_check_remember_me").prop('checked', false);
            }
            $("#device_camera_id").val(NRS.deviceSettings.camera_id);
            $("#device_account_prefix")
                .val(NRS.deviceSettings.account_prefix)
                .on("keyup change", function(event) {
                    event.target.setCustomValidity('');
                    event.target.reportValidity ? event.target.reportValidity() : event.target.checkValidity(); //for standalone wallet which doesn't support reportValidity API
                });
        });

        $deviceSettingsModal.on("hide.bs.modal", function() {
            $("#device_account_prefix").off("keyup change");
        });

        $("#restart_ui_tour").click(function() {
            $deviceSettingsModal.modal("hide");
            NRS.tours.login(true);
            NRS.activeTour.goTo(0);
        })

        NRS.forms.setDeviceSettings = function() {
            NRS.deviceSettings.admin_password = $("#device_admin_password").val();
            NRS.deviceSettings.is_check_remember_me = $("#device_is_check_remember_me").prop('checked');
            let cameraId = $("#device_camera_id").val();
            if (!$.isNumeric(cameraId)) {
                return { error: $.t("camera_id") + " " + $.t("is_not_numeric") };
            }
            NRS.deviceSettings.camera_id = parseInt(cameraId);
            NRS.deviceSettings.account_prefix = $("#device_account_prefix").val();
            NRS.setJSONItem("device_settings", NRS.deviceSettings);
            return { reload: true, forceGet: false };
        };

        return NRS;

    }(NRS || {}, jQuery));
});