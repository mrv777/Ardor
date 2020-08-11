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
		NRS.tours = {};

		NRS.activeTour = null;

		// Step index for the last step on sidebar
		const LAST_SIDEBAR_STEP = 20;
		// Step index for first tab of Account Details modal
		const ACCOUNT_DETAILS_TOUR_STEP_INDEX = 22;
		// Step index for second tab of Account Details modal
		const ACCOUNT_LEASING_TOUR_STEP_INDEX = 24;
		// Step index for third tab of Account Details modal
		const ACCOUNT_CONTROL_TOUR_STEP_INDEX = 25;
		// Step index for step after Account Details modal
		const SEND_COINS_TOUR_STEP_INDEX = 26;
		// Step index for first step of the Send Money modal
		const RECIPIENT_FIELD_TOUR_STEP_INDEX = 27;
		// Step index for step after Send Money modal
		const SEND_MESSAGE_TOUR_STEP_INDEX = 31;

		$("#login_tour").on("click", () => {
			let activeTab = $("#login_buttons").children(".active");
			let tourName = activeTab.attr("data-tab");
			NRS.tours[tourName + "Login"]();
		});

		NRS.tours.accountLogin = function() {
			if (NRS.activeTour) {
				NRS.activeTour.end();
			}

			let tour = new Tour({
				name: "accountLogin",
				template: getTourTemplate(),
				steps: [
					{
						element: "#dropdownMenu1",
						placement: "bottom",
						title: $.t("account_login_bookmarked_accounts_title"),
						content: $.t("account_login_bookmarked_accounts_content"),
					},
					{
						element: "#login_account_other",
						placement: "bottom",
						title: $.t("account_login_account_other_title"),
						content: $.t("account_login_account_other_content"),
					},
					{
						element: ".bookmark-account-form-group",
						placement: "bottom",
						title: $.t("account_login_bookmark_account_title"),
						content: $.t("account_login_bookmark_account_content"),
					}
				],
				onEnd: function() {
					NRS.activeTour = null;
				}
			});
			tour.start(true);
			NRS.activeTour = tour;
		};

		NRS.tours.passphraseLogin = function() {
			if (NRS.activeTour) {
				NRS.activeTour.end();
			}

			let tour = new Tour({
				name: "passphraseLogin",
				template: getTourTemplate(),
				steps: [
					{
						element: "#login_password",
						placement: "bottom",
						title: $.t("passphrase_login_password_title"),
						content: $.t("passphrase_login_password_content"),
					},
					{
						element: "#login_password_account",
						placement: "bottom",
						title: $.t("passphrase_login_password_account_title"),
						content: $.t("passphrase_login_password_account_content"),
					},
					{
						element: ".remember-me-form-group",
						placement: "bottom",
						title: $.t("passphrase_login_remember_me_title"),
						content: $.t("passphrase_login_remember_me_content"),
					}
				],
				onEnd: function() {
					NRS.activeTour = null;
				}
			});
			tour.start(true);
			NRS.activeTour = tour;
		};

		NRS.tours.hardwareLogin = function() {
			if (NRS.activeTour) {
				NRS.activeTour.end();
			}

			let tour = new Tour({
				name: "hardwareLogin",
				template: getTourTemplate(),
				steps: [
					{
						element: "#hardware_from_child",
						placement: "bottom",
						title: $.t("login_from_child_title"),
						content: $.t("login_from_child_content"),
					},
					{
						element: "#hardware_to_child",
						placement: "bottom",
						title: $.t("login_to_child_title"),
						content: $.t("login_to_child_content"),
					},
					{
						element: "#hardware_stop_at_new_account",
						placement: "top",
						title: $.t("login_stop_at_new_title"),
						content: $.t("login_stop_at_new_content"),
					},
					{
						element: "#login_hardware_advanced",
						placement: "bottom",
						title: $.t("login_advanced_title"),
						content: $.t("login_advanced_content"),
					},
					{
						element: "#login_hardware_load_accounts",
						placement: "bottom",
						title: $.t("login_load_title"),
						content: $.t("login_hardware_load_content"),
					},
					{
						element: "#login_hardware_account",
						placement: "bottom",
						title: $.t("login_derived_accounts_title"),
						content: $.t("login_hardware_derived_accounts_content"),
					}
				],
				onEnd: function() {
					NRS.activeTour = null;
				}
			});
			tour.start(true);
			NRS.activeTour = tour;
		};

		NRS.tours.seedLogin = function() {
			if (NRS.activeTour) {
				NRS.activeTour.end();
			}

			let tour = new Tour({
				name: "seedLogin",
				template: getTourTemplate(),
				steps: [
					{
						element: "#login_seed",
						placement: "bottom",
						title: $.t("login_seed_title"),
						content: $.t("login_seed_content"),
					},
					{
						element: "#login_master_public_key",
						placement: "bottom",
						title: $.t("login_master_public_key_title"),
						content: $.t("login_master_public_key_content")
					},
					{
						element: "#seed_from_child",
						placement: "bottom",
						title: $.t("login_from_child_title"),
						content: $.t("login_from_child_content"),
					},
					{
						element: "#seed_to_child",
						placement: "bottom",
						title: $.t("login_to_child_title"),
						content: $.t("login_to_child_content"),
					},
					{
						element: "#seed_stop_at_new_account",
						placement: "top",
						title: $.t("login_stop_at_new_title"),
						content: $.t("login_stop_at_new_content"),
					},
					{
						element: "#login_seed_advanced",
						placement: "bottom",
						title: $.t("login_advanced_title"),
						content: $.t("login_advanced_content"),
					},
					{
						element: "#login_seed_load_accounts",
						placement: "bottom",
						title: $.t("login_load_title"),
						content: $.t("login_seed_load_content"),
					},
					{
						element: "#login_seed_account",
						placement: "bottom",
						title: $.t("login_derived_accounts_title"),
						content: $.t("login_seed_derived_accounts_content"),
					}
				],
				onEnd: function() {
					NRS.activeTour = null;
				}
			});
			tour.start(true);
			NRS.activeTour = tour;
		};

		NRS.tours.login = function(forceStart) {
			if (NRS.activeTour) {
				NRS.activeTour.end();
			}

			let tour = new Tour({
				name: "login",
				template: getTourTemplate(),
				steps: [
					{
						element: "#login_panel",
						placement: "auto bottom",
						smartPlacement: true,
						title: $.t("login_main_intro_step_title"),
						content: $.t("login_main_intro_step_content"),
						onNext: function() {
							$("#login_buttons > li[data-tab=seed]").click();
						}
					},
					{
						element: "#login_buttons > li[data-tab=seed]",
						placement: "bottom",
						title: $.t("login_mode_seed_step_title"),
						content: $.t("login_mode_seed_step_content"),
						onNext: function() {
							$("#login_buttons > li[data-tab=hardware]").click();
						}
					},
					{
						element: "#login_buttons > li[data-tab=hardware]",
						placement: "bottom",
						title: $.t("login_mode_hardware_step_title"),
						content: $.t("login_mode_hardware_step_content"),
						onPrev: function() {
							$("#login_buttons > li[data-tab=seed]").click();
						},
						onNext: function() {
							$("#login_buttons > li[data-tab=passphrase]").click();
						}
					},
					{
						element: "#login_buttons > li[data-tab=passphrase]",
						placement: "bottom",
						title: $.t("login_mode_password_step_title"),
						content: $.t("login_mode_password_step_content"),
						onPrev: function() {
							$("#login_buttons > li[data-tab=hardware]").click();
						},
						onNext: function() {
							$("#login_buttons > li[data-tab=account]").click();
						}
					},
					{
						element: "#login_buttons > li[data-tab=account]",
						placement: "bottom",
						title: $.t("login_mode_account_step_title"),
						content: $.t("login_mode_account_step_content"),
						onPrev: function() {
							$("#login_buttons > li[data-tab=passphrase]").click();
						}
					},
					{
						element: "#login_buttons",
						placement: "bottom",
						title: $.t("login_mode_summary_step_title"),
						content: $.t("login_mode_summary_step_content")
					},
					{
						element: "#login_chain_selector",
						placement: "bottom",
						title: $.t("login_mode_chain_selector_title"),
						content: $.t("login_mode_chain_selector_content")
					},
					{
						element: "#login_language_selector",
						placement: "bottom",
						title: $.t("login_mode_language_selector_title"),
						content: $.t("login_mode_language_selector_content")
					}
				],
				onEnd: function() {
					NRS.activeTour = null;
				}
			});
			tour.start(forceStart);
			NRS.activeTour = tour;
			if (forceStart) {
				localStorage.removeItem("login_current_step");
				localStorage.removeItem("login_end");
			}
		};

		NRS.tours.dashboard = function(forceStart) {
			if (NRS.activeTour) {
				$.growl($.t("tour_is_already_active", { tour: NRS.activeTour.name }));
				return;
			}

			let tour = new Tour({
				name: "dashboard",
				autoscroll: true,
				onShow: function(tourObj, stepNumber) {
					let step = tourObj.getStep(stepNumber);
					let stepElement = document.querySelector(step.element);
					if (stepElement) {
						stepElement.scrollIntoView({block: "center"});
					}
					if (stepNumber <= LAST_SIDEBAR_STEP) {
						NRS.expandSidebar();
					} else if (stepNumber > LAST_SIDEBAR_STEP) {
						NRS.collapseSideBar();
					}
				},
				onShown: function(tourObj) {
					let step = tourObj.getStep(tourObj.getCurrentStep());
					let $containerElement = $(step.container);
					$containerElement.on("scroll.tour", function() {
						tourObj.end();
					});
					// Add disabled class to the settings menu item.
					$("#tours_menu_li").toggleClass("disabled", true);
					NRS.activeTour = tourObj;
				},
				onHidden: function(tourObj) {
					let step = tourObj.getStep(tourObj.getCurrentStep());
					let $containerElement = $(step.container);
					$containerElement.off("scroll.tour");
					$("body").toggleClass("tour-dashboard-just-started", false);
				},
				onEnd: function() {
					$("#tours_menu_li").removeClass("disabled");
					NRS.activeTour = null;
				},
				template: getTourTemplate(),
				steps: [
					{
						element: "#sidebar_account_id",
						placement: "bottom",
						container: "section.sidebar",
						title: $.t("account_id_step_title"),
						content: $.t("account_id_step_content")
					},
					{
						element: "#account_name",
						placement: "bottom",
						container: "section.sidebar",
						title: $.t("set_account_info_step_title"),
						content: $.t("set_account_info_step_content")
					},
					{
						element: "#sidebar_account_link a",
						placement: "bottom",
						container: "section.sidebar",
						title: $.t("details_step_title"),
						content: $.t("details_step_content")
					},
					{
						element: "#copy_account_id",
						placement: "bottom",
						container: "section.sidebar",
						title: $.t("copy_account_id_button_step_title"),
						content: $.t("copy_account_id_button_step_content")
					},
					{
						element: "#account_id_dropdown",
						placement: "bottom",
						container: "section.sidebar",
						title: $.t("switch_account_step_title"),
						content: $.t("switch_account_step_content")
					},
					{
						element: "#sideBalance",
						placement: "bottom",
						container: "section.sidebar",
						title: $.t("coin_balance_step_title"),
						content: $.t("coin_balance_step_content")
					},
					{
						element: "#connected_indicator i",
						placement: "bottom",
						container: "section.sidebar",
						title: $.t("server_connection_status_step_title"),
						content: $.t("server_connection_status_step_content")
					},
					{
						element: "#forging_indicator i",
						placement: "bottom",
						container: "section.sidebar",
						title: $.t("forging_step_title"),
						content: $.t("forging_step_content")
					},
					{
						element: "#chain_dropdown",
						placement: "bottom",
						container: "section.sidebar",
						title: $.t("switch_chain_step_title_dashboard"),
						content: $.t("switch_chain_step_content_dashboard")
					},
					{
						element: ".user-panel .input-group.current-height",
						placement: "bottom",
						container: "section.sidebar",
						title: $.t("current_height_step_title"),
						content: $.t("current_height_step_content")
					},
					{
						element: ".user-panel .input-group.next-block",
						placement: "bottom",
						container: "section.sidebar",
						title: $.t("next_block_step_title"),
						content: $.t("next_block_step_content")
					},
					{
						element: "#id_search .input-group",
						placement: "bottom",
						container: "section.sidebar",
						title: $.t("search_by_account_id_step_title"),
						content: $.t("search_by_account_id_step_content")
					},
					{
						element: "#sidebar_coin_exchange > a",
						placement: "top",
						container: "section.sidebar",
						title: $.t("coin_exchange_step_title"),
						content: $.t("coin_exchange_step_content")
					},
					{
						element: "#sidebar_asset_exchange > a",
						placement: "top",
						container: "section.sidebar",
						title: $.t("asset_exchange_step_title"),
						content: $.t("asset_exchange_step_content")
					},
					{
						element: "#sidebar_monetary_system > a",
						placement: "top",
						container: "section.sidebar",
						title: $.t("monetary_system_step_title"),
						content: $.t("monetary_system_step_content")
					},
					{
						element: "#sidebar_voting_system > a",
						placement: "top",
						container: "section.sidebar",
						title: $.t("voting_system_step_title"),
						content: $.t("voting_system_step_content")
					},
					{
						element: "#sidebar_tagged_data > a",
						placement: "top",
						container: "section.sidebar",
						title: $.t("data_cloud_step_title"),
						content: $.t("data_cloud_step_content")
					},
					{
						element: "#sidebar_dgs_buyer > a",
						placement: "top",
						container: "section.sidebar",
						title: $.t("marketplace_step_title"),
						content: $.t("marketplace_step_content")
					},
					{
						element: "#sidebar_shuffling > a",
						placement: "top",
						container: "section.sidebar",
						title: $.t("shuffling_step_title"),
						content: $.t("shuffling_step_content")
					},
					{
						element: "#sidebar_messages > a",
						placement: "top",
						container: "section.sidebar",
						title: $.t("messages_step_title"),
						content: $.t("messages_step_content")
					},
					{
						element: "#sidebar_aliases > a",
						placement: "top",
						container: "section.sidebar",
						title: $.t("aliases_step_title"),
						content: $.t("aliases_step_content")
					},
					{
						element: "#sidebar_plugins > a",
						placement: "top",
						container: "section.sidebar",
						title: $.t("plugins_step_title"),
						content: $.t("plugins_step_content")
					},
					{
						element: ".dashboard_first_row_tile_1",
						placement: "bottom",
						title: $.t("account_balance_step_title"),
						content: $.t("account_balance_step_content"),
						onNext: function(tourObj) {
							let step = tourObj.getStep(tourObj.getCurrentStep());
							let $triggerElement = $(step.element).find('a[data-target]');
							$triggerElement.click();
							let defer = new jQuery.Deferred();
							return defer.promise();
						},
						onShown: function(tourObj) {
							let step = tourObj.getStep(tourObj.getCurrentStep());
							let $triggerElement = $(step.element).find('a[data-target]');
							let modal = $($triggerElement.attr('data-target'));
							$triggerElement.one('click', function() {
								modal.one("shown.bs.modal", function() {
									tourObj.goTo(ACCOUNT_DETAILS_TOUR_STEP_INDEX);
									modal.find("ul.nav li").on("click.tour", showTourStepOnTab)
								});
								modal.one("hide.bs.modal", function() {
									tourObj.goTo(SEND_COINS_TOUR_STEP_INDEX);
									modal.find("ul.nav li").off("click.tour");
								});
							});
						}
					},
					{
						element: "#account_details_modal_balance",
						placement: "left",
						title: $.t("account_details_step_title"),
						content: $.t("account_details_step_content"),
						onPrev: function() {
							$('#account_details_modal').modal('hide');
						}
					},
					{
						element: "#account_balance_table .account-balance-row",
						placement: "right",
						title: $.t("account_balance_public_key_step_title"),
						content: $.t("account_balance_public_key_step_content"),
						onNext: function(tourObj) {
							tourObj.goTo(SEND_COINS_TOUR_STEP_INDEX);
							$('#account_details_modal').modal('hide');
							let defer = new jQuery.Deferred();
							return defer.promise();
						}
					},
					{
						element: "#account_details_modal_leasing",
						placement: "right",
						title: $.t("account_leasing_step_title"),
						content: $.t("account_leasing_step_content"),
						onPrev: function() {
							$("#account_details_modal ul.nav li[data-tab=balance]").click();
						},
						onNext: function() {
							$("#account_details_modal ul.nav li[data-tab=account_control]").click();
						}
					},
					{
						element: "#account_details_modal_account_control",
						placement: "left",
						title: $.t("account_control_step_title"),
						content: $.t("account_control_step_content"),
						onPrev: function() {
							$("#account_details_modal ul.nav li[data-tab=leasing]").click();
						},
						onNext: function() {
							$('#account_details_modal').modal('hide');
						}
					},
					{
						element: "#header_send_money",
						placement: "bottom",
						title: $.t("send_money_step_title"),
						content: $.t("send_money_step_content"),
						onNext: function(tourObj) {
							let step = tourObj.getStep(tourObj.getCurrentStep());
							let $triggerElement = $(step.element).find('a[data-target]');
							$triggerElement.click();
							let defer = new jQuery.Deferred();
							return defer.promise();
						},
						onShown: function(tourObj) {
							let step = tourObj.getStep(tourObj.getCurrentStep());
							let $triggerElement = $(step.element).find('a[data-target]');
							let modal = $($triggerElement.attr('data-target'));
							$triggerElement.one('click', function() {
								modal.one("shown.bs.modal", function() {
									tourObj.goTo(RECIPIENT_FIELD_TOUR_STEP_INDEX);
								});
								modal.one("hide.bs.modal", function() {
									tourObj.goTo(SEND_MESSAGE_TOUR_STEP_INDEX);
								});
							});
						}
					},
					{
						element: "#send_money_recipient",
						placement: "left",
						title: $.t("recipient_field_step_title"),
						content: $.t("recipient_field_step_content"),
						onPrev: function() {
							$('#send_money_modal').modal('hide');
						}
					},
					{
						element: "#send_money_amount",
						placement: "left",
						title: $.t("amount_field_step_title"),
						content: $.t("amount_field_step_content")
					},
					{
						element: "#send_money_fee",
						placement: "left",
						title: $.t("fee_field_step_title"),
						content: $.t("fee_field_step_content")
					},
					{
						element: "#send_money_password",
						placement: "left",
						title: $.t("password_field_step_title"),
						content: $.t("password_field_step_content"),
						onNext: function() {
							$('#send_money_modal').modal('hide');
						}
					},
					{
						element: "#header_send_message",
						placement: "bottom",
						title: $.t("send_message_step_title"),
						content: $.t("send_message_step_content")
					},
					{
						element: "#shuffling_notification_menu",
						placement: "bottom",
						title: $.t("active_shufflings_step_title"),
						content: $.t("active_shufflings_step_content")
					},
					{
						element: "#unconfirmed_notification_menu",
						placement: "bottom",
						title: $.t("unconfirmed_transaction_step_title"),
						content: $.t("unconfirmed_transaction_step_content")
					},
					{
						element: "#notification_menu_container",
						placement: "bottom",
						title: $.t("notifications_step_title"),
						content: $.t("notifications_step_content")
					},
					{
						element: "#contacts_menu_li > a",
						placement: "bottom",
						title: $.t("contacts_step_title"),
						content: $.t("contacts_step_content")
					},
					{
						element: "li[data-content=Settings] > a",
						placement: "bottom",
						title: $.t("settings_step_title"),
						content: $.t("settings_step_content")
					},
					{
						element: "li[data-content=Exchange] > a",
						placement: "left",
						title: $.t("cryptocurrency_exchange_step_title"),
						content: $.t("cryptocurrency_exchange_step_content")
					},
					{
						element: "#logout_button_container",
						placement: "left",
						title: $.t("logout_step_title"),
						content: $.t("logout_step_content")
					}

				]
			});

			tour.init();
			tour.start(forceStart);
			if (!tour.ended()) { //workaround as onStart is not fired consistently
				$("body").toggleClass("tour-dashboard-just-started", true);
			}
			if (tour.getCurrentStep() && tour.getStep(tour.getCurrentStep()).next === -1) { // if restarted from the last step - rewind.
				tour.goTo(0)
			}
			if (forceStart) {
				localStorage.removeItem("login_current_step");
				localStorage.removeItem("login_end");
			}
		};

		function showTourStepOnTab(event) {
			let tab = $(event.currentTarget).attr('data-tab');
			switch(tab) {
				case "balance":
					NRS.activeTour.goTo(ACCOUNT_DETAILS_TOUR_STEP_INDEX);
					break;
				case "leasing":
					NRS.activeTour.goTo(ACCOUNT_LEASING_TOUR_STEP_INDEX);
					break;
				case "account_control":
					NRS.activeTour.goTo(ACCOUNT_CONTROL_TOUR_STEP_INDEX);
					break;
			}
		}

		function getTourTemplate() {
			return `<div class='popover tour'>
            <div class='arrow'></div>
            <h3 class='popover-title'></h3>
            <div class='popover-content'></div>
            <div class='popover-navigation'>
                <span>
                    <a href="#" class='btn btn-default btn-xs' data-role='prev'>${$.t("previous")}</a>
                </span>
                <span data-role='separator'>&nbsp;</span>
                <span>
                    <a href="#" class='btn btn-default btn-xs' data-role='next'>${$.t("next")}</a>
                </span>
                <span data-role='separator'>&nbsp;</span>
                <span>
                    <a href="#" class='btn btn-default btn-xs' data-role='end'>${$.t("end_tour")}</a>
                </span>
            </div>
        </div>`
		}

		return NRS;
	}(NRS || {}, jQuery));
});