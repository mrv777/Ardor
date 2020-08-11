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

package nxt.http;

import org.junit.rules.ExternalResource;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static java.util.stream.Collectors.toList;

public class LogListeningRule extends ExternalResource {
    private final String loggerName;
    private final MyHandler handler = new MyHandler();


    public LogListeningRule() {
        this("");
    }

    public LogListeningRule(String loggerName) {
        this.loggerName = loggerName;
    }

    @Override
    protected void before() {
        Logger.getLogger(loggerName).addHandler(handler);
    }

    @Override
    protected void after() {
        Logger.getLogger(loggerName).removeHandler(handler);
        handler.clear();
    }

    List<String> getMessages() {
        return handler.getLogRecords().stream()
                .map(LogRecord::getMessage)
                .collect(toList());
    }

    private static class MyHandler extends Handler {
        private final List<LogRecord> logRecords = new ArrayList<>();

        @Override
        public void publish(LogRecord record) {
            logRecords.add(record);
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() throws SecurityException {
        }

        List<LogRecord> getLogRecords() {
            return logRecords;
        }

        void clear() {
            logRecords.clear();
        }
    }
}
