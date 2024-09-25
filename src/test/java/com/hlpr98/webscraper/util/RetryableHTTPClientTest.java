package com.hlpr98.webscraper.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockserver.client.MockServerClient;
import org.mockserver.matchers.Times;
import org.mockserver.verify.VerificationTimes;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import javax.net.ssl.SSLHandshakeException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@Testcontainers(disabledWithoutDocker = true, parallel = false)
public class RetryableHTTPClientTest {

    private static final String PATH = "/rest/api/latest";
    private static final DockerImageName MOCKSERVER_IMAGE =
            DockerImageName.parse("mockserver/mockserver")
                    .withTag("mockserver-" + MockServerClient.class.getPackage().getImplementationVersion());
    @Container
    private final MockServerContainer CONTAINER = new MockServerContainer(MOCKSERVER_IMAGE);

    private HttpRequest getRequest(MockServerContainer container) {
        return HttpRequest.newBuilder()
                .uri(URI.create(container.getEndpoint()).resolve(PATH))
                .GET()
                .build();
    }

    @Test
    public void singleSuccessInvocationWithoutBody() throws Exception {
        try (MockServerClient mockClient = new MockServerClient(CONTAINER.getHost(), CONTAINER.getServerPort());) {
            mockClient.when(request().withMethod("GET").withPath(PATH))
                    .respond(response().withStatusCode(200));

            HttpResponse<Void> response = RetryableHTTPClient.builder()
                    .build()
                    .execute(getRequest(CONTAINER))
                    .get(1, TimeUnit.SECONDS);

            assertEquals(200, response.statusCode());
        }
    }

    @Test
    public void singleSuccessInvocationWithBody() throws Exception {
        try (MockServerClient mockClient = new MockServerClient(CONTAINER.getHost(), CONTAINER.getServerPort())) {

            mockClient.when(request().withMethod("GET").withPath(PATH))
                    .respond(response().withStatusCode(200).withBody("Body"));

            HttpResponse<String> response =
                    RetryableHTTPClient.builder(HttpResponse.BodyHandlers.ofString())
                            .build()
                            .execute(getRequest(CONTAINER))
                            .get(1, TimeUnit.SECONDS);

            assertEquals(200, response.statusCode());
            assertEquals("Body", response.body());
        }
    }

    @Test
    public void successfulWithRetryOnResponseWithBody() throws Exception {
        try (MockServerClient mockClient = new MockServerClient(CONTAINER.getHost(), CONTAINER.getServerPort())) {

            mockClient.when(request().withMethod("GET").withPath(PATH))
                    .respond(response().withStatusCode(200).withBody("Body"));

            HttpResponse<String> response =
                    RetryableHTTPClient.builder(HttpResponse.BodyHandlers.ofString())
                            .withRetryOnResponse(resp -> resp.body().equals("Weird Body"))
                            .build()
                            .execute(getRequest(CONTAINER))
                            .get(5, TimeUnit.SECONDS);

            assertEquals(200, response.statusCode());
            assertEquals("Body", response.body());
        }
    }

    @Test
    public void successfulRetry() throws Exception {
        try (MockServerClient mockClient = new MockServerClient(CONTAINER.getHost(), CONTAINER.getServerPort())) {

            mockClient.when(request().withMethod("GET").withPath(PATH), Times.exactly(2))
                    .respond(response().withStatusCode(500));

            mockClient.when(request().withMethod("GET").withPath(PATH))
                    .respond(response().withStatusCode(200).withBody("Body"));

            HttpResponse<String> response =
                    RetryableHTTPClient.builder(HttpResponse.BodyHandlers.ofString())
                            .withMaxAttempts(3)
                            .withRetryDelay(Duration.ofMillis(100))
                            .build()
                            .execute(getRequest(CONTAINER))
                            .get(10, TimeUnit.SECONDS);

            assertEquals(200, response.statusCode());
            assertEquals("Body", response.body());
            mockClient.verify(request().withPath(PATH), VerificationTimes.exactly(3));
        }

    }

    @Test
    public void attemptsExceededOnResponseThrowException() {
        try (MockServerClient mockClient = new MockServerClient(CONTAINER.getHost(), CONTAINER.getServerPort())) {

            mockClient.when(request().withMethod("GET").withPath(PATH))
                    .respond(response().withStatusCode(500));

            Executable executable =
                    () -> RetryableHTTPClient.builder(HttpResponse.BodyHandlers.ofString())
                            .withMaxAttempts(3)
                            .withRetryDelay(Duration.ofMillis(100))
                            .withThrowWhenRetryOnResponseExceeded(true)
                            .build()
                            .execute(getRequest(CONTAINER))
                            .get(1, TimeUnit.SECONDS);

            ExecutionException ex = assertThrows(ExecutionException.class, executable);
            assertEquals("java.lang.RuntimeException: Retries exceeded: status-code=500", ex.getMessage());
        }
    }

    @Test
    public void attemptsExceededOnResponseReturnResponse() throws Exception {
        try (MockServerClient mockClient = new MockServerClient(CONTAINER.getHost(), CONTAINER.getServerPort())) {

            mockClient.when(request().withMethod("GET").withPath(PATH))
                    .respond(response().withStatusCode(500));

            HttpResponse<String> response = RetryableHTTPClient.builder( HttpResponse.BodyHandlers.ofString())
                    .withMaxAttempts(3)
                    .withRetryDelay(Duration.ofMillis(100))
                    .withThrowWhenRetryOnResponseExceeded(false)
                    .build()
                    .execute(getRequest(CONTAINER))
                    .get(1, TimeUnit.SECONDS);

            assertEquals(500, response.statusCode());
        }
    }

    @Test
    public void attemptsExceededOnIOException() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://non-existing")).build();

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        Executable executable =
                () -> RetryableHTTPClient.builder()
                        .withHttpClient(httpClient)
                        .withMaxAttempts(3)
                        .withRetryDelay(Duration.ofMillis(100))
                        .build()
                        .execute(request)
                        .get(10, TimeUnit.SECONDS);

        ExecutionException ex = assertThrows(ExecutionException.class, executable);
        assertEquals(SSLHandshakeException.class, ex.getCause().getClass());
    }
}
