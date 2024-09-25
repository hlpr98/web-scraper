package com.hlpr98.webscraper.parser;

import com.hlpr98.webscraper.model.net.Response;
import com.hlpr98.webscraper.model.net.ResponseType;

import java.io.IOException;
import java.util.regex.Pattern;

public interface IResponseParser<T> {

    /**
     * Parses the web page response and return the result as POJO
     *
     * @return the parsed POJO
     */
    T parse(Response response) throws IOException;

    /**
     * A regex pattern that provides the url path pattern that the parser can handle
     *
     * @return a regex string
     * <p>
     * TODO: If multiple urls have same response signature, they could be handled by same parser. Maybe support array of patterns here.
     */
    Pattern pathPattern();

    ResponseType responseType();
}
