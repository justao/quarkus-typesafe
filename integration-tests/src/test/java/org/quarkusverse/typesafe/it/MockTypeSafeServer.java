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
import java.util.Map;
import java.util.Queue;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpServer;

/**
 * A stand-in for the Jev API, on a port the operating system picks.
 *
 * <p>
 * The tests that configure an API key point {@code quarkus.typesafe.base-url} at this
 * server instead of the real one: an assertion about which questions, model and state the
 * extension puts on the wire is only worth making against something that records them, and
 * the real API costs tokens and needs a key.
 *
 * <p>
 * A request to {@code /v1/systemone} is answered with the next scripted response, or with a
 * default body carrying one noul answer named {@code probe}. Scripting is what makes the
 * interesting cases expressible — several answers in one body, an answer of a kind the
 * method did not declare, an error status.
 */
final class MockTypeSafeServer {

	static final String REQUEST_ID = "req_0123456789";

	private static final String SYSTEM_ONE_PATH = "/v1/systemone";

	private static final String MODELS_PATH = "/v1/models";

	/** The levels the {@link #score(double, int)} helper builds a legend from. */
	private static final String[] DEFAULT_LEVELS = { "Calm", "Frustrated", "Very angry" };

	private static final String SYSTEM_ONE_BODY = """
			{"model":"jev-1.13.0","answers":{"probe":{"type":"noul","noul":0.5}},"usage":{"input_tokens":10,"output_tokens":2}}""";

	private static final String MODELS_BODY = """
			{"models":[{"name":"jev-latest","description":"The latest iteration of TypeSafe's System One Model: Jev","release_date":"2026-09-10T18:38:01.391457+00:00"},
			           {"name":"jev-preview","description":"A preview version of `jev-latest`: should be better in most ways","release_date":"2026-09-10T18:39:06.057655+00:00"}]}""";

	private static MockTypeSafeServer instance;

	private final HttpServer server;

	private final List<String> requestBodies = new CopyOnWriteArrayList<>();

	private final List<String> requestPaths = new CopyOnWriteArrayList<>();

	private final List<String> authorizationHeaders = new CopyOnWriteArrayList<>();

	private final Queue<Response> scripted = new ConcurrentLinkedQueue<>();

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
			server.createContext(SYSTEM_ONE_PATH, exchange -> mock.respondSystemOne(exchange));
			server.createContext(MODELS_PATH, exchange -> mock.respond(exchange, MODELS_BODY));
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
	 * @return the path of every request received since the last {@link #reset()}
	 */
	List<String> requestPaths() {
		return List.copyOf(this.requestPaths);
	}

	/**
	 * @return how many requests arrived since the last {@link #reset()}
	 */
	int requestCount() {
		return this.requestBodies.size();
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
	 * Queues the answer the next {@code /v1/systemone} request gets.
	 * @param body the response body
	 */
	void respondNext(String body) {
		this.scripted.add(new Response(200, body));
	}

	/**
	 * Queues an error the next {@code /v1/systemone} request gets.
	 * @param status the status code
	 * @param body the response body
	 */
	void respondNext(int status, String body) {
		this.scripted.add(new Response(status, body));
	}

	/**
	 * Forgets the recorded requests and the queued answers, so a test asserts about its own
	 * call and not about whatever ran before it.
	 */
	void reset() {
		this.requestBodies.clear();
		this.requestPaths.clear();
		this.authorizationHeaders.clear();
		this.scripted.clear();
	}

	private void respondSystemOne(HttpExchange exchange) throws IOException {
		Response next = this.scripted.poll();
		respond(exchange, next == null ? new Response(200, SYSTEM_ONE_BODY) : next);
	}

	private void respond(HttpExchange exchange, String body) throws IOException {
		respond(exchange, new Response(200, body));
	}

	private void respond(HttpExchange exchange, Response response) throws IOException {
		this.requestBodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
		this.requestPaths.add(exchange.getRequestURI().getPath());
		String authorization = exchange.getRequestHeaders().getFirst("Authorization");
		this.authorizationHeaders.add(authorization != null ? authorization : "");

		byte[] payload = response.body().getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().add("Content-Type", "application/json");
		// The client lifts the request id off this header onto the response object.
		exchange.getResponseHeaders().add("x-typesafe-request-id", REQUEST_ID);
		exchange.sendResponseHeaders(response.status(), payload.length);
		try (OutputStream out = exchange.getResponseBody()) {
			out.write(payload);
		}
	}

	/**
	 * @param name the answer's key
	 * @param answer the answer's JSON
	 * @return a response body carrying that one answer
	 */
	static String body(String name, String answer) {
		return body(Map.of(name, answer));
	}

	/**
	 * @param answers the answers, keyed by question name
	 * @return a response body carrying them
	 */
	static String body(Map<String, String> answers) {
		StringJoiner joined = new StringJoiner(",", "\"answers\":{", "}");
		answers.forEach((name, answer) -> joined.add("\"" + name + "\":" + answer));
		return "{\"model\":\"jev-1.13.0\"," + joined + ",\"usage\":{\"input_tokens\":10,\"output_tokens\":2}}";
	}

	/**
	 * @param value the truth value
	 * @return a noul answer
	 */
	static String noul(double value) {
		return "{\"type\":\"noul\",\"noul\":" + value + "}";
	}

	/**
	 * @param label the selected option
	 * @return a choice answer
	 */
	static String choice(String label) {
		return "{\"type\":\"choice\",\"choice\":\"" + label + "\",\"probabilities\":{\"" + label
				+ "\":0.9},\"confidence\":0.9}";
	}

	/**
	 * A score answer over the three levels {@link #DEFAULT_LEVELS} names, with the highest
	 * probability — and so the nearest level — on {@code nearestLevel}.
	 * @param value the probability-weighted score
	 * @param nearestLevel the level carrying the most probability
	 * @return a score answer
	 */
	static String score(double value, int nearestLevel) {
		StringJoiner probabilities = new StringJoiner(",", "{", "}");
		StringJoiner legend = new StringJoiner(",", "{", "}");
		for (int level = 0; level < DEFAULT_LEVELS.length; level++) {
			probabilities.add("\"" + level + "\":" + (level == nearestLevel ? "0.65" : "0.05"));
			legend.add("\"" + level + "\":\"" + DEFAULT_LEVELS[level] + "\"");
		}
		return "{\"type\":\"score\",\"score\":" + value + ",\"legend\":" + legend + ",\"probabilities\":" + probabilities
				+ ",\"confidence\":0.78}";
	}

	/** One scripted response. */
	private record Response(int status, String body) {
	}

}
