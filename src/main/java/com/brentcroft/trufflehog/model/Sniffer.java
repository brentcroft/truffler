package com.brentcroft.trufflehog.model;

import com.brentcroft.trufflehog.util.TrufflerException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.lib.Repository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface Sniffer
{
    List< Issue > sniff( Repository repo, DiffEntry diffEntry );


    Pattern DIFF_SEPARATOR = Pattern.compile( "@@[\\s\\-+,\\d]+@@", Pattern.MULTILINE );
    Pattern LINES_SEPARATOR = Pattern.compile( "[\\r\\n]+", Pattern.MULTILINE );


    default String getDiffEntryText( Repository repo, DiffEntry diffEntry )
    {
        try
        {
            OutputStream out = new ByteArrayOutputStream();

            DiffFormatter diffFormatter = new DiffFormatter( out );

            diffFormatter.setRepository( repo );

            diffFormatter.format( diffFormatter.toFileHeader( diffEntry ) );

            String rawText = out.toString();

            Matcher m = DIFF_SEPARATOR.matcher( rawText );

            if ( m.find())
            {
                rawText = rawText.substring( m.end() );
            }

            String[] textLines = LINES_SEPARATOR.split( rawText );

            return Stream.of(textLines)
                    .filter( s -> s.length() > 1 )
                    .map( s -> s.substring( 1 ) )
                    .filter( s -> s.length() > 0 )
                    .collect( Collectors.joining( "\n" ) );

        } catch( IOException e )
        {
            throw new TrufflerException( e );
        }
    }
}
