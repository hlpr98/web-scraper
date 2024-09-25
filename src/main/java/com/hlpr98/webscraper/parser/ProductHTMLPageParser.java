package com.hlpr98.webscraper.parser;

import com.hlpr98.webscraper.model.entities.EntityWithTitle;
import com.hlpr98.webscraper.model.net.Response;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * The parser which parses html response and returns {@link EntityWithTitle}
 */
@Slf4j
public class ProductHTMLPageParser extends HTMLResponseParser<EntityWithTitle> {

    private static final String PATH_MATCHER_PATTERN = ".*/product-(?<slug>[^\\.]+)\\.html";
    private final Pattern urlPathPattern;

    public ProductHTMLPageParser() {
        super(EntityWithTitle.class);
        this.urlPathPattern = Pattern.compile(PATH_MATCHER_PATTERN, Pattern.CASE_INSENSITIVE);
    }

    @Override
    public Pattern pathPattern() {
        return this.urlPathPattern;
    }

    @Override
    protected EntityWithTitle convertValue(Response rawResponse, Document parsedResponse) throws IOException {
        Element h1Element = parsedResponse.select("h1.product-title").first();
        if (h1Element == null) {
            return null;
        }

        String id = h1Element.attr("data-id");
        String title = h1Element.text();

        return EntityWithTitle.builder()
                .id(id)
                .title(title)
                .build();
    }

}
