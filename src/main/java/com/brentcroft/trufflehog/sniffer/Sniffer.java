package com.brentcroft.trufflehog.sniffer;

import com.brentcroft.trufflehog.model.Issue;

import java.util.Set;

public interface Sniffer
{
    Set< Issue > sniff( String diff );
}
