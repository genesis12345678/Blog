package com.spring.blog.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CacheType {

    ARTICLE_LIKE("liked", 60, 10000),
    ARTICLES("articles", 5 * 60, 10000),
    WEATHER_DATA("weatherData", 60 * 60, 10000);

    private final String cacheName;
    private final int expireAfterWrite;
    private final int maximumSize;
}