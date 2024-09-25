package com.hlpr98.webscraper;

import com.hlpr98.webscraper.model.domain.WebScrapingResult;
import com.hlpr98.webscraper.model.entities.EntityWithTitle;
import joptsimple.internal.Strings;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.verify.VerificationTimes;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@Testcontainers(disabledWithoutDocker = true, parallel = false)
public class WebScraperTest {

    private static final DockerImageName MOCKSERVER_IMAGE =
            DockerImageName.parse("mockserver/mockserver")
                    .withTag("mockserver-" + MockServerClient.class.getPackage().getImplementationVersion());
    @Container
    private final MockServerContainer CONTAINER = new MockServerContainer(MOCKSERVER_IMAGE);
    private final Random rand = new Random();

    @Test
    public void testMainSuccess() throws MalformedURLException {
        String uuid1 = "uuid-12312-123123";
        String uuid2 = "uuid-45645645-165656";
        String slugId1 = "12312";

        String path1 = getEntityPath(slugId1, uuid1);
        String path2 = getProductPath(slugId1);

        URI uri1 = URI.create(CONTAINER.getEndpoint()).resolve(path1);
        URI uri2 = URI.create(CONTAINER.getEndpoint()).resolve(path2);

        String[] args = new String[2];
        args[0] = "--urls";
        args[1] = Strings.join(List.of(uri1.toString(), uri2.toString()), ",");

        try (MockServerClient mockClient = new MockServerClient(CONTAINER.getHost(), CONTAINER.getServerPort())) {
            mockClient.when(request().withMethod("GET").withPath(path1))
                    .respond(response().withBody(getEntityResponse(uuid1)).withStatusCode(200));

            mockClient.when(request().withMethod("GET").withPath(path2))
                    .respond(response().withBody(getProductResponse(slugId1, uuid2)).withStatusCode(200));

            WebScraper.main(args);
        }
    }

    @Test
    public void testMainFail() {
        String uuid1 = "uuid-12312-123123.sdfa";
        String slugId1 = "slug12312";

        String path1 = getEntityPath(slugId1, uuid1);
        String path2 = getProductPath(slugId1);

        URI uri1 = URI.create(CONTAINER.getEndpoint()).resolve(path1);
        URI uri2 = URI.create(CONTAINER.getEndpoint()).resolve(path2);

        String[] args = new String[2];
        args[0] = "--urls";
        args[1] = Strings.join(List.of(uri1.toString(), uri2.toString()), ",");

        assertThrows(IllegalStateException.class, () -> WebScraper.main(args));
    }

    @Test
    public void testScrapeUrlsSuccess() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        List<Pair<String, String>> entityIds = new ArrayList<>();
        List<Pair<String, String>> productIds = new ArrayList<>();
        List<String> urls = new ArrayList<>();

        for (int i = 0; i <= 100; i++) {
            String uuid = String.valueOf(UUID.randomUUID());
            String slugId = "slug" + rand.nextInt(1000000000);
            entityIds.add(new ImmutablePair<>(slugId, uuid));

            String path = getEntityPath(slugId, uuid);
            URI uri = URI.create(CONTAINER.getEndpoint()).resolve(path);
            urls.add(uri.toString());
        }

        for (int i = 0; i <= 100; i++) {
            String uuid = String.valueOf(UUID.randomUUID());
            String slugId = "slug" + rand.nextInt(1000000000);
            productIds.add(new ImmutablePair<>(slugId, uuid));

            String path = getProductPath(slugId);
            URI uri = URI.create(CONTAINER.getEndpoint()).resolve(path);
            urls.add(uri.toString());
        }

