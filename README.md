# Web Scraper

This is a Java based web scraper implementation. It is designed with a few functional goals, namely:

* It should handle multiple response formats (ex: JSON, HTML etc.)
* It should scale to scrape millions of URLs.

And a few design goals, namely:

* It should be easily extensible to support new response formats
* It should be easily extensible to support new scrapable entities
* It should handle the complexities of making HTTP requests (ex: retrying, handling errors, rate limits etc.)

## Implementation design

The following are the key aspects of the implementation:

* The scraper is designed as `master` and `worker` semantics.
* The `master` (`WebScraper.class`) is responsible for spawning `workers` (`WebScraperWorker.class`), submitting `urls` for scraping and collecting the results
* **One** `worker` is spawned per `domain` and all paths belonging to that domain are scraped by the same worker. This is done so that:
  * Retries, max parallelism, rate limiting etc could be handled at domain level
  * HTTP clients could be configured at domain level
  * Errors due to origin issues would be confined to a single worker
* The `worker` makes **asynchronous** HTTP request to each Request URLs and **asynchronously** parses the response with the relevant `parser`.
* A `parser` ([ResponseParser.java](src/main/java/com/hlpr98/webscraper/parser/ResponseParser.java)) is responsible for converting the Response body to the POJO.
* Each `parser` would be responsible for a particular URL path and a given POJO.
* The scraped data is converted to the POJO by the `parser`.

### Note on Processing
* The `worker` and the `RetryableHTTPClient` employ only **asynchronous** processing.
* They return `CompletableFuture`s in every step.
* Hence effectively until the `master` ([WebScraper.java](src/main/java/com/hlpr98/webscraper/WebScraper.java)) doesn't make a `join` call, no processing is done. Only a job graph would be created and kept.
* Exception or failure in scraping of a particular URL **doesn't lead to failure of entire task**. The `exception` is captured at the URL level and shown at the end as a failure.



