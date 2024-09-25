package com.hlpr98.webscraper.parser;

import com.hlpr98.webscraper.model.entities.EntityWithTitle;
import com.hlpr98.webscraper.model.net.Response;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;


public class EntityAsJsonResponseParserTest {

    private final EntityJsonPageParser parser = new EntityJsonPageParser();

    @Test
    public void testHandlesURLWithDomain() throws MalformedURLException {
        boolean result = parser.handlesURL("https://www.example.com/entity-slug12312-uuid-12312-123123.json");
        assertTrue(result);

        result = parser.handlesURL("https://www.example.com/sub-path-1/entity-slug12312-uuid-12312-123123.json");
        assertTrue(result);

        result = parser.handlesURL("https://www.example.com/entity-slug12312.json");
        assertFalse(result);

        result = parser.handlesURL("https://www.example.com/entity-slug12312-uuid-12312-123123.json/blah");
        assertFalse(result);
    }

    @Test
    public void testHandlesURLWithoutDomain() {
        Assert.assertThrows(MalformedURLException.class,
                () -> parser.handlesURL("/entity-slug12312-uuid-12312-123123.json"));
    }

    @Test
    public void testParse() throws IOException {
        String response = "{\"title\":\"My Title\"}";

        EntityWithTitle entity = parser.parse(
                Response.builder()
                        .url(new URL("https://www.example.com/sub-path-1/entity-slug12312-uuid-12312-123123.json"))
                        .response(response)
                        .build()
        );

        assertNotNull(entity);
        assertEquals("uuid-12312-123123", entity.getId());
        assertEquals("My Title", entity.getTitle());
    }
}