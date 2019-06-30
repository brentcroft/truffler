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

    public RegexSniffer withJsonRegexFile( String path )
    {
        try
        {
            return withRegex( new ObjectMapper().readValue( new File( path ), STRING_MAP ) );

        } catch( IOException e )
        {
            throw new TrufflerException( e );
        }
    }

    private static final TypeReference STRING_MAP = new TypeReference< Map< String, String > >()
    {
    };

    public RegexSniffer withJsonRegexText( String text )
    {
        if( Objects.isNull( text ) || text.isEmpty() )
        {
            throw new IllegalArgumentException( "JSON regex text is empty" );
        }
        try
        {
            return withRegex( new ObjectMapper().readValue( text, STRING_MAP ) );

        } catch( IOException e )
        {
            throw new TrufflerException( e );
        }
    }


    public RegexSniffer withRegex( Map< String, String > regexText )
    {
        regexText.forEach( ( k, v ) -> regex.put( k, Pattern.compile( v ) ) );
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

            Matcher m = pattern.matcher( diff );

            int[] occurrences = {0};
            String[] text = {null};

            while( m.find() )
            {
                text[ 0 ] = diff.substring( m.start(), m.end() );
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
