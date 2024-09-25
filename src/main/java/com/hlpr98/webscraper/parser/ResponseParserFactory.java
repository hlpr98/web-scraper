package com.hlpr98.webscraper.parser;

import lombok.extern.slf4j.Slf4j;

import java.net.MalformedURLException;
import java.util.List;

@Slf4j
public class ResponseParserFactory {

    private static final List<ResponseParser> PARSERS;

    static {
        PARSERS = List.of(
                new EntityJsonPageParser(),
                new ProductHTMLPageParser()
        );
    }

    // TODO: Optimise the selection process.
    // If URLs are known at compile time, this could be made into a simple Map<URL, Parser>
    // But when its a dynamic runtime input, see how this could be optimised.
    public static ResponseParser getParser(String url) throws MalformedURLException {
        for (ResponseParser parser : PARSERS) {
            if (parser.handlesURL(url)) {
                return parser;
            }
        }

        throw new IllegalStateException("No parser found for handling: " + url);
    }
}
