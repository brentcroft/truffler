package com.brentcroft.trufflehog.sniffer;

import com.brentcroft.trufflehog.model.Issue;
import com.brentcroft.trufflehog.model.Sniffer;
import com.brentcroft.trufflehog.util.TrufflerException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.Repository;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import static java.lang.String.format;

public class RegexSniffer implements Sniffer
{
    private Map<String, Pattern > regex = new HashMap<> (  );

    public RegexSniffer withJsonRegexFile(String path )
    {
        try
        {
            setRegex(
                    new ObjectMapper()
                            .readValue ( new File (path), Map.class ));

        } catch ( IOException e )
        {
            throw new TrufflerException ( e );
        }
        return this;
    }


    public void setRegex(Map<String, String > regexText )
    {
        regexText.forEach ( (k,v)->regex.put ( k, Pattern.compile ( v ) ) );
    }


    @Override
    public List< Issue > sniff ( Repository repo, DiffEntry diffEntry )
    {
        return investigateEntry(repo, diffEntry);
    }


    private List< Issue > investigateEntry ( Repository repo, DiffEntry diffEntry )
    {
        String diffEntryText = getDiffEntryText ( repo, diffEntry );

        if ( Objects.isNull ( diffEntryText ) || diffEntryText.isEmpty () )
        {
            return Collections.emptyList ();
        }

        List< Issue > issues = new ArrayList<> ();

        regex.forEach ( (k, pattern) -> {
            Matcher m = pattern.matcher ( diffEntryText );
            while ( m.find () )
            {
                String text = diffEntryText.substring ( m.start (), m.end () );

                issues.add ( new Issue ( "regex", text )
                {
                    String TYPE = "key";

                    {
                        getAttributes ().put ( TYPE, k );
                    }

                    public String toString ()
                    {
                        return format (
                                "[%s=%s] %s",
                                TYPE,
                                k,
                                text );
                    }
                } );
            }
        } );

        return issues;
    }

}
