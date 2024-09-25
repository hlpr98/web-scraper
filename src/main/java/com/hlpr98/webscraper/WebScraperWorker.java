package com.hlpr98.webscraper;

import com.hlpr98.webscraper.model.domain.WebScrapingResult;
import com.hlpr98.webscraper.model.net.Response;
import com.hlpr98.webscraper.parser.IResponseParser;
import com.hlpr98.webscraper.util.RetryableHTTPClient;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Each worker handle a set of urls. It makes request to each url and parses the response with the provided parser.
 */
@Slf4j
public class WebScraperWorker implements BiConsumer<String, IResponseParser> {

    private final RetryableHTTPClient<String> client;
    private final Map<String, IResponseParser<?>> urlVsParser;
    private final Map<String, CompletableFuture<WebScrapingResult>> urlVsParsingTask;

    public WebScraperWorker(RetryableHTTPClient<String> client) {
        this.client = client;
        this.urlVsParser = new HashMap<>();
        this.urlVsParsingTask = new HashMap<>();
    }

    @Override
    public void accept(String url, IResponseParser parser) {
        this.urlVsParser.put(url, parser);
        this.urlVsParsingTask.put(url, createTask(url, parser));
    }

    /**
     * Scrapes the urls under its purview asynchronously
     * @return a completable future of Map&lt;url, result&gt;
     */
    public CompletableFuture<Map<String, WebScrapingResult>> scrape() {
        return CompletableFuture.allOf(this.urlVsParsingTask.values().toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    Map<String, WebScrapingResult> urlVsParsedObject = new HashMap<>();
                    this.urlVsParsingTask.forEach((url, task) -> {
                        urlVsParsedObject.put(url, task.join());
                    });
                    return urlVsParsedObject;
                });
    }

    // TODO: handle other HTTP methods
    private CompletableFuture<WebScrapingResult> createTask(String url, IResponseParser<?> parser) {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        return this.client.execute(req)
                .thenApply((res) -> {
                    try {
                        Response response = Response.fromHTTPResponse(res);
                        WebScrapingResult result = WebScrapingResult.builder()
                                .parsedEntity(parser.parse(response))
                                .build();

                        return CompletableFuture.completedFuture(result);
                    } catch (MalformedURLException ex) {
                        throw new RuntimeException("Exception while reading response body of " + url, ex);
                    } catch (IOException ex) {
                        log.error("Exception while parsing response of {}", url, ex);
                        throw new RuntimeException("Exception while parsing response of " + url, ex);
                    }
                })
                .exceptionally(ex -> {
                    WebScrapingResult result = WebScrapingResult.builder().exception(ex).build();
                    return CompletableFuture.completedFuture(result);
                })
                .thenCompose(Function.identity());
    }
}
