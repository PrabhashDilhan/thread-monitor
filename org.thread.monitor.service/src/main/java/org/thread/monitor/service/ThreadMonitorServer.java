package org.thread.monitor.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class ThreadMonitorServer {
    private static final Log logger = LogFactory.getLog(ThreadMonitorServer.class);
    private static final int DEFAULT_PORT = 8080;
    
    private final int port;
    private final Map<String, ThreadMonitor> monitors;
    private final AtomicBoolean isRunning;
    private HttpServer server;

    public ThreadMonitorServer() {
        this(DEFAULT_PORT);
    }

    public ThreadMonitorServer(int port) {
        this.port = port;
        this.monitors = new ConcurrentHashMap<>();
        this.isRunning = new AtomicBoolean(false);
    }

    public void start() throws IOException {
        if (isRunning.compareAndSet(false, true)) {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            
            server.createContext("/monitor/start", new StartMonitorHandler());
            server.createContext("/monitor/stop", new StopMonitorHandler());
            server.createContext("/monitor/status", new StatusHandler());
            
            server.setExecutor(null); // creates a default executor
            server.start();
            
            logger.info("Thread Monitor Server started on port : " + port);
        }
    }

    public void stop() {
        if (isRunning.compareAndSet(true, false)) {
            if (server != null) {
                server.stop(0);
            }
            // Stop all monitors
            monitors.values().forEach(ThreadMonitor::stop);
            monitors.clear();
            logger.info("Thread Monitor Server stopped");
        }
    }

    private class StartMonitorHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                String query = exchange.getRequestURI().getQuery();
                Map<String, String> params = parseQuery(query);
                
                String threadPrefix = params.get("threadPrefix");
                int interval = Integer.parseInt(params.get("interval"));
                int sampleCount = Integer.parseInt(params.get("sampleCount"));
                long threshold = Long.parseLong(params.get("threshold"));

                ThreadMonitor monitor = new ThreadMonitor(threadPrefix, interval, sampleCount, threshold);
                monitor.start();
                monitors.put(threadPrefix, monitor);

                String response = "{\"status\":\"success\",\"message\":\"Monitor started for thread prefix: " + threadPrefix + "\"}";
                sendResponse(exchange, 200, response);
            } catch (Exception e) {
                logger.error("Error starting monitor", e);
                String response = "{\"status\":\"error\",\"message\":\"" + e.getMessage() + "\"}";
                sendResponse(exchange, 400, response);
            }
        }
    }

    private class StopMonitorHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                String query = exchange.getRequestURI().getQuery();
                Map<String, String> params = parseQuery(query);
                String threadPrefix = params.get("threadPrefix");
                
                ThreadMonitor monitor = monitors.remove(threadPrefix);
                if (monitor != null) {
                    monitor.stop();
                    String response = "{\"status\":\"success\",\"message\":\"Monitor stopped for thread prefix: " + threadPrefix + "\"}";
                    sendResponse(exchange, 200, response);
                } else {
                    String response = "{\"status\":\"error\",\"message\":\"No monitor found for thread prefix: " + threadPrefix + "\"}";
                    sendResponse(exchange, 404, response);
                }
            } catch (Exception e) {
                logger.error("Error stopping monitor", e);
                String response = "{\"status\":\"error\",\"message\":\"" + e.getMessage() + "\"}";
                sendResponse(exchange, 400, response);
            }
        }
    }

    private class StatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                StringBuilder json = new StringBuilder("{\"monitors\":[");
                boolean first = true;
                
                for (String prefix : monitors.keySet()) {
                    if (!first) {
                        json.append(",");
                    }
                    json.append("{\"threadPrefix\":\"").append(prefix)
                        .append("\",\"status\":\"running\"}");
                    first = false;
                }
                
                json.append("]}");
                sendResponse(exchange, 200, json.toString());
            } catch (Exception e) {
                logger.error("Error getting status", e);
                String response = "{\"status\":\"error\",\"message\":\"" + e.getMessage() + "\"}";
                sendResponse(exchange, 500, response);
            }
        }
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, response.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    private Map<String, String> parseQuery(String query) {
        Map<String, String> params = new ConcurrentHashMap<>();
        if (query != null) {
            for (String param : query.split("&")) {
                String[] pair = param.split("=");
                if (pair.length == 2) {
                    params.put(pair[0], pair[1]);
                }
            }
        }
        return params;
    }
} 