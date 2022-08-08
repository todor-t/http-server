package com.cycos.i3log.server;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.joining;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import com.sun.net.httpserver.HttpExchange; // NOSONAR
import com.sun.net.httpserver.HttpServer; // NOSONAR

public class Server implements AutoCloseable {
	private static final Logger LOG = Logger.getLogger(Server.class.getName());
	private final HttpServer httpServer;
	private final ExecutorService executor;

	public Server(String hostname, int port) throws IOException {
		InetSocketAddress address = new InetSocketAddress(hostname, port);
		httpServer = HttpServer.create(address, 0);
		executor = Executors.newFixedThreadPool(10);
		httpServer.setExecutor(executor);
		httpServer.createContext("/", this::handle9);
	}

	public void start() {
		httpServer.start();
		LOG.info(() -> "HTTP Server started on " + httpServer.getAddress());
	}

	public void handle(HttpExchange exchange) throws IOException {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), UTF_8)); exchange) {
			String body = reader.lines().collect(joining("\n"));
			LOG.info(body);

			// Send response
			exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, -1);
		}
	}

	public void handle9(HttpExchange exchange) throws IOException { // Java 9 or later
		try (InputStream stream = exchange.getRequestBody(); exchange) {
			String body = new String(stream.readAllBytes(), UTF_8);
			LOG.info(body);

			// Send response
			exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, -1);
		}
	}

	@Override
	public void close() {
		try {
			httpServer.stop(5);
			executor.shutdownNow();
			executor.awaitTermination(5, SECONDS);
			LOG.info(() -> "HTTP Server stopped.");
		} catch (InterruptedException ex) {
			LOG.warning(() -> "HTTP Server stopped with exception: " + ex);
			Thread.currentThread().interrupt();
		} catch (Exception ex) {
			LOG.warning(() -> "HTTP Server stopped with exception: " + ex);
		}
	}

	public static void main(String[] args) {
		try (Server server = new Server("localhost", 5184)) {
			server.start();
			try (Scanner scanner = new Scanner(System.in)) {
				while(!Objects.equals(scanner.nextLine(), "quit")) {
					LOG.info(() -> "Enter 'quit' to exit.");
				}
				LOG.info(() -> "Stopping HTTP server...");
			}
		} catch (Exception ex) {
			LOG.warning(ex::toString);
		}
	}
}
