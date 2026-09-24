# quarkus-typesafe

A Quarkus library that wires up the [TypeSafe AI](https://docs.typesafe.ai) System One (Jev)
client: it binds `quarkus.typesafe.*`, contributes an injectable `TypeSafeClient` and gets
out of the way when you would rather build the client yourself.

It is the Quarkus counterpart of the reference project's `spring-ai-starter-typesafe`
module — same keys one prefix deeper, same defaults, same conditional behaviour — built the
way a Quarkus extension is built rather than translated line by line.

```java
@ApplicationScoped
public class TriageService {

    @Inject
    TypeSafeClient jev;

    public String department(String message) {
        SystemOneResponse response = this.jev.systemOne(message,
                Map.of("urgent", Noul.of("Does this convey urgency?"),
                       "team", Choice.builder()
                               .instructions("Which team should handle this?")
                               .option("billing", "Payments, invoicing, refunds")
                               .option("technical", "Bugs, outages, integrations")
                               .build()));
        return response.choiceValue("team");
    }
}
```

## Modules

| Module | Artifact | What it is |
|---|---|---|
| `runtime/` | `org.quarkusverse:quarkus-typesafe` | The configuration mapping, the CDI producers, the extension descriptor. This is the dependency an application declares. |
| `deployment/` | `org.quarkusverse:quarkus-typesafe-deployment` | The build steps: what Quarkus runs while augmenting an application. Never declared by an application; it is pulled in through the runtime descriptor. |
| `integration-tests/` | — | An application that consumes the extension, and the tests that prove the beans appear, are configured and reach the wire. |

Project names are the Maven artifact ids rather than the directory names (see
`settings.gradle`), because the runtime descriptor names the deployment module by Maven
coordinates.

## Prerequisites

* **JDK 17 or newer.** The reference `typesafe-java-sdk` targets 17 and so does this library.
* **The `typesafe-java-sdk` in your local Maven repository.** It has not been released yet, so
  it is built from the reference project, which this repository expects at
  `spring-ai-typesafe/`:

  ```shell
  git clone https://github.com/spring-ai-community/spring-ai-typesafe.git
  ./gradlew installTypeSafeSdk
  ```

  `installTypeSafeSdk` drives that project's own Maven wrapper and installs
  `typesafe-java-sdk` only. Every module here resolves it from `mavenLocal()`. The version is
  the `typesafeSdkVersion` property in `gradle.properties`.

## Quick start

```groovy
dependencies {
    implementation 'org.quarkusverse:quarkus-typesafe:0.1.0-SNAPSHOT'
}
```

```properties
quarkus.typesafe.api-key=${TYPESAFE_API_KEY}
```

That is the whole setup. `TypeSafeClient` and `TypeSafeEndpoints` are now beans.

## Configuration

| Property | Default | Description |
|---|---|---|
| `quarkus.typesafe.api-key` | — | The API key. **No client is created without it.** |
| `quarkus.typesafe.base-url` | `https://api.typesafe.ai` | The API root. |
| `quarkus.typesafe.model` | `jev-latest` | Applied to requests that do not name a model. |
| `quarkus.typesafe.timeout` | `10s` | Per-attempt HTTP timeout. |
| `quarkus.typesafe.retry.max-retries` | `2` | Retries after the initial attempt. `0` disables retrying. |
| `quarkus.typesafe.retry.initial-backoff` | `500ms` | First delay, doubled on each subsequent attempt. |
| `quarkus.typesafe.retry.max-backoff` | `5s` | Upper bound on a single delay. |
| `quarkus.typesafe.retry.jitter` | `0.25` | Fraction of each delay randomly subtracted, `0`–`1`. |
| `quarkus.typesafe.retry.statuses` | `408,429` | Status codes retried on top of every 5xx, which is always retried. |
| `quarkus.typesafe.retry.respect-retry-after` | `true` | A server-stated wait overrides the computed backoff. |
| `quarkus.typesafe.retry.retry-connection-errors` | `true` | Retry failures that never got an HTTP response. |
| `quarkus.typesafe.retry.total-timeout` | `30s` | Budget for a whole call including waits. `0s` removes the budget. |

Durations accept the Quarkus forms — `500ms`, `10s`, `2m` — as well as ISO-8601 (`PT10S`).
Every key is run-time configuration: an environment variable, a system property or a mounted
secret supplied at startup is read then, and does not have to exist while the application is
being built.

### Environment variables

The plain-Java client falls back to `TYPESAFE_API_KEY`, `TYPESAFE_BASE_URL` and
`TYPESAFE_DEFAULT_MODEL` when built without explicit values. Here, `base-url` and `model`
always have property defaults and are always passed explicitly, so **only the key reaches the
client from the environment** — and only through the placeholder you write yourself, exactly
as under Boot.

## Conditional wiring

The client bean is produced only when something asks for it, and it needs a key:

* an application that configures no key and injects no client **starts normally**;
* an application that injects one without a key fails at that injection point with
  `No API key configured. Set quarkus.typesafe.api-key.` rather than handing out a client
  whose every call would come back `401`;
* declaring a `TypeSafeClient` bean of your own makes the extension's producer step aside —
  it is a `@DefaultBean`, which is what `@ConditionalOnMissingBean` means here.

To guard a bean of your own the way Boot's `@ConditionalOnBean` does, inject
`Instance<TypeSafeClient>` and check `isResolvable()`, or take the key as a
`quarkus.typesafe.api-key` presence check of your own.

## Endpoints bean

`TypeSafeEndpoints` carries the paths in use, for a health check or for observability:

```java
@Inject
TypeSafeEndpoints endpoints;   // endpoints.systemOnePath(), endpoints.modelsPath()
```

## Native images

`quarkus.typesafe.api-key` is read at run time, so a native image built once can be deployed
to any environment. The extension registers the SDK's JSON model for reflection in the build
step, which is what Jackson needs in a native image; `TypeSafeClient` itself is plain JDK
HTTP through Spring's `RestClient`, with no dynamic proxies.

## How this differs from the Boot starter

| Reference starter | Here | Why |
|---|---|---|
| `spring.ai.typesafe.*` | `quarkus.typesafe.*` | Prefix follows the platform. |
| `@ConfigurationProperties` | `@ConfigMapping` | SmallRye Config binds and documents the same keys. |
| `@ConditionalOnProperty` + `@ConditionalOnMissingBean` | `@DefaultBean` + a run-time key check | A run-time property cannot gate bean registration at build time, so the key is checked when the client is built. |
| `RestClient.Builder` taken from the context | the SDK builds its own transport | There is no Spring context to take a builder from; the SDK's `java.net.http` transport is used, and `quarkus.typesafe.timeout` is applied to it. To supply your own transport, build the client yourself — the extension then steps aside. |
| `additional-spring-configuration-metadata.json` | javadoc on `TypeSafeConfig` | Quarkus generates the configuration reference from the mapping. |
| `AutoConfiguration.imports` | `META-INF/quarkus-extension.properties` | The descriptor that links the runtime artifact to its build steps. |

## Building and testing

```shell
./gradlew installTypeSafeSdk   # once: the SDK is not on Maven Central yet
./gradlew build                # compiles all three modules and runs the tests
./gradlew :integration-tests:test
```

The tests boot an application with `@QuarkusTest`; each profile starts its own, so the suite
is slower than a plain unit test run and covers more. What they assert:

* `TypeSafeClientMissingApiKeyTest` — an application with no key starts, and asking for the
  client says which property to set.
* `TypeSafeClientConfiguredTest` — every property binds, and the client it produced sends the
  configured model, the bearer token and the request id back from a mock Jev API.
* `TypeSafeClientDefaultsTest` — with nothing but a key, the retry policy is
  `RetryPolicy.defaults()` exactly, not merely similar.
* `TypeSafeClientBacksOffTest` — an application-defined client wins.
* `ExtensionDescriptorTest` — the runtime artifact still names this build's deployment module.
* `TypeSafeLiveApiIT` — the same wiring against the real Jev API. Skipped unless
  `TYPESAFE_API_KEY` is exported; it costs tokens.

### Publishing locally

`build` publishes both artifacts to the local Maven repository before the integration tests
run, because the Quarkus bootstrap resolves the deployment artifact through Maven
coordinates even when the runtime half is a project dependency:

```shell
./gradlew publishToMavenLocal
```

## See also

* `spring-ai-typesafe/` — the reference project this library is modelled on, including
  `spring-ai-starter-typesafe` and the SDK it wires up.
