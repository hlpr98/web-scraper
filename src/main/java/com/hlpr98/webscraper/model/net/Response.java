package com.hlpr98.webscraper.model.net;

import lombok.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.http.HttpResponse;

@Getter
@Setter
@Builder
@NoArgsConstructor
@EqualsAndHashCode
@AllArgsConstructor
public class Response {

    private URL url;
    private String response;

    public static Response fromHTTPResponse(HttpResponse<String> response) throws MalformedURLException {
        if (response.body().isBlank()) {
            throw new IllegalArgumentException("Response body is empty");
        }
        return Response.builder()
                .url(response.uri().toURL())
                .response(response.body())
                .build();
    }
}
