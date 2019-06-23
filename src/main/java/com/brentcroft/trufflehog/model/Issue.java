package com.brentcroft.trufflehog.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@ToString
@Getter
public class Issue
{
    private final String tag;
    private final String text;
    private final Map<String,Object> attributes = new HashMap<> (  );
}
