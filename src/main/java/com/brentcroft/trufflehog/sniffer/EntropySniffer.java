package com.brentcroft.trufflehog.sniffer;

import com.brentcroft.trufflehog.model.Issue;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Getter
public class EntropySniffer implements Sniffer
{

    private static final String BASE64_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=@.";
    public static double BASE64_THRESHOLD = 4.5;

    private static final String HEX_CHARS = "1234567890abcdefABCDEF";
    public static double HEX_THRESHOLD = 3;

    public static int MIN_LENGTH = 20;

    private final Set< String > knownStrings = new HashSet<>();

    private final List< CharBase > charBases = new ArrayList<>();


    private static final TypeReference OBJECT_MAP_MAP = new TypeReference< Map< String, Map< String, Object > > >()
    {
    };

    public EntropySniffer withJsonCharBases( String jsonCharBasesText )
    {
        try
        {
            Map< String, Map< String, Object > > bases = new ObjectMapper().readValue( jsonCharBasesText, OBJECT_MAP_MAP );

            bases.forEach( ( key, charBaseMap ) -> {
                charBaseMap.put( "name", key );
                charBases.add( SimpleCharBase.fromMap( charBaseMap ) );
            } );

        } catch( IOException e )
        {
            throw new IllegalArgumentException( e );
        }


        return this;
    }


    public EntropySniffer withKnownStrings( List< String > strings )
    {
        knownStrings.addAll( strings
                .stream()
                .filter( s -> ! s.startsWith( "#" ) )
                .map( String::trim )
                .filter( s -> ! s.isEmpty() )
                .collect( Collectors.toList() ) );

        return this;
    }


    @Override
    public Set< Issue > sniff( String diff )
    {
        if( Objects.isNull( diff ) || diff.isEmpty() )
        {
            return Collections.emptySet();
        }

        Set< Issue > issues = new HashSet<>();

        for( String line : diff.split( "\n" ) )
        {
            for( String word : line.split( "[ \\s.()]" ) )
            {
                for( CharBase charBase : charBases )
                {
                    charBase.stringsOfSet( word )
                            .stream()
                            .filter( text -> ! knownStrings.contains( text ) )
                            .forEach( text -> {

                                double entropy = getShannonEntropy( charBase.getCharset(), text );

                                if( entropy > charBase.getThreshold() )
                                {
                                    issues.add(
                                            new Issue( "entropy", text )
                                                    .withAttribute( "op", charBase.getName() )
                                                    .withAttribute( "score", entropy ) );
                                }
                            } );
                }
            }
        }
        return issues;
    }


    // see: https://stackoverflow.com/questions/3719631/log-to-the-base-2-in-python
    private double mathLog( double value, double base )
    {
        return Math.log10( value ) / Math.log10( base );
    }


    private float getShannonEntropy( String charset, String data )
    {

        if( Objects.isNull( data ) || data.length() == 0 )
        {
            return 0;
        }

        float[] entropy = {0};

        IntStream
                .range( 0, charset.length() )
                .map( charset::charAt )
                .distinct()
                .forEach( c -> {

                    long occurrences = IntStream
                            .range( 0, data.length() )
                            .filter( i -> c == data.charAt( i ) )
                            .count();

                    if( occurrences > 0 )
                    {
                        double ratio = ( double ) occurrences / data.length();

                        entropy[ 0 ] += ratio * mathLog( ratio, 2 );
                    }
                } );

        return - 1 * entropy[ 0 ];
    }


    public interface CharBase
    {
        String getName();

        String getCharset();

        double getThreshold();

        int getMaxLength();

        default List< String > stringsOfSet( String word )
        {
            int count = 0;
            StringBuilder letters = new StringBuilder();
            List< String > strings = new ArrayList<>();
            for( char c : word.toCharArray() )
            {
                // include all charset chars
                if( getCharset().indexOf( c ) >= 0 )
                {
                    letters.append( c );
                    count += 1;
                }
                // if exceeded length then collect and continue
                else if( count > getMaxLength() )
                {
                    strings.add( letters.toString() );
                    letters.setLength( 0 );
                    count = 0;
                }
            }
            // if exceeded length then collect
            if( count > getMaxLength() )
            {
                strings.add( letters.toString() );
            }
            return strings;
        }
    }

    @RequiredArgsConstructor
    @Getter
    static class SimpleCharBase implements CharBase
    {
        private final String name;
        private final String charset;
        private final double threshold;
        private final int maxLength;

        static SimpleCharBase fromMap( Map< String, ? > entries )
        {
            return new SimpleCharBase(
                    ( String ) entries.get( "name" ),
                    ( String ) entries.get( "chars" ),
                    ( ( Number ) entries.get( "threshold" ) ).doubleValue(),
                    ( ( Number ) entries.get( "maxLength" ) ).intValue() );
        }
    }

}
