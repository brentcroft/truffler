package com.brentcroft.trufflehog;

import com.brentcroft.trufflehog.model.*;
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;

@Getter
@Setter
@Log
public class Truffler
{
    static
    {
        JUL.install ();
    }

    public static final String EMPTY_TREE = "4b825dc642cb6eb9a060e54bf8d69288fbee4904";

    public int maxDepth = 3;
    private String repositoryDirectory;

    private final List< Sniffer > sniffers = new ArrayList<> ();
    private final List< Receiver > receivers = new ArrayList<> ();


    public void truffle ()
    {
        try ( Repository repo = openRepo () )
        {
            try ( Git git = new Git ( repo ) )
            {

                ObjectId headId = repo.resolve ( Constants.HEAD );
                String headName = headId.name ();

                receivers.forEach ( Receiver::open );

                Ref branch = git
                        .branchList ()
                        .setListMode ( ListBranchCommand.ListMode.ALL )
                        .call ()
                        .stream ()
                        .filter ( ref -> headName.equals ( ref.getObjectId ().name () ) )
                        .findAny ()
                        .orElseThrow ( () -> new RuntimeException ( "No branch named: " + headId ) );

                try ( RevWalk walk = new RevWalk ( repo ) )
                {
                    Set< String > alreadySeen = new HashSet<> ();

                    processCommits (
                            repo,
                            walk,
                            walk.parseCommit ( branch.getObjectId () ),
                            maxDepth,
                            alreadySeen );
                }
                finally
                {
                    receivers.forEach ( Receiver::close );
                }
            }
        } catch ( Exception e )
        {
            throw new RuntimeException ( e );
        }
    }


    private void processCommits ( Repository repo, RevWalk walk, RevCommit commit, int depth, Set< String > alreadySeen ) throws IOException
    {

        if ( depth < 0 || alreadySeen.contains ( commit.getId ().name () ) )
        {
            return;
        }

        alreadySeen.add ( commit.getId ().name () );

        RevCommit[] parents = commit.getParents ();

        if ( Objects.isNull ( parents ) || parents.length == 0 )
        {
            // TODO: diff against empty tree
        }
        else
        {


            for ( RevCommit parentCommit : parents )
            {
                walk.parseHeaders ( parentCommit );

                DiffFormatter df = new DiffFormatter ( new ByteArrayOutputStream () );
                df.setRepository ( repo );

                List< DiffEntry > entries = df.scan ( commit, parentCommit );

                CommitIssues commitIssues = new CommitIssues ( commit );

                for ( DiffEntry entry : entries )
                {
                    commitIssues
                            .getDiffIssues ()
                            .addAll ( notifySniffers ( repo, entry ) );
                }

                df.close ();

                if ( ! commitIssues.hasIssues () )
                {
                    receivers.forEach ( r -> r.receive ( commitIssues ) );
                }


                processCommits (
                        repo,
                        walk,
                        parentCommit,
                        depth - 1,
                        alreadySeen );
            }
        }
    }

    private List< DiffIssues > notifySniffers ( Repository repo, DiffEntry diffEntry )
    {
        return sniffers
                .stream ()
                .map ( sniffer -> sniffer.sniff ( repo, diffEntry ) )
                .filter ( Objects::nonNull )
                .filter ( issues -> ! issues.isEmpty () )
                .map ( issues -> new DiffIssues ( diffEntry, issues ) )
                .collect ( Collectors.toList () );
    }


    private Repository openRepo ()
    {
        try
        {
            return new FileRepositoryBuilder ()
                    .setGitDir ( new File ( repositoryDirectory ) )
                    .readEnvironment () // scan environment GIT_* variables
                    .findGitDir () // scan up the file system tree
                    .build ();
        } catch ( IOException e )
        {
            throw new TrufflerException ( e );
        }
    }


}
