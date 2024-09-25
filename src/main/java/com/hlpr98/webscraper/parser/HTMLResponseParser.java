package com.hlpr98.webscraper.parser;

import com.hlpr98.webscraper.model.net.Response;
import com.hlpr98.webscraper.model.net.ResponseType;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.ParseSettings;
import org.jsoup.parser.Parser;

import java.io.IOException;

/**
 * Handles parsing the pages which return HTML type as the response
 * Each specific implementation of this class would handle specific response signatures (i.e. urls)
 * for specific return entity types.
 *
 * @param <T> the parsed entity type
 */
@Slf4j
public abstract class HTMLResponseParser<T> extends ResponseParser<T> {

    protected final Parser parser;

    public HTMLResponseParser(Class<T> tClass) {
        super(tClass);
        this.parser = Parser.htmlParser();
        this.configureParser();
    }

    @Override
    public T parse(Response response) throws IOException {
        Document document = Jsoup.parse(response.getResponse());

        return convertValue(response, document);
    }

    @Override
    public ResponseType responseType() {
        return ResponseType.HTML;
    }

    /**
     * Converts the parsed HTML response to the object
     *
     * @param rawResponse    the raw response
     * @param parsedResponse the parsed HTML response
     * @return the entity
     */
    protected abstract T convertValue(Response rawResponse, Document parsedResponse) throws IOException;

    /**
     * Any parser that needs to add custom configurations can override this method
     */
    protected void configureParser() {
        this.parser.settings(ParseSettings.htmlDefault);
    }
}
