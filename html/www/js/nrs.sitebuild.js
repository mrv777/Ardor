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
 * @depends {3rdparty/jquery-2.1.0.js}
 * @depends {3rdparty/i18next.js}
 */
var NRS = (function(NRS, $) {

    var _modalUIElements = null;
    const loadPromises = [];
    const promises = [];

    function fetchResource(url) {
        return fetch(url).then((response) => {
                if (!response.ok) {
                    throw new Error(`Network response was not OK: ${url}`);
                }
                return response.text();
            }).then(textHTML => $(textHTML));
    }

    NRS.loadLockscreenHTML = function(path) {
        if (!NRS.getUrlParameter("account") && !NRS.getUrlParameter("lifetime_modal")) {
            loadPromises.push(fetchResource(path).then(data => $("#testnet_warning").after(data)));
        }
    };

    NRS.loadHeaderHTML = function(path) {
    	loadPromises.push(fetchResource(path).then(data => $("#testnet_warning").after(data)));
    };

    NRS.loadSidebarHTML = function(path) {
    	loadPromises.push(fetchResource(path).then(data => $("#sidebar").append(data)));
    };

    NRS.loadSidebarContextHTML = function(path) {
    	loadPromises.push(fetchResource(path).then(data => $("body").append(data)));
    };

    NRS.loadPageHTML = function(path) {
        loadPromises.push(NRS.asyncLoadPageHTML(path));
    };

    NRS.asyncLoadPageHTML = function(path) {
    	return fetchResource(path).then(data => $("#content").append(data));
    };

    NRS.loadModalHTML = function(path) {
    	loadPromises.push(fetchResource(path).then(data => $("body").append(data)));
    };

    function _replaceModalHTMLTemplateDiv(data, templateName) {
        var html = $(data).filter('div#' + templateName).html();
        var template = Handlebars.compile(html);
        $('div[data-replace-with-modal-template="' + templateName + '"]').each(function() {
            var name = $(this).closest('.modal').attr('id').replace('_modal', '');
            var context = { name: name };
            $(this).replaceWith(template(context));
        });
    }

    NRS.loadModalHTMLTemplates = function() {
        let loadAllHTML = NRS.whenWithProgress(loadPromises, NRS.updateLoadingProgress);
        
        // this function requires the HTML to be already loaded so it acts as a barrier to join all promises
        function replaceModalTemplates(data) {
            _replaceModalHTMLTemplateDiv(data, 'recipient_modal_template');
            _replaceModalHTMLTemplateDiv(data, 'add_message_modal_template');
            _replaceModalHTMLTemplateDiv(data, 'fee_calculation_modal_template');
            _replaceModalHTMLTemplateDiv(data, 'secret_phrase_modal_template');
            _replaceModalHTMLTemplateDiv(data, 'admin_password_modal_template');
            _replaceModalHTMLTemplateDiv(data, 'advanced_deadline_template');
            _replaceModalHTMLTemplateDiv(data, 'advanced_approve_template');
            _replaceModalHTMLTemplateDiv(data, 'advanced_rt_hash_template');
            _replaceModalHTMLTemplateDiv(data, 'advanced_broadcast_template');
            _replaceModalHTMLTemplateDiv(data, 'advanced_note_to_self_template');
        }
        promises.push(fetchResource("html/modals/templates.html").then(data => {
            return loadAllHTML.then(() => replaceModalTemplates(data));
        }));
    };

    NRS.preloadModalUIElements = function() {
        promises.push(fetchResource("html/modals/ui_elements.html").then((data) => {
            _modalUIElements = data
        }));
    };

    NRS.whenWithProgress = function(arrayOfPromises, progressCallback) {
        var cntr = 0;
        for (var i = 0; i < arrayOfPromises.length; i++) {
            arrayOfPromises[i].then(() => {
                progressCallback(++cntr, arrayOfPromises.length);
            });
        }
        return Promise.all(arrayOfPromises);
     }

    NRS.updateLoadingProgress = function(done, total) {
        const progress = $('#progress');
        const progressStatic = $('#progress-static-img');
        const progressPercent = (done / total) * 100;
        progress.css('width', progressPercent + '%');
        progressStatic.css('filter', 'grayscale(' + Math.floor(100 - progressPercent) + '%)');
    }

    NRS.onSiteBuildDone = function() {
        return Promise.all(promises);
    }

    NRS.initModalUIElement = function($modal, selector, elementName, context) {
        var html = $(_modalUIElements).filter('div#' + elementName).html();
        var template = Handlebars.compile(html);
        var $elems = $modal.find("div[data-modal-ui-element='" + elementName + "']" + selector);

        var modalId = $modal.attr('id');
        var modalName = modalId.replace('_modal', '');
        context["modalId"] = modalId;
        context["modalName"] = modalName;

        $elems.each(function() {
            $(this).empty();
            $(this).append(template(context));
            $(this).parent().find("[data-i18n]").i18n();
        });

       return $elems;
    };

    NRS.overrideCopyrightNotice = function(path) {
        promises.push(fetch(path).then(async response => {
            if (response.ok) {
                const data = await response.text();
                await Promise.all(loadPromises);
                return $("#copyright_notice").html(data);
            }
        }));
    }

    function _appendToSidebar(menuHTML, id, desiredPosition) {
        if ($('#' + id).length == 0) {
            var inserted = false;
            var sidebarMenu = $("#sidebar_menu");
            $.each(sidebarMenu.find('> li'), function(key, elem) {
                var compPos = $(elem).data("sidebarPosition");
                if (!inserted && compPos && desiredPosition <= parseInt(compPos)) {
                    $(menuHTML).insertBefore(elem);
                    inserted = true;
                }
            });
            if (!inserted) {
                sidebarMenu.append(menuHTML);
            }
            sidebarMenu.find("[data-i18n]").i18n();
        }
    }

    NRS.initSidebarMenu = function() {
        $("#sidebar_menu").html("");
    };

    NRS.addSimpleSidebarMenuItem = function(options) {
        if (!NRS.isApiEnabled(options.depends)) {
            return;
        }
        var menuHTML = '<li id="' + options["id"] + '" class="sm_simple" data-sidebar-position="' + options["desiredPosition"] + '">';
        menuHTML += '<a href="#" data-page="' + options["page"] + '">' + options["titleHTML"] + '</a></li>';
        _appendToSidebar(menuHTML, options["id"], options["desiredPosition"]);

    };

    NRS.addTreeviewSidebarMenuItem = function(options) {
        if (!NRS.isApiEnabled(options.depends)) {
            return;
        }
        var menuHTML = '<li class="treeview" id="' + options["id"] + '" class="sm_treeview" data-sidebar-position="' + options["desiredPosition"] + '">';
        menuHTML += '<a href="#" data-page="' + options["page"] + '">' + options["titleHTML"] + '<i class="far pull-right fa-angle-right" style="padding-top:3px"></i></a>';
        menuHTML += '<ul class="treeview-menu" style="display: none;"></ul>';
        menuHTML += '</li>';
        _appendToSidebar(menuHTML, options["id"], options["desiredPosition"]);
    };
    
    NRS.appendMenuItemToTSMenuItem = function(itemId, options) {
        if (!NRS.isApiEnabled(options.depends)) {
            return;
        }
        var parentMenu = $('#' + itemId + ' ul.treeview-menu');
        if (parentMenu.length == 0) {
            return;
        }
        var menuHTML ='<li class="sm_treeview_submenu"><a href="#" ';
        if (options["type"] == 'PAGE' && options["page"]) {
            menuHTML += 'data-page="' + options["page"] + '"';
        } else if (options["type"] == 'MODAL' && options["modalId"]) {
            menuHTML += 'data-toggle="modal" data-target="#' + options["modalId"] + '"';
        } else {
            return false;
        }
        menuHTML += '><i class="far fa-angle-double-right"></i> ';
        menuHTML += options["titleHTML"] + ' <span class="badge" style="display:none;"></span></a></li>';
        parentMenu.append(menuHTML);
    };

    NRS.appendSubHeaderToTSMenuItem = function(itemId, options) {
        if (!NRS.isApiEnabled(options.depends)) {
            return;
        }
        var parentMenu = $('#' + itemId + ' ul.treeview-menu');
        if (parentMenu.length == 0) {
            return;
        }
        var menuHTML ='<li class="sm_treeview_submenu" style="background-color:#eee;color:#777;padding-top:3px;padding-bottom:3px;">';
        menuHTML += '<span class="sm_sub_header"><span style="display:inline-block;width:20px;">&nbsp;</span> ';
        menuHTML += options["titleHTML"] + ' </span></li>';
        parentMenu.append(menuHTML);
    };

    function widgetVisibility(widget, depends, chain) {
        if (NRS.isApiEnabled(depends) && chain === NRS.getActiveChainId()) {
            widget.show();
        } else {
            widget.hide();
        }
    }
    NRS.initHeader = function() {
        var activeChainId = NRS.getActiveChainId();
        var eurChainId = NRS.findChainByName("AEUR");
        widgetVisibility($("#header_send_money"), { apis: [NRS.constants.REQUEST_TYPES.sendMoney] }, activeChainId);
        widgetVisibility($("#header_send_message"), { apis: [NRS.constants.REQUEST_TYPES.sendMessage] }, activeChainId == eurChainId ? -1 : activeChainId);
        widgetVisibility($("#header_withdraw_aeur"), { apis: [NRS.constants.REQUEST_TYPES.sendMoney] }, eurChainId);
    };

    NRS.getUrlParameter = function (param) {
		var url = window.location.search.substring(1);
		var urlParams = url.split('&');
        for (var i = 0; i < urlParams.length; i++) {
			var paramKeyValue = urlParams[i].split('=');
            if (paramKeyValue.length != 2) {
                continue;
            }
            if (paramKeyValue[0] == param) {
				return paramKeyValue[1];
			}
		}
		return false;
    };

    return NRS;
}(NRS || {}, jQuery));