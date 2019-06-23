package com.brentcroft.trufflehog.model;

public interface Receiver
{
    void receive ( CommitIssues commitIssues );

    default void open () {};

    default void close () {};

    default String serialize () { return ""; }
}
