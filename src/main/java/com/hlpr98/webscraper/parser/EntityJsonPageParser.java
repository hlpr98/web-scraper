package com.hlpr98.webscraper.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.hlpr98.webscraper.model.entities.EntityWithTitle;
import com.hlpr98.webscraper.model.net.Response;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The parser which parses response and returns {@link EntityWithTitle}
 */
@Slf4j
public class EntityJsonPageParser extends JsonResponseParser<EntityWithTitle> {

    // TODO: improve the regex to handle uuids only
    private static final String PATH_MATCHER_PATTERN = ".*/entity-(?<slug>[^-\\.]+)-(?<uuid>[^\\.]+)\\.json$";
    private final Pattern urlPathPattern;

    public EntityJsonPageParser() {
        super(EntityWithTitle.class);
        this.urlPathPattern = Pattern.compile(PATH_MATCHER_PATTERN, Pattern.CASE_INSENSITIVE);
    }

    @Override
    public Pattern pathPattern() {
        return this.urlPathPattern;
    }

    @Override
    protected EntityWithTitle convertValue(Response rawResponse, JsonNode parsedResponse) throws IOException {
        EntityWithTitle instance = this.objectMapper.treeToValue(parsedResponse, EntityWithTitle.class);

        Matcher matcher = this.urlPathPattern.matcher(rawResponse.getUrl().getPath());
        matcher.matches(); // would always match
        instance.setId(matcher.group("uuid"));
        return instance;
    }
}