        Map<String, WebScrapingResult> result = null;
        try (MockServerClient mockClient = new MockServerClient(CONTAINER.getHost(), CONTAINER.getServerPort())) {
            for (Pair<String, String> entityId : entityIds) {
                String slugId = entityId.getLeft();
                String uuid = entityId.getRight();
                mockClient.when(request().withMethod("GET").withPath(getEntityPath(slugId, uuid)))
                        .respond(response().withBody(getEntityResponse(uuid)).withStatusCode(200));
            }

            for (Pair<String, String> productId : productIds) {
                String slugId = productId.getLeft();
                String uuid = productId.getRight();
                mockClient.when(request().withMethod("GET").withPath(getProductPath(slugId)))
                        .respond(response().withBody(getProductResponse(slugId, uuid)).withStatusCode(200));
            }

            Method method = WebScraper.class.getDeclaredMethod("scrapeURLs", List.class);
            method.setAccessible(true);

            result = (Map<String, WebScrapingResult>) method.invoke(null, urls);
        }

        assertNotNull(result);

        for (Pair<String, String> entityId : entityIds) {
            String slugId = entityId.getLeft();
            String uuid = entityId.getRight();
            String path = getEntityPath(slugId, uuid);
            String url = URI.create(CONTAINER.getEndpoint()).resolve(path).toString();

            assertTrue(result.containsKey(url), "URL: " + url + " not in map");
            assertNull(result.get(url).getException(), "URL: " + url + " ended with exception");

            EntityWithTitle parsedEntity = (EntityWithTitle) result.get(url).getParsedEntity();
            assertNotNull(parsedEntity);
            assertEquals(uuid, parsedEntity.getId());
            assertTrue(parsedEntity.getTitle().contains(uuid), "URL: " + url + " title not parsed correctly");
        }

