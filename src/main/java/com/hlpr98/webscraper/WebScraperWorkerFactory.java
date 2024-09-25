package com.hlpr98.webscraper;

import com.hlpr98.webscraper.util.RetryableHTTPClient;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles creation and provision of scrapping working for a given url.
 * <p>
 * Note: It provides one worker per domain. This is done because:
 * <ul>
 *     <li>HTTP client could be configured at domain level</li>
 *     <li>Retries, max parallelism, rate limiting etc could be handled at domain level</li>
 *     <li>Errors due to origin issues would be confined to a single worker</li>
 *     <li>Etc.</li>
 * </ul>
 * <p>
 * TODO: Add support for domain level parallelism
 */
@Slf4j
public class WebScraperWorkerFactory {

    private static final HTTPClientConfiguration defaultHTTPClientConfiguration = new HTTPClientConfiguration("", 5, Duration.ofSeconds(10));
    private final Map<String, WebScraperWorker> domainVsWorker;
    private final Map<String, HTTPClientConfiguration> domainSpecificHTTPClientConfigs;
    private final HttpClient httpClient;

    public WebScraperWorkerFactory(HttpClient httpClient, HTTPClientConfiguration... domainSpecificConfigs) {
        this.httpClient = httpClient;
        this.domainVsWorker = new HashMap<>();
        this.domainSpecificHTTPClientConfigs = new HashMap<>();
        if (domainSpecificConfigs != null) {
            for (HTTPClientConfiguration configuration : domainSpecificConfigs) {
                this.domainSpecificHTTPClientConfigs.put(configuration.getDomain(), configuration);
            }
        }
    }

    public WebScraperWorker getWorker(String url) throws MalformedURLException {
        String domain = new URL(url).getHost();
        if (domainVsWorker.containsKey(domain)) {
            return domainVsWorker.get(domain);
        }

        HTTPClientConfiguration configuration = defaultHTTPClientConfiguration;
        if (this.domainSpecificHTTPClientConfigs.containsKey(domain)) {
            configuration = this.domainSpecificHTTPClientConfigs.get(domain);
        }
        WebScraperWorker worker = createWorker(configuration);
        this.domainVsWorker.put(domain, worker);
        return worker;
    }

    public Collection<WebScraperWorker> getAllWorkers() {
        return this.domainVsWorker.values();
    }

    private WebScraperWorker createWorker(HTTPClientConfiguration configuration) {
        RetryableHTTPClient<String> client = RetryableHTTPClient.builder(HttpResponse.BodyHandlers.ofString())
                .withMaxAttempts(configuration.getMaxAttempts())
                .withRetryDelay(configuration.getRetryDelay())
                .withRetryOnResponse((resp) -> resp.statusCode() >= 500) // handle TOO MANY REQUESTS
                .withThrowWhenRetryOnResponseExceeded(true)
                .withHttpClient(this.httpClient)
                .build();

        return new WebScraperWorker(client);
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HTTPClientConfiguration {
        // TODO: add validation

        private String domain;
        private int maxAttempts;
        private Duration retryDelay;

    }
}
