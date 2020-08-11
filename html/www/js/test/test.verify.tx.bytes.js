/*global QUnit*/
/*
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

QUnit.module("verifyTransactionBytes", function () {
    function createFetchParams(requestType) {
        let fetchParams = {
            method: "POST",
            mode: "cors",
            cache: "no-cache",
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body: new URLSearchParams()
        };
        let body = fetchParams.body;
        body.set('requestType', requestType);
        body.set('chain', '2');
        body.set('publicKey', '112e0c5748b5ea610a44a09b1ad0d2bddc945a6ef5edc7551b80576249ba585b'); // Alice's public key
        body.set('deadline', '15');
        body.set('voucher', 'true');
        return {fetchParams, body};
    }

    async function fetchAndVerify(assert, fetchParams) {
        if (!fetchParams.body.has('amountNQT')) {
            fetchParams.body.set('amountNQT', '0');
        }
        let requestType = fetchParams.body.get('requestType');

        // first test: generate voucher and compare returned transaction bytes with the transactionJSON
        let response = await fetch('/nxt', fetchParams);
        let voucher = await response.json();
        assert.notOk(voucher.errorCode, "No error code");
        assert.notOk(voucher.errorDescription, "No error description");
        let data = NRS.buildDataFromTransactionJSON(requestType, voucher.transactionJSON);
        let result = NRS.verifyTransactionBytes(voucher.unsignedTransactionBytes, requestType,
            data, voucher.transactionJSON.attachment, false);
        assert.ok(result.fail === undefined || result.fail === false, "verification against voucher: " + JSON.stringify(result));

        // second test: compare sent parameters with received transaction bytes to verify the API response
        data = {};
        for (const [key, value] of fetchParams.body) {
            data[key] = value;
        }
        result = NRS.verifyTransactionBytes(voucher.unsignedTransactionBytes, requestType,
            data, voucher.transactionJSON.attachment, false);
        assert.ok(result.fail === undefined || result.fail === false, "verification against form data: " + JSON.stringify(result));
    }

    async function getCurrentHeight() {
        let response = await fetch('nxt?requestType=getBlock');
        let json = await response.json();
        return json.height;
    }

    const aliceAccountId = '5873880488492319831';
    const bobAccountId = '16992224448242675179';
    const assetId = '4224755768739707777';
    const goodsId = '9704537788226868813';
    const currencyId = '5004610146815386929';
    let currentHeightPromise = getCurrentHeight();

    QUnit.test('sendMoney', async assert => {
        let {fetchParams, body} =  createFetchParams('sendMoney');
        body.set('recipient', bobAccountId);
        body.set('amountNQT', '42');
        await fetchAndVerify(assert, fetchParams);
    });

    QUnit.test('sendMessage', async assert => {
        let {fetchParams, body} =  createFetchParams('sendMessage');
        body.set('recipient', bobAccountId);
        body.set('message', 'test');
        body.set('messageIsText', 'true');
        await fetchAndVerify(assert, fetchParams);
    });

    QUnit.test('setAlias', async assert => {
        let {fetchParams, body} =  createFetchParams('setAlias');
        body.set('aliasName', 'cFg5cyQYwAyGlP8CGOxiGkwyHJ22tKUXbNrUOQ5yZAyMbc4aDEYIBe09q4Fh6Y3oIinns6twUsMPWkgzMT4dS4GkxcFtrGxuQoXQ');
        body.set('aliasURI', 'test value');
        await fetchAndVerify(assert, fetchParams);
    });

    QUnit.test('createPoll', async assert => {
        let currentHeight = await currentHeightPromise;
        let {fetchParams, body} =  createFetchParams('createPoll');
        body.set('name', 'test poll');
        body.set('description', 'test description');
        body.set('finishHeight', String(currentHeight + 1000));
        body.set('votingModel', '0');
        body.set('minNumberOfOptions', '1');
        body.set('maxNumberOfOptions', '1');
        body.set('minRangeValue', '0');
        body.set('maxRangeValue', '0');
        body.set('option00', 'ans1');
        body.set('option01', 'ans2');
        await fetchAndVerify(assert, fetchParams);
    });

    QUnit.test('setAccountInfo', async assert => {
        let {fetchParams, body} =  createFetchParams('setAccountInfo');
        body.set('name', 'testname');
        body.set('description', 'test description');
        await fetchAndVerify(assert, fetchParams);
    });

    QUnit.test('sellAlias', async assert => {
        let {fetchParams, body} =  createFetchParams('sellAlias');
        body.set('aliasName', '7R335');
        body.set('priceNQT', '42');
        await fetchAndVerify(assert, fetchParams);
    });

    QUnit.test('deleteAlias', async assert => {
        let {fetchParams, body} =  createFetchParams('deleteAlias');
        body.set('aliasName', '7R335');
        await fetchAndVerify(assert, fetchParams);
    });

    QUnit.test('setAccountProperty', async assert => {
        let {fetchParams, body} =  createFetchParams('setAccountProperty');
        body.set('recipient', bobAccountId); // Bob
        body.set('property', 'test property');
        body.set('value', 'test value');
        await fetchAndVerify(assert, fetchParams);
    });

    QUnit.test('deleteAccountProperty', async assert => {
        let {fetchParams, body} =  createFetchParams('deleteAccountProperty');
        body.set('property', 'Key1');
        body.set('recipient', '5873880488492319831');
        await fetchAndVerify(assert, fetchParams);
    });

    QUnit.test('issueAsset', async assert => {
        let {fetchParams, body} =  createFetchParams('issueAsset');
        body.set('name', 'test');
        body.set('description', 'Singleton asset qunit test.');
        body.set('quantityQNT', '1');
        body.set('decimals', '0');
        await fetchAndVerify(assert, fetchParams);
    });

    QUnit.test('transferAsset', async assert => {
        let {fetchParams, body} =  createFetchParams('transferAsset');
        body.set('recipient', bobAccountId);
        body.set('asset', assetId);
        body.set('quantityQNT', '42');
        await fetchAndVerify(assert, fetchParams);
    });

    QUnit.test('placeAskOrder', async assert => {
        let {fetchParams, body} =  createFetchParams('placeAskOrder');
        body.set('asset', assetId);
        body.set('quantityQNT', '1000');
        body.set('priceNQTPerShare', '42');
        await fetchAndVerify(assert, fetchParams);
    });

    QUnit.test('placeBidOrder', async assert => {
        let {fetchParams, body} =  createFetchParams('placeBidOrder');
        body.set('asset', assetId);
        body.set('quantityQNT', '42');
        body.set('priceNQTPerShare', '1000');
        await fetchAndVerify(assert, fetchParams);
    });

    QUnit.test('deleteAssetShares', async assert => {
        let {fetchParams, body} =  createFetchParams('deleteAssetShares');
        body.set('asset', assetId);
        body.set('quantityQNT', '42');
        await fetchAndVerify(assert, fetchParams);
    });

    QUnit.test('increaseAssetShares', async assert => {
        let {fetchParams, body} =  createFetchParams('increaseAssetShares');
        body.set('asset', assetId);
        body.set('quantityQNT', '42');
        await fetchAndVerify(assert, fetchParams);
    });

    QUnit.test('dividendPayment', async assert => {
        let currentHeight = await currentHeightPromise;
        let {fetchParams, body} =  createFetchParams('dividendPayment');
        body.set('asset', assetId);
        body.set('amountNQTPerShare', '42');
        body.set('height', String(currentHeight - 1000));
        body.set('holding', '2');
        body.set('holdingType', '0');
        await fetchAndVerify(assert, fetchParams);
    });

    QUnit.test('setAssetProperty', async assert => {
        let {fetchParams, body} =  createFetchParams('setAssetProperty');
        body.set('asset', assetId);
        body.set('property', 'test property');
        body.set('value', 'test value');
        await fetchAndVerify(assert, fetchParams);
    });

    QUnit.test('deleteAssetProperty', async assert => {
        let {fetchParams, body} =  createFetchParams('deleteAssetProperty');
        body.set('asset', assetId);
        body.set('property', 'unittest');
        await fetchAndVerify(assert, fetchParams);
    });

    QUnit.test('dgsListing', async assert => {
        let {fetchParams, body} =  createFetchParams('dgsListing');
        body.set('name', 'test name');
        body.set('description', 'test description');
        body.set('quantity', '5');
        body.set('priceNQT', '42');
        body.set('tags', '');
        await fetchAndVerify(assert, fetchParams);
    });

    QUnit.test('dgsDelisting', async assert => {
        let {fetchParams, body} =  createFetchParams('dgsDelisting');
        body.set('goods', goodsId);
        await fetchAndVerify(assert, fetchParams);
    });

    QUnit.test('dgsPriceChange', async assert => {
        let {fetchParams, body} =  createFetchParams('dgsPriceChange');
        body.set('goods', goodsId);
        body.set('priceNQT', '42');
        await fetchAndVerify(assert, fetchParams);
    });

    QUnit.test('dgsQuantityChange', async assert => {
        let {fetchParams, body} =  createFetchParams('dgsQuantityChange');
        body.set('goods', goodsId);
        body.set('deltaQuantity', '42');
        await fetchAndVerify(assert, fetchParams);
    });

    QUnit.test('dgsPurchase', async assert => {
        let {fetchParams, body} =  createFetchParams('dgsPurchase');
        body.set('goods', goodsId);
        body.set('quantity', '2');
        body.set('priceNQT', '120000000000');
        body.set('deliveryDeadlineTimestamp', String(NRS.toEpochTime() + 1000));
        body.set('recipient', aliceAccountId);
        await fetchAndVerify(assert, fetchParams);
    });

    QUnit.test('leaseBalance', async assert => {
        let {fetchParams, body} =  createFetchParams('leaseBalance');
        body.set('recipient', bobAccountId);
        body.set('period', '1500');
        body.set('chain', 1);
        await fetchAndVerify(assert, fetchParams);
    });

    QUnit.test('issueCurrency', async assert => {
        let {fetchParams, body} =  createFetchParams('issueCurrency');
        body.set('name', 'UnitTest');
        body.set('code', 'QUNIT');
        body.set('description', 'test description');
        body.set('type', '33');
        body.set('initialSupplyQNT', '42');
        body.set('maxSupplyQNT', '42');
        await fetchAndVerify(assert, fetchParams);
    });

    QUnit.test('issueCurrency.reservable', async assert => {
        let currentHeight = await currentHeightPromise;
        let {fetchParams, body} =  createFetchParams('issueCurrency');
        body.set('name', 'UnitTest');
        body.set('code', 'QUNIT');
        body.set('description', 'test description');
        body.set('type', '37');
        body.set('initialSupplyQNT', '42');
        body.set('maxSupplyQNT', '100');
        body.set('reserveSupplyQNT', '100');
        body.set('minReservePerUnitNQT', '1');
        body.set('issuanceHeight', currentHeight + 1000);
        await fetchAndVerify(assert, fetchParams);
    });

    QUnit.test('transferCurrency', async assert => {
        let {fetchParams, body} =  createFetchParams('transferCurrency');
        body.set('currency', currencyId);
        body.set('unitsQNT', '42');
        body.set('recipient', bobAccountId);
        await fetchAndVerify(assert, fetchParams);
    });

    QUnit.test('publishExchangeOffer', async assert => {
        let currentHeight = await currentHeightPromise;
        let {fetchParams, body} =  createFetchParams('publishExchangeOffer');
        body.set('currency', currencyId);
        body.set('buyRateNQTPerUnit', '2');
        body.set('sellRateNQTPerUnit', '3');
        body.set('totalBuyLimitQNT', '10');
        body.set('totalSellLimitQNT', '11');
        body.set('initialBuySupplyQNT', '4');
        body.set('initialSellSupplyQNT', '5');
        body.set('expirationHeight', currentHeight + 1000);
        await fetchAndVerify(assert, fetchParams);
    });

    QUnit.test('shufflingCreate', async assert => {
        let {fetchParams, body} =  createFetchParams('shufflingCreate');
        body.set('holdingType', '0');
        body.set('holding', '2');
        body.set('amount', '4200000000');
        body.set('participantCount', '3');
        body.set('registrationPeriod', '1000');
        await fetchAndVerify(assert, fetchParams);
    });

    QUnit.test('uploadTaggedData', async assert => {
        let {fetchParams, body} =  createFetchParams('uploadTaggedData');
        body.set('data', 'test data');
        body.set('name', 'test name');
        body.set('description', '');
        body.set('tags', '');
        body.set('type', 'text/plain');
        body.set('channel', '');
        body.set('isText', 'true');
        body.set('filename', '');
        await fetchAndVerify(assert, fetchParams);
    });
});
