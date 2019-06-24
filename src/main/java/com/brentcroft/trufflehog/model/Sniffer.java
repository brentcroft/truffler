package com.brentcroft.trufflehog.model;

import com.brentcroft.trufflehog.util.TrufflerException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.Repository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public interface Sniffer
{
    List< Issue > sniff( Repository repo, DiffEntry diffEntry );


    default String getDiffEntryText( Repository repo, DiffEntry diffEntry )
    {
        try
        {
            OutputStream out = new ByteArrayOutputStream();

            DiffFormatter diffFormatter = new DiffFormatter( out );

            diffFormatter.setRepository( repo );

            diffFormatter.format( diffFormatter.toFileHeader( diffEntry ) );

            return out.toString();

        } catch( IOException e )
        {
            throw new TrufflerException( e );
        }
    }
}
