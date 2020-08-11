/*
 * Copyright © 2013-2016 The Nxt Core Developers.
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

/**
 * @depends {nrs.js}
 */
NRS.onSiteBuildDone().then(() => {
    NRS = (function(NRS, $) {

        const $groupSelect = $('#configuration_page_group_select');
        const $filterInput = $(".node_configuration_search [name=fs_q]");
        const $configurationPage = $('#configuration_page');

        // noinspection JSMismatchedCollectionQueryUpdate
        let configProperties = [];
        let viewProperties = [];
        let showAll = false;

        NRS.jsondata = NRS.jsondata||{};

        NRS.jsondata.configProperty = function (p) {
            p.currentValue = p.configuredValue !== null ? p.configuredValue : (p.installerValue != null ? p.installerValue : p.defaultValue);
            p.newValue = null;
            p.isWithNewValue = false;
            p.trClass = p.configuredValue !== null ? 'config-changed' : 'config-default';
            p.isShowValues = p.type !== 'PASSWORD';
            p.isShowChangeButton = p.type !== 'READONLY';
            p.formattedGroup = NRS.initialCaps(p.group.toLowerCase());

            if (p.currentValue === null) {
                p.formattedValue = '<em>None</em>';
            } else if (p.type === 'PASSWORD') {
                p.formattedValue = '<em>N/A</em>';
            } else if (p.isList && p.currentValue.length > 50) {
                p.formattedValue = `<em>List of ${NRS.unescapeRespStr(p.currentValue).split(';').length}</em>`;
            } else {
                p.formattedValue = NRS.addEllipsis(p.currentValue, 40);
            }

            p.formattedDefaultValue = formatViewValue(p, p.defaultValue);
            p.formattedInstallerValue = formatViewValue(p, p.installerValue);
            p.formattedConfiguredValue = formatViewValue(p, p.configuredValue);

            if (p.isList === true) {
                p.formattedType = 'List of ' + p.type.toLowerCase();
            } else {
                p.formattedType = NRS.initialCaps(p.type.toLowerCase());
                if (p.min !== undefined && p.min !== null) {
                    p.formattedType += ` (min ${p.min})`;
                }
                if (p.max !== undefined && p.max !== null) {
                    p.formattedType += ` (max ${p.max})`;
                }
            }

            return p;
        };

        function formatViewValue(configProperty, value) {
            return value !== null && configProperty.isList === true ? NRS.unescapeRespStr(value).split(';').map(s => s.escapeHTML()).join('<br>') : value;
        }

        NRS.pages.configuration = function () {
            NRS.preparePage();
            if (configProperties.length === 0) {
                NRS.hasMorePages = false;
                var view = NRS.simpleview.get('configuration_page_content', {
                    errorMessage: null,
                    isLoading: true,
                    isEmpty: false,
                    properties: []
                });
                NRS.sendRequest("getConfiguration", {adminPassword: NRS.getAdminPassword()},
                    function(response) {
                        if (NRS.isErrorResponse(response)) {
                            view.render({
                                errorMessage: NRS.getErrorMessage(response),
                                isLoading: false,
                                isEmpty: false
                            });
                            return;
                        }
                        configProperties = response.properties.map(p => NRS.jsondata.configProperty(p));
                        renderGroupSelect();
                        renderProperties();
                        NRS.pageLoaded();
                    }
                );
            } else {
                renderProperties();
                NRS.pageLoaded();
            }
        };

        function renderGroupSelect() {
            const $select = $groupSelect;
            $select.find('option:gt(0)').remove();
            const groups = {};
            configProperties.forEach(p => groups[p.formattedGroup] = true);
            Object.getOwnPropertyNames(groups).forEach(group => {
                $select.append($('<option></option>').text(group));
            });
        }

        function testField(field, testValue) {
            if (field === null) {
                return false;
            }
            return field.toLowerCase().indexOf(testValue) > -1;
        }

        function filterProperties(property) {
            const $groupFilter = $groupSelect.val();
            const $textFilterValue = $.trim($filterInput.val().toLowerCase());
            if (!showAll && property.configuredValue === null && !property.isWithNewValue) {
                return false;
            }
            return ($groupFilter === 'All' || property.formattedGroup === $groupFilter) && (
                testField(property.name, $textFilterValue) ||
                testField(property.description, $textFilterValue) ||
                testField(property.configuredValue, $textFilterValue) ||
                testField(property.installerValue, $textFilterValue) ||
                testField(property.defaultValue, $textFilterValue)
            );
        }

        function renderProperties() {
            // check if the current or future configuration opens the API port and no admin password is set
            if (mustSetAdminPassword()) {
                viewProperties = configProperties.filter(p => filterProperties(p) || p.name === 'nxt.adminPassword');
                $('#configuration_restart_required').addClass('hidden');
                $('div[data-i18n="configuration_must_set_admin_password"]').removeClass('hidden');
            } else {
                viewProperties = configProperties.filter(filterProperties);
                $('div[data-i18n="configuration_must_set_admin_password"]').addClass('hidden');
                $('#configuration_restart_required').toggleClass('hidden', configProperties.findIndex(p => p.isWithNewValue) === -1);
            }

            NRS.simpleview.get('configuration_page_content', {
                errorMessage: null,
                isLoading: false,
                isEmpty: viewProperties.length == 0,
                properties: viewProperties
            });
        }

        function mustSetAdminPassword() {
            let adminPasswordProperty = null;
            let apiServerHostProperty = null;
            for (let i = 0; i < configProperties.length && (adminPasswordProperty === null || apiServerHostProperty === null); i++) {
                if (configProperties[i].name === 'nxt.adminPassword') {
                    adminPasswordProperty = configProperties[i];
                } else if (configProperties[i].name === 'nxt.apiServerHost') {
                    apiServerHostProperty = configProperties[i];
                }
            }
            if (adminPasswordProperty === null || apiServerHostProperty === null) {
                return false;
            }
            const apiServerHost = apiServerHostProperty.newValue !== null ? apiServerHostProperty.newValue : apiServerHostProperty.currentValue;
            const adminPasswordSet = (adminPasswordProperty.newValue !== null && adminPasswordProperty.newValue !== '') ||
                (adminPasswordProperty.newValue === null && adminPasswordProperty.currentValue === '');
            return apiServerHost !== '127.0.0.1' && apiServerHost !== '' && !adminPasswordSet;
        }

        $configurationPage.on('click', ' .content-header .btn[data-show-all]', function (e) {
            e.preventDefault();
            showAll = $(this).data('showAll');
            renderProperties();
        });

        $configurationPage.on('click', '.node_configuration_search .btn', function(e) {
            e.preventDefault();
            renderProperties();
        });

        $groupSelect.on('change', () => renderProperties());

        $filterInput.on('keyup', () => renderProperties());

        // view modal

        $('#m_view_configuration_property').on('show.bs.modal', function(event) {
            const $invokerButton = $(event.relatedTarget);
            const configProperty = viewProperties[$invokerButton.data('property')];
            NRS.simpleview.get('m_view_configuration_property_template', configProperty);
        });

        // change modal

        $('#m_update_configuration_property').on('show.bs.modal', function(event) {
            const $invokerButton = $(event.relatedTarget);
            const configProperty = viewProperties[$invokerButton.data('property')];
            const $modal = $(this);
            const isPassword = configProperty.type === 'PASSWORD';
            const isList = configProperty.isList === true;
            const isBoolean = configProperty.type === 'BOOLEAN';

            // property name
            $modal.data('property', configProperty);
            $('#configuration_property_name').val(configProperty.name);

            // current value
            $('#configuration_property_current_value')
                .val(isPassword ? '' : NRS.unescapeRespStr(configProperty.currentValue))
                .parent().toggleClass('hidden', isList || isPassword);
            $('#configuration_property_current_value_list')
                .val(NRS.unescapeRespStr(configProperty.currentValue).split(';').join('\n'))
                .parent().toggleClass('hidden', !isList || isPassword);

            // new value
            let value = configProperty.newValue;
            if (value === null) {
                value = configProperty.configuredValue === null ? '' : NRS.unescapeRespStr(configProperty.configuredValue);
            }
            const $changeModalValue = $('#configuration_property_value');
            $changeModalValue
                .prop('disabled', false)
                .val(value === null || isPassword ? '' : value)
                .parent().toggleClass('hidden', isList || isPassword || isBoolean);
            $('#configuration_property_list')
                .prop('disabled', false)
                .val($changeModalValue.val().split(';').map(s => s.trim()).join('\n'))
                .parent().toggleClass('hidden', !isList);
            $('#configuration_property_passphrase')
                .prop('disabled', false)
                .val('')
                .parent().toggleClass('hidden', !isPassword);
            const booleanShowTrue = value === 'true' || (value === '' && configProperty.defaultValue === 'true');
            $('#configuration_property_boolean')
                .prop({disabled: false, checked: booleanShowTrue})
                .siblings('span').text(booleanShowTrue ? 'true' : 'false')
                .parents('.form-group').toggleClass('hidden', !isBoolean);

            $modal.find('.type-information').text(configProperty.formattedType);

            $('#configuration_restore_property_checkbox').prop('disabled', value === '');
            $('#configuration_discard_property_checkbox').prop('disabled', !configProperty.isWithNewValue);
        });

        $('#configuration_property_boolean').on('change', function () {
            const $this = $(this);
            $this.siblings('span').text($this.is(':checked') ? 'true' : 'false');
        });

        $('#configuration_restore_property_checkbox,#configuration_discard_property_checkbox').on('change', function() {
            const $this = $(this);
            const isChecked = $this.is(':checked');
            // noinspection JSCheckFunctionSignatures
            const inputs = $this.parents('form').find('input,textarea');
            inputs.filter('.value').prop('disabled', isChecked);
            const otherCheckbox = inputs.filter('input[type=checkbox]').not($this).not('.value').first();
            if (isChecked) {
                // disable otherCheckbox
                otherCheckbox.data('previousDisabled', otherCheckbox.prop('disabled'));
                otherCheckbox.prop({disabled: true, checked: false});
            } else {
                // re-enable (if applicable) otherCheckbox
                otherCheckbox.prop('disabled', otherCheckbox.data('previousDisabled'));
            }
        });

        $('#configuration_property_submit').on('click', function () {
            const $modal = $('#m_update_configuration_property');
            const data = NRS.getFormData($modal.find("form:first"));
            const configProperty = $modal.data('property');

            let newValue;
            if (data.discard === 'true') {
                newValue = null;
            } else if (data.restore === 'true') {
                newValue = '';
            } else if (configProperty.isList === true) {
                newValue = data.list.split(/\r?\n/).join('; ');
            } else if (configProperty.type === 'PASSWORD') {
                newValue = data.passphrase;
            } else if (configProperty.type === 'BOOLEAN') {
                newValue = data.boolean_value === undefined ? 'false' : 'true';
            } else {
                newValue = data.value;
            }

            const validation = validateNewValue(configProperty, newValue);
            if (validation !== '') {
                $modal.find(".error_message").html(validation).show();
            } else {
                configProperty.newValue = configProperty.type !== 'PASSWORD' && newValue === configProperty.currentValue ? null : newValue;
                configProperty.formattedNewValue = formatViewValue(configProperty, String(configProperty.newValue).escapeHTML());
                configProperty.isWithNewValue = configProperty.newValue !== null;
                $modal.modal('hide');
                $.growl($.t("change_recorded"));
                NRS.loadPage("configuration");
            }
        });

        // validates the new property value
        // returns a non-empty string for invalid values, the content of the string is the message to show the user
        function validateNewValue(configProperty, value) {
            if (value === null || value === '' || configProperty.type === 'STRING' || configProperty.type === 'PASSWORD') {
                return '';
            }
            if (configProperty.type === 'BOOLEAN') {
                if (value !== 'true' && value !== 'false') {
                    return $.t('must_be_true_or_false');
                }
                return '';
            }
            if (configProperty.type === 'INTEGER') {
                if (!/^-?[0-9]+$/.test(value)) {
                    return $.t('error_not_a_number', {field: $.t('value')});
                }
                const iValue = parseInt(value);
                if (configProperty.min !== null && iValue < configProperty.min) {
                    return $.t('error_min_value', {field: $.t('value'), min: configProperty.min});
                }
                if (configProperty.max !== null && iValue > configProperty.max) {
                    return $.t('error_max_value', {field: $.t('value'), max: configProperty.max});
                }
                return '';
            }
            if (configProperty.type === 'ACCOUNT') {
                if (configProperty.isList === true) {
                    const values = value.split(';');
                    for(let i = 0; i < values.length; i++) {
                        const s = values[i].trim();
                        if (!NRS.isNumericAccount(s) && !NRS.isRsAccount(s)) {
                            return $.t('error_invalid_account_id') + ' #' + (i+1);
                        }
                    }
                } else if (!NRS.isNumericAccount(value) && !NRS.isRsAccount(value)) {
                    return $.t('error_invalid_account_id');
                }
                return '';
            }
        }

        $('#configuration_restart_required button').on('click', function () {
            let texts = ['<p>' + $.t('save_and_shutdown_confirmation') + '</p>'];
            let properties = configProperties.filter(p => p.isWithNewValue);
            let updatedProperties = properties.filter(p => p.newValue !== '');
            let removedProperties = properties.filter(p => p.newValue === '');
            if (updatedProperties.length > 0) {
                texts.push('<p class="lead">' + $.t('updated_properties') + '</p>');
                texts.push('<dl>');
                updatedProperties.forEach(p => {
                    texts.push(`<dt>${String(p.name).escapeHTML()}</dt>`);
                    let value = p.isShowValues ? p.newValue : '********';
                    texts.push(`<dl>${String(value).escapeHTML()}</dl>`);
                });
                texts.push('</dl>');
            }
            if (removedProperties.length > 0) {
                texts.push('<p class="lead">' + $.t('removed_properties') + '</p>');
                texts.push('<ul>');
                removedProperties.forEach(p => {
                    texts.push(`<li>${String(p.name).escapeHTML()}</li>`);
                });
                texts.push('</ul>');
            }
            NRS.showConfirmModal($.t('warning'), texts.join('\n'), $.t('save_and_shutdown'),
                () => {
                    $.growl($.t('requesting_shutdown'));
                    NRS.sendRequest("setConfiguration", {
                        adminPassword: NRS.getAdminPassword(),
                        propertiesJSON: JSON.stringify(properties.map(p => ({property: p.name, value: p.newValue}))),
                        shutdown: true
                    }, () => $.growl($.t('shutting_down')));
                });
        });

        return NRS;
    }(NRS || {}, jQuery));
});