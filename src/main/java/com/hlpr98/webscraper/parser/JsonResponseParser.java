package com.hlpr98.webscraper.parser;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hlpr98.webscraper.model.net.Response;
import com.hlpr98.webscraper.model.net.ResponseType;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * Handles parsing the pages which return Json type as the response
 * Each specific implementation of this class would handle specific response signatures (i.e. urls)
 * for specific return entity types.
 *
 * @param <T> the parsed entity type
 */
@Slf4j
public abstract class JsonResponseParser<T> extends ResponseParser<T> {

    protected final ObjectMapper objectMapper;

    public JsonResponseParser(Class<T> tClass) {
        super(tClass);
        this.objectMapper = new ObjectMapper();
        this.configureObjectMapper();
    }

    @Override
    public T parse(Response response) throws IOException {
        JsonNode parsed = this.objectMapper.readTree(response.getResponse());

        return convertValue(response, parsed);
    }

    @Override
    public ResponseType responseType() {
        return ResponseType.JSON;
    }

    /**
     * Converts the parsed json response to the object
     *
     * @param rawResponse    the raw response
     * @param parsedResponse the parsed json response
     * @return the entity
     */
    protected abstract T convertValue(Response rawResponse, JsonNode parsedResponse) throws IOException;

    /**
     * Any parser that needs to add custom configurations like NamedTypes, can override this method
     */
    protected void configureObjectMapper() {
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
}
