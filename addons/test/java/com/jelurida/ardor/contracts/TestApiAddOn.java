package com.jelurida.ardor.contracts;

import nxt.addons.AddOn;
import nxt.http.APIServlet;
import nxt.http.APITag;
import nxt.util.JSON;
import org.json.simple.JSONStreamAware;
import org.junit.rules.ExternalResource;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Collections.synchronizedList;

public class TestApiAddOn implements AddOn {
    private static Function<HttpServletRequest, JSONStreamAware> requestHandler;

    static {
        reset();
    }

    @Override
    public Map<String, APIServlet.APIRequestHandler> getAPIRequests() {
        return Collections.singletonMap("TestApiAddOn", new APIServlet.APIRequestHandler(new APITag[]{APITag.ADDONS}) {
            @Override
            protected JSONStreamAware processRequest(HttpServletRequest request) {
                return getRequestHandler().apply(request);
            }
        });
    }

    public static synchronized Function<HttpServletRequest, JSONStreamAware> getRequestHandler() {
        return requestHandler;
    }

    public static synchronized void setRequestHandler(Function<HttpServletRequest, JSONStreamAware> requestHandler) {
        TestApiAddOn.requestHandler = requestHandler;
        if (requestHandler == null) {
            reset();
        }
    }

    public static void reset() {
        setRequestHandler(ignored -> null);
    }

    public static class CollectMessagesRule extends ExternalResource {
        private final List<String> messages = synchronizedList(new ArrayList<>());

        @Override
        protected void before() {
            TestApiAddOn.setRequestHandler(request -> {
                try {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(request.getInputStream()))) {
                        final String message = reader.lines().collect(Collectors.joining("\n"));
                        messages.add(message);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return JSON.emptyJSON;
            });
        }

        public List<String> getMessages() {
            return messages;
        }

        public void clear() {
            messages.clear();
        }
    }
}
