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
package nxt.peer;

import nxt.http.APIEnum;

import java.util.Set;

public class DummyPeer implements Peer {
    @Override
    public State getState() {
        return null;
    }

    @Override
    public String getHost() {
        return null;
    }

    @Override
    public String getAnnouncedAddress() {
        return null;
    }

    @Override
    public long getDownloadedVolume() {
        return 0;
    }

    @Override
    public long getUploadedVolume() {
        return 0;
    }

    @Override
    public String getApplication() {
        return null;
    }

    @Override
    public String getVersion() {
        return null;
    }

    @Override
    public String getPlatform() {
        return null;
    }

    @Override
    public String getSoftware() {
        return null;
    }

    @Override
    public int getPort() {
        return 0;
    }

    @Override
    public int getApiPort() {
        return 0;
    }

    @Override
    public int getApiSSLPort() {
        return 0;
    }

    @Override
    public boolean shareAddress() {
        return false;
    }

    @Override
    public Set<APIEnum> getDisabledAPIs() {
        return null;
    }

    @Override
    public int getApiServerIdleTimeout() {
        return 0;
    }

    @Override
    public BlockchainState getBlockchainState() {
        return null;
    }

    @Override
    public boolean isBlacklisted() {
        return false;
    }

    @Override
    public String getBlacklistingCause() {
        return null;
    }

    @Override
    public void connectPeer() {

    }

    @Override
    public boolean isHandshakePending() {
        return false;
    }

    @Override
    public void disconnectPeer() {

    }

    @Override
    public void blacklist(Exception cause) {

    }

    @Override
    public void blacklist(String cause) {

    }

    @Override
    public void unBlacklist() {

    }

    @Override
    public int getLastUpdated() {
        return 0;
    }

    @Override
    public int getLastConnectAttempt() {
        return 0;
    }

    @Override
    public boolean isInbound() {
        return false;
    }

    @Override
    public boolean providesService(Service service) {
        return false;
    }

    @Override
    public boolean isOpenAPI() {
        return false;
    }

    @Override
    public boolean isApiConnectable() {
        return false;
    }

    @Override
    public StringBuilder getPeerApiUri() {
        return null;
    }

    @Override
    public boolean providesServices(long services) {
        return false;
    }

    @Override
    public void sendMessage(NetworkMessage message) {

    }

    @Override
    public NetworkMessage sendRequest(NetworkMessage message) {
        return null;
    }

    @Override
    public void waitHandshake() {

    }
}
