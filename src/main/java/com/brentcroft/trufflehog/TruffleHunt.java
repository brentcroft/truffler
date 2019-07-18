package com.brentcroft.trufflehog;

import org.junit.Before;
import org.junit.Test;

import static java.lang.String.format;
import static junit.framework.TestCase.assertEquals;

public class TruffleHunt
{
    private static final String TRUFFLE_ISSUES_MESSAGE = "Expected %d truffle issues but there were %d." +
            "%nOpen the file [%s] in a browser and either, %n exempt known strings %n or select strings to replace %n 3. select paths to remove." +
            "or prepare to rewrite history. %n ";

    protected DefaultTruffler truffler = new DefaultTruffler();

    @Before
    public void configure()
    {
        truffler.getEntropySniffer().setEntropyThreshold( "b64", 4.5 );
        truffler.getEntropySniffer().setEntropyThreshold( "hex", 3.0 );

        truffler.getRegexSniffer().withRegex( "ld", "[a]ardvark" );

        truffler.getXmlReceiver().withIssueTextNodes( true );
    }


    @Test
    public void truffle()
    {
        truffler.truffle();

        long expectedIssues = 0;
        long actualIssues = truffler.getNumIssues();

        assertEquals(
                format(
                        TRUFFLE_ISSUES_MESSAGE,
                        expectedIssues,
                        actualIssues,
                        truffler.getXmlReceiver().getHtmlFilename()
                ),
                expectedIssues,
                actualIssues
        );
    }
}
