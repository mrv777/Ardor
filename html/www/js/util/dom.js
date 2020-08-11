/******************************************************************************
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

$("body").on("click", "#decrypt_note_form_container button.btn-primary", function(e) {
    e.preventDefault();
    NRS.decryptNoteFormSubmit();
});

$("body").on("click", ".description_toggle", function (e) {
    e.preventDefault();

    if ($(this).closest(".description").hasClass("open")) {
        NRS.showPartialDescription();
    } else {
        NRS.showFullDescription();
    }
});

$("body").on("click", "#offcanvas_toggle", function (e) {
    e.preventDefault();
    if ($(window).width() <= 992) {
        let rowOffCanvas = $('.row-offcanvas');
        if (rowOffCanvas.hasClass('active')) {
            NRS.collapseSideBar();
        } else {
            NRS.expandSidebar();
        }
    } else {
        let leftSide = $(".left-side");
        if (leftSide.hasClass('collapse-left')) {
            NRS.expandSidebar();
        } else {
            NRS.collapseSideBar();
        }
    }
});

$.fn.hasAttr = function(name) {
    let attr = this.attr(name);
    return attr !== undefined && attr !== false;
};

$.fn.tree = function () {
    return this.each(function () {
        var btn = $(this).children("a").first();
        var menu = $(this).children(".treeview-menu").first();
        var isActive = $(this).hasClass('active');

        //initialize already active menus
        if (isActive) {
            menu.show();
            btn.children(".fa-angle-right").first().removeClass("fa-angle-right").addClass("fa-angle-down");
        }
        //Slide open or close the menu on link click
        btn.click(function (e) {
            e.preventDefault();
            if (isActive) {
                //Slide up to close menu
                menu.slideUp();
                isActive = false;
                btn.children(".fa-angle-down").first().removeClass("fa-angle-down").addClass("fa-angle-right");
                btn.parent("li").removeClass("active");
            } else {
                //Slide down to open menu
                menu.slideDown();
                isActive = true;
                btn.children(".fa-angle-right").first().removeClass("fa-angle-right").addClass("fa-angle-down");
                btn.parent("li").addClass("active");
            }
        });
    });
};

var userAgent = navigator.userAgent, isAndroid = /android/i.test(userAgent);
$.fn.decimalValidation = function (getMaxFractionLength) {
    return this.each(function () {
        $(this).focusin(function() {
            var maxFractionLength = $.proxy(getMaxFractionLength, this).call();
            var prevVal = $(this).val();
            $(this).on("keydown.dec_validation", function (e) {
                var charCode = !e.charCode ? e.which : e.charCode;
                if (NRS.isControlKey(charCode) || e.ctrlKey || e.metaKey) {
                    return;
                }
                var caretPos = $(this)[0].selectionStart;
                NRS.validateDecimals(maxFractionLength, charCode, $(this).val(), caretPos, e);
                prevVal = $(this).val();
            });
            $(this).on("paste.dec_validation", function() {
                var input = $(this);
                setTimeout(function() {
                    if (NRS.checkTextDecimals(input.val(), maxFractionLength)) {
                        prevVal = input.val();
                    } else {
                        input.val(prevVal);
                    }
                }, 0);
            });
            if (isAndroid) {
                $(this).on("input.dec_validation", function() {
                    if (NRS.checkTextDecimals($(this).val(), maxFractionLength)) {
                        prevVal = $(this).val();
                    } else {
                        $(this).val(prevVal);
                    }
                });
            }
        }).focusout(function () {
            $(this).off(".dec_validation");
        });
    });
};
