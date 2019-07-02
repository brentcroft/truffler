package com.brentcroft.trufflehog.util;

import lombok.extern.java.Log;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Level;

import static java.lang.String.format;
import static java.nio.file.FileVisitResult.CONTINUE;

@Log
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
            } catch( NoSuchElementException e )
            {
                return "";
            }
        } );
    }

    private static < T > T accessResourceStream( String uri, Function< InputStream, T > accessor )
    {
        try( InputStream is = Local.class.getClassLoader().getResourceAsStream( uri ) )
        {
            if( Objects.isNull( is ) )
            {
                throw new IllegalArgumentException( "No such resource: " + uri );
            }
            return accessor.apply( is );

        } catch( IOException e )
        {
            throw new IllegalArgumentException( e );
        }
    }

    public static void removeAll( Path path )
    {
        if( Objects.nonNull( path ) )
        {
            boolean completed = false;
            int maxAttempts = 10;
            int attempts = 0;

            while( ! completed && attempts <= maxAttempts )
            {
                attempts++;

                try
                {
                    Files.walkFileTree( path, new Remover() );

                    completed = true;

                    log.log( Level.INFO, format( "Emptied directory after [%s] attempts: %s", attempts, path ) );

                } catch( Exception e )
                {
                    try
                    {
                        Thread.sleep( 1 );
                    } catch( InterruptedException e1 )
                    {
                        return;
                    }
                }
            }
        }
    }

    static class Remover extends SimpleFileVisitor< Path >
    {
        @Override
        public FileVisitResult visitFile( Path file, BasicFileAttributes attr )
        {
            if( attr.isRegularFile() )
            {
                if( ! file.toFile().delete() )
                {
                    throw new IllegalStateException( "Failed to delete file: " + file );
                }
            }

            return CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory( Path dir, IOException exc )
        {
            if( ! dir.toFile().delete() )
            {
                throw new IllegalStateException( "Failed to delete directory: " + dir );
            }

            return CONTINUE;
        }
    }
}
