package com.brentcroft.trufflehog.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.function.Function;

public class Local
{
    public static List< String > getFileLines( String uri )
    {
        return Arrays.asList( getFileText( uri ).split( "\n" ) );
    }

    public static String getFileText( String uri )
    {
        return accessResourceStream( uri, is -> {
            try( Scanner scanner = new Scanner( is ) )
            {
                return scanner.useDelimiter( "\\Z" ).next();
            }
        } );
    }

    private static < T > T accessResourceStream( String uri, Function< InputStream, T > accessor )
    {
        try( InputStream is = Local.class.getClassLoader().getResourceAsStream( uri ) )
        {
            if ( Objects.isNull(is))
            {
                throw new IllegalArgumentException( "No such resource: " + uri );
            }
            return accessor.apply( is );

        } catch( IOException e )
        {
            throw new IllegalArgumentException( e );
        }
    }
}
