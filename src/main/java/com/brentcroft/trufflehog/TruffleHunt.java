package com.brentcroft.trufflehog;

import org.junit.Before;
import org.junit.Test;


public class TruffleHunt
{
    protected DefaultTruffler truffler = new DefaultTruffler();

    @Before
    public void configure()
    {
        truffler.getEntropySniffer().setEntropyThreshold( "b64", 4.5 );
        truffler.getEntropySniffer().setEntropyThreshold( "hex", 3.0 );

        truffler.getRegexSniffer().withRegex( "ld", "la[z]ydog" );

        truffler.getXmlReceiver().withIssueTextNodes( true );
    }

    @Test
    public void truffle()
    {
        truffler.truffle();
    }
}
