package com.brentcroft.trufflehog.receiver;

import com.brentcroft.trufflehog.model.CommitIssues;

import java.util.Map;

public interface Receiver
{
    void receive( CommitIssues commitIssues );

    default void open( Map<String, String> attr )
    {
    }

    default void close()
    {
    }

    default String serialize()
    {
        return "";
    }
}
