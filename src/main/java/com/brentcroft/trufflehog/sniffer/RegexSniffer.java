package com.brentcroft.trufflehog.sniffer;

import com.brentcroft.trufflehog.model.Issue;
import com.brentcroft.trufflehog.util.TrufflerException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexSniffer implements Sniffer
{
    private Map< String, Pattern > regex = new HashMap<>();

    private Set< String > knownStrings;

    public RegexSniffer withRegexJsonFile( String path )
    {
        try
        {
            return withRegexMap( new ObjectMapper().readValue( new File( path ), STRING_MAP ) );

        } catch( IOException e )
        {
            throw new TrufflerException( e );
        }
    }

    private static final TypeReference STRING_MAP = new TypeReference< Map< String, String > >()
    {
    };

    public RegexSniffer withRegexJsonText( String text )
    {
        if( Objects.isNull( text ) || text.isEmpty() )
        {
            throw new IllegalArgumentException( "JSON regex text is empty" );
        }
        try
        {
            return withRegexMap( new ObjectMapper().readValue( text, STRING_MAP ) );

        } catch( IOException e )
        {
            throw new TrufflerException( e );
        }
    }


    public RegexSniffer withRegexMap( Map< String, String > regexText )
    {
        regexText.forEach( ( k, v ) -> regex.put( k, Pattern.compile( v ) ) );
        return this;
    }

    public RegexSniffer withRegex( String name, String pattern )
    {
        regex.put( name, Pattern.compile( pattern ) );
        return this;
    }



    public RegexSniffer withKnownStrings( Set< String > knownStrings )
    {
        this.knownStrings = knownStrings;
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

        regex.forEach( ( k, pattern ) -> {

            // riff through any matches
            Matcher m = pattern.matcher( diff );

            int[] occurrences = {0};
            String[] text = {null};

            while( m.find() )
            {
                text[ 0 ] = diff.substring( m.start(), m.end() );

                if ( Objects.nonNull( knownStrings ) && knownStrings.contains( text[0 ] ))
                {
                    continue;
                }

                occurrences[ 0 ]++;
            }

            if( occurrences[ 0 ] > 0 )
            {
                issues.add(
                        new Issue( "regex", text[ 0 ] )
                                .withAttribute( "key", k )
                                .withAttribute( "count", occurrences[ 0 ] ) );
            }
        } );

        return issues;
    }
}
