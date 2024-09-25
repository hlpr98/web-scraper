package com.hlpr98.webscraper.parser;

import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;

import static org.junit.jupiter.api.Assertions.*;

public class ResponseParserFactoryTest {

    @Test
    public void testGetParserSuccess() throws MalformedURLException {
        ResponseParser parser = ResponseParserFactory.getParser("https://www.example.com/sub-path-1/entity-slug12312-uuid-12312-123123.json");
        assertInstanceOf(EntityJsonPageParser.class, parser);

        parser = ResponseParserFactory.getParser("https://www.example.com/sub-path1/subpath-2/product-slug12312.html");
        assertInstanceOf(ProductHTMLPageParser.class, parser);
    }

    @Test
    public void testGetParserFail() throws MalformedURLException {
        assertThrows(IllegalStateException.class,
                () -> ResponseParserFactory.getParser("https://www.example.com/sub-path-1/entity-slug12312.json"));

        assertThrows(MalformedURLException.class,
                () -> ResponseParserFactory.getParser("www.example/sub-path-1/entity-slug12312.json"));
    }
}