        for (Pair<String, String> productId : productIds) {
            String slugId = productId.getLeft();
            String uuid = productId.getRight();

            String path = getProductPath(slugId);
            String url = URI.create(CONTAINER.getEndpoint()).resolve(path).toString();

            assertTrue(result.containsKey(url), "URL: " + url + " not in map");
            assertNull(result.get(url).getException(), "URL: " + url + " ended with exception");

            EntityWithTitle parsedEntity = (EntityWithTitle) result.get(url).getParsedEntity();
            assertNotNull(parsedEntity);
            assertEquals(uuid, parsedEntity.getId());
            assertTrue(parsedEntity.getTitle().contains(slugId), "URL: " + url + " title not parsed correctly");
        }
    }

    @Test
    public void testScrapeUrlsWithException() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        String uuid1 = String.valueOf(UUID.randomUUID());
        String uuid2 = String.valueOf(UUID.randomUUID());
        String uuid3 = String.valueOf(UUID.randomUUID());
        String slugId1 = "12312";

        String pathWithEmptyResponse = getEntityPath(slugId1, uuid1);
        String pathWithServerError = getEntityPath(slugId1, uuid2);
        String pathWithTooManyRequests = getProductPath(slugId1);
        String pathWithSuccessfulResponse = getEntityPath(slugId1, uuid3);

        String uriWithEmptyResponse = URI.create(CONTAINER.getEndpoint()).resolve(pathWithEmptyResponse).toString();
        String uriWithTooManyRequests = URI.create(CONTAINER.getEndpoint()).resolve(pathWithTooManyRequests).toString();
        String uriWithServerError = URI.create(CONTAINER.getEndpoint()).resolve(pathWithServerError).toString();
        String uriWithSuccessfulResponse = URI.create(CONTAINER.getEndpoint()).resolve(pathWithSuccessfulResponse).toString();

        List<String> urls = List.of(uriWithEmptyResponse, uriWithTooManyRequests, uriWithSuccessfulResponse, uriWithServerError);

        Map<String, WebScrapingResult> result = null;
        try (MockServerClient mockClient = new MockServerClient(CONTAINER.getHost(), CONTAINER.getServerPort())) {
            mockClient.when(request().withMethod("GET").withPath(pathWithEmptyResponse))
                    .respond(response().withBody("").withStatusCode(404));
            mockClient.when(request().withMethod("GET").withPath(pathWithTooManyRequests))
                    .respond(response().withBody("").withStatusCode(429));
            mockClient.when(request().withMethod("GET").withPath(pathWithServerError))
                    .respond(response().withStatusCode(500));
            mockClient.when(request().withMethod("GET").withPath(pathWithSuccessfulResponse))
                    .respond(response().withBody(getEntityResponse(uuid3)).withStatusCode(200));


            Method method = WebScraper.class.getDeclaredMethod("scrapeURLs", List.class);
            method.setAccessible(true);

            result = (Map<String, WebScrapingResult>) method.invoke(null, urls);

            // uriWithEmptyResponse
            assertTrue(result.containsKey(uriWithEmptyResponse));
            WebScrapingResult result1 = result.get(uriWithEmptyResponse);
            assertNull(result1.getParsedEntity());
            assertNotNull(result1.getException());
            assertInstanceOf(IllegalArgumentException.class, result1.getException().getCause());
            mockClient.verify(request().withPath(pathWithEmptyResponse), VerificationTimes.exactly(1));

            // uriWithTooManyRequests
            assertTrue(result.containsKey(uriWithTooManyRequests));
            result1 = result.get(uriWithTooManyRequests);
            assertNull(result1.getParsedEntity());
            assertNotNull(result1.getException());
            assertInstanceOf(IllegalArgumentException.class, result1.getException().getCause());
            mockClient.verify(request().withPath(pathWithTooManyRequests), VerificationTimes.exactly(1));

            // uriWithServerError
            assertTrue(result.containsKey(uriWithServerError));
            result1 = result.get(uriWithServerError);
            assertNull(result1.getParsedEntity());
            assertNotNull(result1.getException());
            assertInstanceOf(RuntimeException.class, result1.getException().getCause());
            assertTrue(result1.getException().getCause().getMessage().contains("Retries exceeded: status-code=500"));
            mockClient.verify(request().withPath(pathWithServerError), VerificationTimes.exactly(5));

            // uriWithSuccessfulResponse
            assertTrue(result.containsKey(uriWithSuccessfulResponse));
            assertNull(result.get(uriWithSuccessfulResponse).getException());

            EntityWithTitle parsedEntity = (EntityWithTitle) result.get(uriWithSuccessfulResponse).getParsedEntity();
            assertNotNull(parsedEntity);
            assertEquals(uuid3, parsedEntity.getId());
            assertTrue(parsedEntity.getTitle().contains(uuid3));
        }
    }

    @Test
    public void testScrapeUrlsFailure() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        String uriWithMalformedURL = "malformed-url";
        String uriWithNonExistentURL = "https://non-existing";

        Method method = WebScraper.class.getDeclaredMethod("scrapeURLs", List.class);
        method.setAccessible(true);

        InvocationTargetException ex =
                assertThrows(InvocationTargetException.class, () -> method.invoke(null, List.of(uriWithMalformedURL)));
        assertInstanceOf(MalformedURLException.class, ex.getCause());

        ex = assertThrows(InvocationTargetException.class, () -> method.invoke(null, List.of(uriWithNonExistentURL)));
        assertInstanceOf(IllegalStateException.class, ex.getCause());

    }

    private String getEntityPath(String slugId, String uuid) {
        return "/entity-" + slugId + "-" + uuid + ".json";
    }

    private String getProductPath(String slugId) {
        return "/product-" + slugId + ".html";
    }

    private String getEntityResponse(String uuid) {
        return String.format("{\"title\":\"My Title for uuid: %s\"}", uuid);
    }

    private String getProductResponse(String slugId, String uuid) {
        String response = "<html>\n" +
                "\t<head></head>\n" +
                "\t<body>\n" +
                "\t\t<h1 class=\"product-title\" data-id=\"%s\">My Product Title for slug: %s</h1>\n" +
                "\t</body>\n" +
                "</html>";

        return String.format(response, uuid, slugId);
    }
}