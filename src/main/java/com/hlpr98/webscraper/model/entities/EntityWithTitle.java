package com.hlpr98.webscraper.model.entities;

import lombok.*;

/**
 * A simple entity with only Title and ID fields
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@EqualsAndHashCode
@AllArgsConstructor
@ToString
public class EntityWithTitle {

    private String id;
    private String title;
}
