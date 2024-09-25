package com.hlpr98.webscraper.parser;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;

@Slf4j
public abstract class ResponseParser<T> implements IResponseParser<T> {

    private final Class<T> tClass;

    protected ResponseParser(Class<T> tClass) {
        this.tClass = tClass;
    }

    /**
     * Tells if the response from the provided URL could be parsed by the parser
     *
     * @param url the page url
     * @return boolean saying if the parser can handle the response
     */
    public boolean handlesURL(String url) throws MalformedURLException {
        URL parsedURL = new URL(url);
        return this.pathPattern().matcher(parsedURL.getPath()).find();
    }

    protected T getEntityInstance() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Class<?> clazz = Class.forName(tClass.getName());
        Constructor<?> constructor = clazz.getDeclaredConstructor(); // get no args constructor
        return (T) constructor.newInstance();
    }
}
