/*
 * Copyright 2026 - 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.quarkusverse.typesafe.it;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * A stand-in for the Jev API, on a port the operating system picks.
 *
 * <p>
 * The tests that configure an API key point {@code quarkus.typesafe.base-url} at this
 * server instead of the real one: an assertion about which headers, model and bearer token
 * the injected client puts on the wire is only worth making against something that records
 * them, and the real API costs tokens and needs a key.
 *
 * <p>
 * The response bodies are the documented ones — the same literals the reference SDK asserts
 * against — so a parsing failure here is a real failure and not an artifact of the fixture.
 */
final class MockTypeSafeServer {

	static final String REQUEST_ID = "req_0123456789";

	private static final String SYSTEM_ONE_BODY = """
			{"model":"jev-1.13.0","answers":{"probe":{"type":"noul","noul":0.5}},"usage":{"input_tokens":10,"output_tokens":2}}""";

	private static final String MODELS_BODY = """
			{"models":[{"name":"jev-latest","description":"The latest iteration of TypeSafe's System One Model: Jev","release_date":"2026-09-10T18:38:01.391457+00:00"},
			           {"name":"jev-preview","description":"A preview version of `jev-latest`: should be better in most ways","release_date":"2026-09-10T18:39:06.057655+00:00"}]}""";

	private static MockTypeSafeServer instance;

	private final HttpServer server;

	private final List<String> requestBodies = new CopyOnWriteArrayList<>();

	private final List<String> authorizationHeaders = new CopyOnWriteArrayList<>();

	private MockTypeSafeServer(HttpServer server) {
		this.server = server;
	}

	/**
	 * The server the profiles point the client at. One instance per test JVM: the profiles
	 * are instantiated before the application starts, and the tests then assert against the
	 * requests it recorded.
	 * @return the started server
	 */
	static synchronized MockTypeSafeServer instance() {
		if (instance == null) {
			instance = start();
		}
		return instance;
	}

	private static MockTypeSafeServer start() {
		try {
			HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
			MockTypeSafeServer mock = new MockTypeSafeServer(server);
			server.createContext("/v1/systemone", exchange -> mock.respond(exchange, SYSTEM_ONE_BODY));
			server.createContext("/v1/models", exchange -> mock.respond(exchange, MODELS_BODY));
			ExecutorService executor = Executors.newCachedThreadPool(runnable -> {
				Thread thread = new Thread(runnable, "mock-jev");
				// Daemon threads, and a shutdown hook, so the test JVM can still exit.
				thread.setDaemon(true);
				return thread;
			});
			server.setExecutor(executor);
			server.start();
			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				server.stop(0);
				executor.shutdownNow();
			}, "mock-jev-stop"));
			return mock;
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Could not start the mock Jev API", ex);
		}
	}

	/**
	 * @return the URL the client under test has to be pointed at
	 */
	String baseUrl() {
		return "http://localhost:" + this.server.getAddress().getPort();
	}

	/**
	 * @return every request body received since the last {@link #reset()}, oldest first
	 */
	List<String> requestBodies() {
		return List.copyOf(this.requestBodies);
	}

	/**
	 * @return the body of the most recent request, or an empty string when none arrived
	 */
	String lastRequestBody() {
		return this.requestBodies.isEmpty() ? "" : this.requestBodies.get(this.requestBodies.size() - 1);
	}

	/**
	 * @return the {@code Authorization} header of every request received since the last
	 * {@link #reset()}, oldest first
	 */
	List<String> authorizationHeaders() {
		return List.copyOf(this.authorizationHeaders);
	}

	/**
	 * Forgets the recorded requests, so a test asserts about its own call and not about
	 * whatever ran before it.
	 */
	void reset() {
		this.requestBodies.clear();
		this.authorizationHeaders.clear();
	}

	private void respond(HttpExchange exchange, String body) throws IOException {
		this.requestBodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
		String authorization = exchange.getRequestHeaders().getFirst("Authorization");
		this.authorizationHeaders.add(authorization != null ? authorization : "");

		byte[] payload = body.getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().add("Content-Type", "application/json");
		// The client lifts the request id off this header onto the response object.
		exchange.getResponseHeaders().add("x-typesafe-request-id", REQUEST_ID);
		exchange.sendResponseHeaders(200, payload.length);
		try (OutputStream out = exchange.getResponseBody()) {
			out.write(payload);
		}
	}

}
