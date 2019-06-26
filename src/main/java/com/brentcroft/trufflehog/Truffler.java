package com.brentcroft.trufflehog;

import com.brentcroft.trufflehog.model.CommitIssues;
import com.brentcroft.trufflehog.model.DiffIssues;
import com.brentcroft.trufflehog.model.Receiver;
import com.brentcroft.trufflehog.model.Sniffer;
import com.brentcroft.trufflehog.util.JUL;
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Getter
@Setter
@Log
public class Truffler
{
    static
    {
        JUL.install();
    }

    public int maxDepth = Integer.MAX_VALUE;

    private String repositoryDirectory = ".";
    private String earliestCommitId = null;

    private final List< Sniffer > sniffers = new ArrayList<>();
    private final List< Receiver > receivers = new ArrayList<>();

    public void truffle()
    {
        try( Repository repo = openRepo() )
        {
            try( Git git = new Git( repo ) )
            {

                ObjectId headId = repo.resolve( Constants.HEAD );
                String headName = headId.name();

                receivers.forEach( Receiver::open );

                Ref branch = git
                        .branchList()
                        .setListMode( ListBranchCommand.ListMode.ALL )
                        .call()
                        .stream()
                        .filter( ref -> headName.equals( ref.getObjectId().name() ) )
                        .findAny()
                        .orElseThrow( () -> new RuntimeException( "No branch named: " + headId ) );

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

                if( !commitId.equals( earliestCommitId ) )
                {
                    processCommits( repo, walk, parentCommit, depth - 1, alreadySeen );
                }
            }
        }
    }

    private void diffFirstCommit(Repository repo, RevCommit commit) throws IOException
    {
        AbstractTreeIterator oldTreeIter = new EmptyTreeIterator();
        ObjectReader reader = repo.newObjectReader();
        AbstractTreeIterator newTreeIter = new CanonicalTreeParser( null, reader, commit.getTree() );

        try( DiffFormatter df = new DiffFormatter( new ByteArrayOutputStream() ) )
        {
            df.setRepository( repo );

            processDiffEntries(repo, commit, df.scan( oldTreeIter, newTreeIter ));
        }
    }

    private void diffIssues( Repository repo, RevCommit commit, RevCommit parentCommit ) throws IOException
    {
        try( DiffFormatter df = new DiffFormatter( new ByteArrayOutputStream() ) )
        {
            df.setRepository( repo );

            processDiffEntries(repo, commit, df.scan( commit, parentCommit ));
        }
    }

    private void processDiffEntries(Repository repo, RevCommit commit, List< DiffEntry > entries)
    {
        CommitIssues commitIssues = new CommitIssues( commit );

        for( DiffEntry entry : entries )
        {
            commitIssues
                    .getDiffIssues()
                    .addAll( notifySniffers( repo, entry ) );
        }
        if( ! commitIssues.hasIssues() )
        {
            receivers.forEach( r -> r.receive( commitIssues ) );
        }
    }

    private List< DiffIssues > notifySniffers( Repository repo, DiffEntry diffEntry )
    {
        return sniffers
                .stream()
                .map( sniffer -> sniffer.sniff( repo, diffEntry ) )
                .filter( Objects::nonNull )
                .filter( issues -> ! issues.isEmpty() )
                .map( issues -> new DiffIssues( diffEntry, issues ) )
                .collect( Collectors.toList() );
    }

    public Repository openRepo()
    {
        try
        {
            return new FileRepositoryBuilder()
                    .setGitDir( Paths.get( repositoryDirectory, ".git" ).toFile() )
                    .readEnvironment() // scan environment GIT_* variables
                    .findGitDir() // scan up the file system tree
                    .build();
        } catch( IOException e )
        {
            throw new TrufflerException( e );
        }
    }
}
