package com.hlpr98.webscraper;

import com.hlpr98.webscraper.model.domain.WebScrapingResult;
import com.hlpr98.webscraper.parser.ResponseParser;
import com.hlpr98.webscraper.parser.ResponseParserFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.requireNonNull;

/**
 * The entry point of this application.
 * <p>
 * TODO: Convert this into a service or a command line tool
 */
@Slf4j
public class WebScraper {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public static void main(String[] args) throws MalformedURLException {
        if (args == null) {
            throw new IllegalArgumentException("Require filename or an url");
        }

        Options options = new Options();
        options.addOption("u", "urls", true, "List of urls to parse");
        options.addOption("f", "filepath", true, "File containing a list of urls to parse");

        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options, args);
            String urls = cmd.getOptionValue("urls", "");
            String filepath = cmd.getOptionValue("filepath", "");

            if (urls.isEmpty() && filepath.isEmpty()) {
                throw new IllegalArgumentException("Require filename or an url");
            }

            List<String> urlList = null;
            if (!urls.isEmpty()) {
                urlList = List.of(urls.split(","));
            } else {
                String path = requireNonNull(WebScraper.class.getResource(filepath)).getPath();
                String urlsInFile = Files.readString(Path.of(path), StandardCharsets.UTF_8);

                urlList = List.of(urlsInFile.split("\n"));
            }

            Map<String, WebScrapingResult> urlVsScrapedObject = scrapeURLs(urlList);
            persist(urlVsScrapedObject);
        } catch (ParseException e) {
            log.error("Exception while parsing arguments", e);
            System.exit(1);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Map<String, WebScrapingResult> scrapeURLs(List<String> urls) throws MalformedURLException {
        if (urls == null || urls.isEmpty()) {
            return null;
        }
        WebScraperWorkerFactory workerFactory = new WebScraperWorkerFactory(HTTP_CLIENT);

        for (String url : urls) {
            WebScraperWorker worker = workerFactory.getWorker(url);
            ResponseParser parser = ResponseParserFactory.getParser(url);
            worker.accept(url, parser);
        }

        List<CompletableFuture<Map<String, WebScrapingResult>>> futures = new ArrayList<>();
        workerFactory.getAllWorkers().forEach(w -> futures.add(w.scrape()));

        Map<String, WebScrapingResult> result = new ConcurrentHashMap<>();
        CompletableFuture
                .allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    futures.forEach(f -> {
                        Map<String, WebScrapingResult> urlVsObject = f.join();
                        result.putAll(urlVsObject);
                    });
                    return null;
                })
                .join(); // blocking call

        return result;
    }

    /**
     * Handle persistence mechanisms
     *
     * @param urlVsScrapedResult
     */
    private static void persist(Map<String, WebScrapingResult> urlVsScrapedResult) {
        System.out.println("Scraped data");

        for (Map.Entry<String, WebScrapingResult> resultSet : urlVsScrapedResult.entrySet()) {
            String url = resultSet.getKey();
            WebScrapingResult result = resultSet.getValue();

            if (result.getException() == null) {
                System.out.println("URL: " + url + " Entity: " + result.getParsedEntity());
            } else {
                System.out.println("URL: " + url + " Error: " + result.getException());
            }
        }
    }
}
