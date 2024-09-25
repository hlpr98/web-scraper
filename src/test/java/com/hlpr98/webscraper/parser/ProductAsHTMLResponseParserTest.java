package com.hlpr98.webscraper.parser;

import com.hlpr98.webscraper.model.entities.EntityWithTitle;
import com.hlpr98.webscraper.model.net.Response;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;

public class ProductAsHTMLResponseParserTest {

    private final ProductHTMLPageParser parser = new ProductHTMLPageParser();

    @Test
    public void testHandlesURLWithDomain() throws MalformedURLException {
        boolean result = parser.handlesURL("https://www.example.com/product-slug12312.html");
        assertTrue(result);

        result = parser.handlesURL("https://www.example.com/sub-path1/subpath-2/product-slug12312.html");
        assertTrue(result);

        result = parser.handlesURL("https://www.example.com/product.html");
        assertFalse(result);

        result = parser.handlesURL("https://www.example.com/product-slug12312.html/blah");
        assertTrue(result);
    }

    @Test
    public void testHandlesURLWithoutDomain() {
        Assert.assertThrows(MalformedURLException.class,
                () -> parser.handlesURL("/product-slug12312.html"));
    }

    @Test
    public void testParse() throws IOException {
        String response = "<html>\n" +
                "\t<head></head>\n" +
                "\t<body>\n" +
                "\t\t<h1 class=\"product-title\" data-id=\"0614a3a0-5716-4690-8bde-9a60177b5946\">My Product Title</h1>\n" +
                "\t</body>\n" +
                "</html>";

        EntityWithTitle entity = parser.parse(
                Response.builder()
                        .url(new URL("https://www.example.com/sub-path1/subpath-2/product-slug12312.html"))
                        .response(response)
                        .build()
        );

        assertNotNull(entity);
        assertEquals("0614a3a0-5716-4690-8bde-9a60177b5946", entity.getId());
        assertEquals("My Product Title", entity.getTitle());
    }
}