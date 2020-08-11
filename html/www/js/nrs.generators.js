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
    NRS = (function(NRS) {

        const DEFAULT_GENERATION_DELAY = 10;
        var timer = null;
        var generators = [];
        var lastBlockTime;
        var timeFormatted;
        var heightFormatted;
        var activeCount;

        NRS.pages.generators = function() {
            NRS.preparePage();
            NRS.renderGenerators(false);
        };

        NRS.renderGenerators = function(isRefresh) {
            var view;
            if (isRefresh) {
                const $generatorsRemainingCells = $(".generators-remaining");
                generators.forEach(
                    function(generator, index) {
                        let nextBlockTime = NRS.getEstimatedNextBlockTime(generator.deadline, lastBlockTime);
                        $generatorsRemainingCells.eq(index).text(nextBlockTime);
                    }
                );
                return;
            }
            if (timer) {
                clearInterval(timer);
                timer = null;
            }
            NRS.hasMorePages = false;
            view = NRS.simpleview.get('generators_page', {
                errorMessage: null,
                infoMessage: NRS.getGeneratorAccuracyWarning(),
                isLoading: true,
                isEmpty: false,
                generators: [],
                timeFormatted: "<span>.</span><span>.</span><span>.</span></span>",
                heightFormatted: "<span>.</span><span>.</span><span>.</span></span>",
                activeCount: "<span>.</span><span>.</span><span>.</span></span>",
                loadingDotsClass: "loading_dots"
            });
            var params = {
                "limit": 10
            };
            NRS.sendRequest("getNextBlockGenerators+", params,
                function(response) {
                    view.generators.length = 0;
                    lastBlockTime = response.timestamp;
                    if (!response.generators) {
                        view.render({
                            isLoading: false,
                            isEmpty: true,
                            errorMessage: NRS.getErrorMessage(response)
                        });
                        return;
                    }
                    response.generators.forEach(
                        function(generatorsJson) {
                            view.generators.push(NRS.jsondata.generators(generatorsJson));
                        }
                    );
                    timeFormatted = NRS.formatTimestamp(response.timestamp);
                    heightFormatted = String(response.height).escapeHTML();
                    activeCount = String(response.activeCount).escapeHTML();
                    view.render({
                        isLoading: false,
                        isEmpty: view.generators.length == 0,
                        timeFormatted: timeFormatted,
                        heightFormatted: heightFormatted,
                        activeCount: activeCount,
                        loadingDotsClass: ""
                    });
                    NRS.pageLoaded();
                    if (NRS.currentPage == "generators") {
                        generators = view.generators;
                        timer = setInterval(function() {
                            if (NRS.currentPage != "generators") {
                                clearInterval(timer);
                            } else {
                                NRS.renderGenerators(true);
                            }
                        }, 1000);
                    }
                }
            );
        };

        NRS.jsondata.generators = function(generator) {
            let nextBlockTime = NRS.getEstimatedNextBlockTime(generator.deadline, lastBlockTime);
            return {
                accountFormatted: NRS.getAccountLink(generator, "account"),
                balanceFormatted: NRS.formatAmount(generator.effectiveBalanceFXT),
                hitTimeFormatted: NRS.formatTimestamp(generator.hitTime),
                deadline: generator.deadline,
                remaining: nextBlockTime
            };
        };

        NRS.incoming.generators = function() {
            NRS.renderGenerators(false);
        };

        NRS.getEstimatedNextBlockTime = function(generatorDeadline, lastBlockTime) {
            return generatorDeadline - (NRS.toEpochTime() - lastBlockTime) + DEFAULT_GENERATION_DELAY;
        };

        return NRS;
    }(NRS || {}, jQuery));
});