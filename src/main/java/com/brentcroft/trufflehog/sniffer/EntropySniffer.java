package com.brentcroft.trufflehog.sniffer;

import com.brentcroft.trufflehog.model.Issue;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Getter
public class EntropySniffer implements Sniffer
{
    private static final double LOG_10_2 = Math.log10( 2 );

    private static final Pattern WORD_SEPARATOR = Pattern.compile( "[ \\s.,()\"]" );


    private final Set< String > knownStrings = new HashSet<>();

    private final List< CharBase > charBases = new ArrayList<>();


    private static final TypeReference OBJECT_MAP_LIST = new TypeReference< List< Map< String, Object > > >()
    {
    };

    public EntropySniffer withJsonCharBases( String jsonCharBasesText )
    {
        try
        {
            List< Map< String, Object > > bases = new ObjectMapper().readValue( jsonCharBasesText, OBJECT_MAP_LIST );

            bases
                    .stream()
                    .map( SimpleCharBase::fromMap )
                    .forEach( scb -> {

                        charBases
                                .removeAll(
                                        charBases
                                                .stream()
                                                .filter( cb -> cb.getName().equals( scb.getName() ) )
                                                .collect( Collectors.toList() ) );

                        charBases.add( scb );
                    } );

            return this;

        } catch( IOException e )
        {
            throw new IllegalArgumentException( e );
        }
    }


    public EntropySniffer withKnownStrings( List< String > strings )
    {
        if ( Objects.nonNull( strings ))
        {
            knownStrings
                    .addAll( strings
                            .stream()
                            .filter( s -> ! s.startsWith( "#" ) )
                            .map( String::trim )
                            .filter( s -> ! s.isEmpty() )
                            .collect( Collectors.toList() ) );
        }
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
            WORD_SEPARATOR
                    .splitAsStream( line )
                    .filter( word -> ! word.isEmpty() )
                    .forEach( word -> {
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
                    } );
        }
        return issues;
    }

    private double mathLog2( double value )
    {
        return Math.log10( value ) / LOG_10_2;
    }


    public double getShannonEntropy( String charset, String data )
    {

        if( Objects.isNull( data ) || data.length() == 0 )
        {
            return 0;
        }

        double[] entropy = {0};




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

                        entropy[ 0 ] += ratio * mathLog2( ratio );
                    }
                } );

        return ( entropy[ 0 ] < 0 -1 ? -1 : 1 ) * entropy[ 0 ] ;
    }


    public interface CharBase
    {
        String getName();

        String getCharset();

        double getThreshold();

        int getMaxLength();

        default boolean contains( char c)
        {
            return getCharset().indexOf( c ) >= 0;
        }


        default List< String > stringsOfSet( String word )
        {
            int count = 0;
            StringBuilder letters = new StringBuilder();
            List< String > strings = new ArrayList<>();

            // include contiguous sequences of charset members
            // that ar bigger than the min length
            for( char c : word.toCharArray() )
            {
                if( contains( c ) )
                {
                    letters.append( c );
                    count += 1;
                }
                // if exceeded length then collect and continue
                else
                {
                    if( count >= getMaxLength() )
                    {
                        strings.add( letters.toString() );
                    }
                    letters.setLength( 0 );
                    count = 0;
                }
            }
            // if exceeded length then collect
            if( count >= getMaxLength() )
            {
                strings.add( letters.toString() );
            }
            return strings;
        }
    }

    @RequiredArgsConstructor
    @Getter
    public static class SimpleCharBase implements CharBase
    {
        private final String name;
        private final String charset;
        private final double threshold;
        private final int maxLength;

        private final Map<Character, Boolean> cache = new HashMap<>(  );

        private boolean cache(char c, boolean inCharset)
        {
            cache.put( c, inCharset );
            return inCharset;
        }

        @Override
        public boolean contains( char c)
        {
            Boolean cacheHit = cache.get( c );

            if ( Objects.isNull( cacheHit ))
            {
                return cache( c, CharBase.super.contains( c ) );
            }

            return cacheHit;
        }

        static SimpleCharBase fromMap( Map< String, ? > entries )
        {
            return new SimpleCharBase(
                    ( String ) entries.get( "name" ),
                    ( String ) entries.get( "chars" ),
                    ( ( Number ) entries.get( "threshold" ) ).doubleValue(),
                    ( ( Number ) entries.get( "maxLength" ) ).intValue() );
        }
    }

    public void setEntropyThreshold( String tag, double threshold )
    {
        EntropySniffer.CharBase ocb = getCharBases()
                .stream()
                .filter( cb -> tag.equals( cb.getName() ) )
                .findFirst()
                .orElseThrow( () -> new IllegalArgumentException( "No charbase named: " + tag ) );

        getCharBases()
                .remove( ocb );

        getCharBases()
                .add( new EntropySniffer.SimpleCharBase(
                        ocb.getName(),
                        ocb.getCharset(),
                        threshold,
                        ocb.getMaxLength() ) );
    }

    public void setEntropyMaxLength( String tag, int maxLength )
    {
        EntropySniffer.CharBase ocb = getCharBases()
                .stream()
                .filter( cb -> tag.equals( cb.getName() ) )
                .findFirst()
                .orElseThrow( () -> new IllegalArgumentException( "No charbase named: " + tag ) );

        getCharBases()
                .remove( ocb );

        getCharBases()
                .add( new EntropySniffer.SimpleCharBase(
                        ocb.getName(),
                        ocb.getCharset(),
                        ocb.getThreshold(),
                        maxLength ) );
    }
}
