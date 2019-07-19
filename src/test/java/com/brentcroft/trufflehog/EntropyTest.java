package com.brentcroft.trufflehog;

import com.brentcroft.trufflehog.sniffer.EntropySniffer;
import org.junit.Test;

import java.util.UUID;
import java.util.stream.IntStream;

import static java.lang.String.format;
import static org.fest.assertions.api.Assertions.assertThat;

public class EntropyTest
{
    private static final int NUM_UUIDS = 10000;

    private static final String B64_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";
    private static final String HEX_CHARS = "1234567890abcdefABCDEF";

    private EntropySniffer b64Sniffer = new EntropySniffer()
            .withJsonCharBases( toJsonCharBase(
                    "b64",
                    B64_CHARS,
                    4.5,
                    20
            ) );

    private EntropySniffer hexSniffer = new EntropySniffer()
            .withJsonCharBases( toJsonCharBase(
                    "hex",
                    HEX_CHARS,
                    3,
                    20
            ) );


    private String toJsonCharBase( String name, String chars, double threshold, int maxLenth )
    {
        return format( "[{\"name\": \"%s\", \"chars\": \"%s\", \"threshold\": %s, \"maxLength\": %s }]",
                name,
                chars,
                threshold,
                maxLenth
        );
    }

    @Test
    public void b64_entropy_of_empty_string_is_zero()
    {
        double entropy = b64Sniffer.getShannonEntropy( B64_CHARS, "" );

        assertThat( entropy ).isEqualTo( 0 );
    }

    @Test
    public void b64_entropy_of_20_a_is_zero()
    {
        double entropy = b64Sniffer.getShannonEntropy( B64_CHARS, "aaaaaaaaaaaaaaaaaaaa" );

        assertThat( entropy ).isEqualTo( 0 );
    }


    @Test
    public void b64_entropy_of_b64()
    {
        double entropy = b64Sniffer.getShannonEntropy( B64_CHARS, B64_CHARS );

        assertThat( entropy ).isEqualTo( 6.022367813028458 );
    }


    @Test
    public void b64_entropy_of_hex()
    {
        double entropy = b64Sniffer.getShannonEntropy( B64_CHARS, HEX_CHARS );

        assertThat( entropy ).isEqualTo( 4.459431618637295 );
    }


    @Test
    public void b64_entropy_of_uuid_is_greater_than_2_5()
    {
        IntStream
                .range( 0, NUM_UUIDS )
                .forEach( i -> {
                    double entropy = b64Sniffer.getShannonEntropy( B64_CHARS, UUID.randomUUID().toString() );

                    assertThat( entropy ).isGreaterThan( 2.5 );
                } );
    }


    @Test
    public void hex_entropy_of_empty_string_is_zero()
    {

        assertThat( hexSniffer.getShannonEntropy( HEX_CHARS, "" ) ).isEqualTo( 0 );
    }

    @Test
    public void hex_entropy_of_20_a_is_zero()
    {
        double entropy = b64Sniffer.getShannonEntropy( HEX_CHARS, "aaaaaaaaaaaaaaaaaaaa" );

        assertThat( entropy ).isEqualTo( 0 );
    }

    @Test
    public void hex_entropy_of_hex()
    {
        double entropy = hexSniffer.getShannonEntropy( HEX_CHARS, HEX_CHARS );

        assertThat( entropy ).isEqualTo( 4.459431618637295 );
    }


    @Test
    public void hex_entropy_of_b64()
    {
        double entropy = hexSniffer.getShannonEntropy( HEX_CHARS, B64_CHARS );

        assertThat( entropy ).isEqualTo( 2.038339875178862 );
    }


    @Test
    public void hex_entropy_of_uuid_is_greater_than_2_5()
    {
        IntStream
                .range( 0, NUM_UUIDS )
                .forEach( i -> {
                    double entropy = hexSniffer.getShannonEntropy( HEX_CHARS, UUID.randomUUID().toString() );

                    assertThat( entropy ).isGreaterThan( 2.5 );
                } );
    }
}
