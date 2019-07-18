package com.brentcroft.trufflehog;

import org.junit.Before;
import org.junit.Test;

import static java.lang.String.format;
import static junit.framework.TestCase.assertEquals;

public class TruffleHunt
{
    private static final String TRUFFLE_ISSUES_MESSAGE = "Expected %d truffle issues but there were %d." +
            "%n%nReview the file [%s] in a browser, and either: " +
            " %n 1. exempt known strings by adding them to 'truffler/entropy-known-strings.txt' " +
            " %n 2. select strings to replace and paths (whole nodes) to remove and then rewrite the repository history %n ";

    protected DefaultTruffler truffler = new DefaultTruffler();

    @Before
    public void configure()
    {
        //truffler.getEntropySniffer().setEntropyThreshold( "b64", 4.5 );
        //truffler.getEntropySniffer().setEntropyThreshold( "hex", 3.0 );

        //truffler.getRegexSniffer().withRegex( "ld", "[a]ardvark" );

        //truffler.getXmlReceiver().withIssueTextNodes( true );
    }


    @Test
    public void truffle()
    {
        truffler.truffle();

        long actualIssues = truffler.getIssuesCount();

        long expectedIssues = 0;

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
