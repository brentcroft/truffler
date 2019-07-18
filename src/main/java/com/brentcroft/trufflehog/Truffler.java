package com.brentcroft.trufflehog;

import com.brentcroft.trufflehog.model.CommitIssues;
import com.brentcroft.trufflehog.model.DiffIssues;
import com.brentcroft.trufflehog.model.Issue;
import com.brentcroft.trufflehog.receiver.Receiver;
import com.brentcroft.trufflehog.sniffer.Sniffer;
import com.brentcroft.trufflehog.util.JUL;
import com.brentcroft.trufflehog.util.Local;
import com.brentcroft.trufflehog.util.TrufflerException;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;
import org.eclipse.jgit.util.FS_Win32;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
@Setter
@Log
public class Truffler
{
    static
    {
        JUL.install();
    }


    private static final Pattern DIFF_SEPARATOR = Pattern.compile( "@@[\\s\\-+,\\d]+@@", Pattern.MULTILINE );
    private static final Pattern LINES_SEPARATOR = Pattern.compile( "[\\r\\n]+", Pattern.MULTILINE );


    private String repositoryDirectory = ".";
    private String earliestCommitId = null;
    private int maxDepth = Integer.MAX_VALUE;

    private final List< Sniffer > sniffers = new ArrayList<>();
    private final List< Receiver > receivers = new ArrayList<>();



    public void truffle()
    {
        try( Repository repo = openRepo() )
        {
            try( Git git = new Git( repo ) )
            {

                ObjectId headId = repo.resolve( Constants.HEAD );

                if( Objects.isNull( headId ) )
                {
                    throw new IllegalArgumentException( "No object id for HEAD." );
                }

                String headName = headId.name();


                Ref branch = git
                        .branchList()
                        .setListMode( ListBranchCommand.ListMode.ALL )
                        .call()
                        .stream()
                        .filter( ref -> headName.equals( ref.getObjectId().name() ) )
                        .findAny()
                        .orElseThrow( () -> new RuntimeException( "No branch named: " + headId ) );

                Map< String, String > attr = new HashMap<>();
                attr.put( "branch", branch.getName() );
                attr.put( "repo", repositoryDirectory );

                receivers.forEach( r -> r.open( attr ) );

                try( RevWalk walk = new RevWalk( repo ) )
                {
                    Set< String > alreadySeen = new HashSet<>();

                    processCommits(
                            repo,
                            walk,
                            walk.parseCommit( branch.getObjectId() ),
                            maxDepth,
                            alreadySeen );
                } finally
                {
                    receivers.forEach( Receiver::close );
                }
            }
        } catch( Exception e )
        {
            throw new RuntimeException( e );
        }
    }

    private void processCommits( Repository repo, RevWalk walk, RevCommit commit, int depth, Set< String > alreadySeen ) throws IOException
    {
        String commitId = commit.getId().name();

        if( depth < 0 || alreadySeen.contains( commitId ) )
        {
            return;
        }

        alreadySeen.add( commitId );

        RevCommit[] parents = commit.getParents();

        if( Objects.isNull( parents ) || parents.length == 0 )
        {
            diffFirstCommit( repo, commit );
        }
        else
        {
            for( RevCommit parentCommit : parents )
            {
                walk.parseHeaders( parentCommit );

                diffIssues( repo, commit, parentCommit );

                if( ! commitId.equals( earliestCommitId ) )
                {
                    processCommits( repo, walk, parentCommit, depth - 1, alreadySeen );
                }
            }
        }
    }

    private void diffFirstCommit( Repository repo, RevCommit commit ) throws IOException
    {
        AbstractTreeIterator oldTreeIter = new EmptyTreeIterator();
        ObjectReader reader = repo.newObjectReader();
        AbstractTreeIterator newTreeIter = new CanonicalTreeParser( null, reader, commit.getTree() );

        try( DiffFormatter df = new DiffFormatter( new ByteArrayOutputStream() ) )
        {
            df.setRepository( repo );

            processDiffEntries( repo, commit, df.scan( oldTreeIter, newTreeIter ) );
        }
    }

    private void diffIssues( Repository repo, RevCommit commit, RevCommit parentCommit ) throws IOException
    {
        try( DiffFormatter df = new DiffFormatter( new ByteArrayOutputStream() ) )
        {
            df.setRepository( repo );

            processDiffEntries( repo, commit, df.scan( commit, parentCommit ) );
        }
    }

    private void processDiffEntries( Repository repo, RevCommit commit, List< DiffEntry > entries )
    {
        CommitIssues commitIssues = new CommitIssues( commit );

        for( DiffEntry entry : entries )
        {
            DiffIssues diffIssues = notifySniffers( repo, entry );

            if( Objects.nonNull( diffIssues ) )
            {
                commitIssues.getDiffIssues().add( diffIssues );
            }
        }
        if( commitIssues.hasIssues() )
        {
            receivers.forEach( r -> r.receive( commitIssues ) );
        }
    }


    private String getDiffEntryText( Repository repo, DiffEntry diffEntry )
    {
        try
        {
            OutputStream out = new ByteArrayOutputStream();

            DiffFormatter diffFormatter = new DiffFormatter( out );

            diffFormatter.setRepository( repo );

            diffFormatter.format( diffFormatter.toFileHeader( diffEntry ) );

            String rawText = out.toString();

            Matcher m = DIFF_SEPARATOR.matcher( rawText );

            if( m.find() )
            {
                rawText = rawText.substring( m.start() );
            }

            String[] textLines = LINES_SEPARATOR.split( rawText );

            return Stream.of( textLines )
                    .filter( s -> s.length() > 1 )
                    .map( s -> s.substring( 1 ) )
                    .filter( s -> s.length() > 0 )
                    .collect( Collectors.joining( "\n" ) );

        } catch( IOException e )
        {
            throw new TrufflerException( e );
        }
    }


    private DiffIssues notifySniffers( Repository repo, DiffEntry diffEntry )
    {
        String diffEntryText = getDiffEntryText( repo, diffEntry );

        Set< Issue > issues = sniffers
                .stream()
                .map( sniffer -> sniffer.sniff( diffEntryText ) )
                .flatMap( Set::stream )
                .filter( Objects::nonNull )
                .collect( Collectors.toSet() );

        if( Objects.isNull( issues ) || issues.isEmpty() )
        {
            return null;
        }

        return new DiffIssues( diffEntry, diffEntryText, issues );
    }

    public Repository openRepo()
    {
        try
        {
            return new FileRepositoryBuilder()
                    .setGitDir( Paths.get( repositoryDirectory, ".git" ).toFile() )
                    .readEnvironment()
                    .findGitDir()
                    .build();
        } catch( IOException e )
        {
            throw new TrufflerException( e );
        }
    }
}
