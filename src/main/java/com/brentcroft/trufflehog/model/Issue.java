package com.brentcroft.trufflehog.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.String.format;

@RequiredArgsConstructor
@Getter
public class Issue
{
    private final String tag;
    private final String text;
    private final Map< String, Object > attributes = new HashMap<>();

    public Issue withAttribute( String key, Object value )
    {
        attributes.put( key, value );
        return this;
    }

    public String toString()
    {
        return format(
                "[%s] %s",
                attributes
                        .entrySet()
                        .stream()
                        .map( e -> format( "%s=%s", e.getKey(), e.getValue() ) )
                        .collect( Collectors.joining( ", " ) ),
                text );
    }
}
