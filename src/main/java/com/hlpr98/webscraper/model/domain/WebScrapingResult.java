package com.hlpr98.webscraper.model.domain;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode
@ToString
public class WebScrapingResult {

    private Object parsedEntity;
    private Throwable exception;
}
