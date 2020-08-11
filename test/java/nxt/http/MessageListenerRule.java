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

package nxt.http;

import nxt.messaging.MessagingTransactionType;
import nxt.messaging.MessagingTransactionType.MessageEvent;
import nxt.util.Listener;
import org.junit.rules.ExternalResource;

import java.util.ArrayList;
import java.util.List;

import static nxt.messaging.MessagingTransactionType.Event.ON_MESSAGE;

public class MessageListenerRule extends ExternalResource {
    private final MessageEventListener listener = new MessageEventListener();

    @Override
    protected void before() {
        listener.clear();
        MessagingTransactionType.addListener(listener, ON_MESSAGE);
    }

    @Override
    protected void after() {
        MessagingTransactionType.removeListener(listener, ON_MESSAGE);
        listener.clear();
    }

    public List<MessageEvent> getEvents() {
        return listener.getEvents();
    }

    private static class MessageEventListener implements Listener<MessageEvent> {
        private final List<MessageEvent> events = new ArrayList<>();

        @Override
        public void notify(MessageEvent messageEvent) {
            events.add(messageEvent);
        }

        private List<MessageEvent> getEvents() {
            return events;
        }

        private void clear() {
            events.clear();
        }
    }
}
