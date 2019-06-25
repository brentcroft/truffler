package com.brentcroft.trufflehog.fixture;

import com.brentcroft.trufflehog.Truffler;
import com.brentcroft.trufflehog.model.Receiver;
import com.brentcroft.trufflehog.receiver.LoggingReceiver;
import com.brentcroft.trufflehog.receiver.XmlReceiver;
import com.brentcroft.trufflehog.sniffer.EntropySniffer;
import com.brentcroft.trufflehog.sniffer.RegexSniffer;
import com.brentcroft.trufflehog.util.TrufflerException;
import com.tngtech.jgiven.Stage;
import com.tngtech.jgiven.annotation.ProvidedScenarioState;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.junit.After;


import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Objects;


public class GivenState extends Stage< GivenState >
{
    @ProvidedScenarioState
    Truffler truffler;

    @ProvidedScenarioState
    Receiver receiver;

    @ProvidedScenarioState
    EntropySniffer entropySniffer;

    @ProvidedScenarioState
    RegexSniffer regexSniffer;


    @ProvidedScenarioState
    private String firstCommitId;


    private Path tempGitPath;

    @After
    public void removeTemporaryFiles()
    {
        if ( Objects.nonNull(tempGitPath))
        {
            try
            {
                Files
                        .walk(tempGitPath)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            } catch( IOException e )
            {
                throw new TrufflerException( e );
            }
        }
    }


    public GivenState a_truffler()
    {
        truffler = new Truffler();

        return self();
    }


    public GivenState temp_git_directory( )
    {
        if ( tempGitPath != null)
        {
            throw new RuntimeException( "tempGitPath already exists" );
        }
        try
        {
            Path tempDir = Files.createTempDirectory( "truffler" );
            File gitDir = new File( tempDir.toFile(), ".git" );

            tempGitPath = gitDir.toPath();

            Repository repo = FileRepositoryBuilder.create( gitDir );

            repo.create();

        } catch( IOException e )
        {
            throw new RuntimeException( e );
        }

        git_directory(tempGitPath.toFile().getAbsolutePath());

        return self();
    }



    public GivenState git_directory( String pathToGitDirectory )
    {
        truffler.setRepositoryDirectory( pathToGitDirectory );

        return self();
    }

    public GivenState max_depth( int depth )
    {
        truffler.setMaxDepth( depth );

        return self();
    }

    public GivenState logs_report()
    {
        truffler.getReceivers().add( new LoggingReceiver() );

        return self();
    }

    public GivenState writes_xml_report_to( String filename )
    {
        receiver = new XmlReceiver( filename );

        truffler.getReceivers().add( receiver );

        return self();
    }

    public GivenState entropy_sniffer()
    {
        entropySniffer = new EntropySniffer();

        truffler.getSniffers().add( entropySniffer );

        return self();
    }


    public GivenState entropy_base64_threshold( double base64T )
    {
        EntropySniffer.BASE64_THRESHOLD = base64T;

        return self();
    }

    public GivenState entropy_hex_threshold( double hexT )
    {
        EntropySniffer.HEX_THRESHOLD = hexT;

        return self();
    }

    public GivenState regex_sniffer( String jsonRegexPath )
    {
        regexSniffer = new RegexSniffer()
                .withJsonRegexFile( jsonRegexPath );

        truffler.getSniffers().add( regexSniffer );

        return self();
    }

    public GivenState earliest_commit( String earliestCommitId)
    {
        truffler.setEarliestCommitId( earliestCommitId );

        return self();
    }

    public GivenState first_commit()
    {
        if ( firstCommitId != null)
        {
            throw new RuntimeException( "firstCommitId already exists" );
        }

        String filename = "first-commit.txt";

        try
        {
            Repository repo = truffler.openRepo();

            Git git = new Git( repo );

            File newFile = new File( tempGitPath.toFile(), filename );

            try (PrintWriter out = new PrintWriter(newFile)) {
                out.println("testing testing 123");
            }

            git.add().addFilepattern( filename ).call();

            RevCommit rev = git
                    .commit()
                    .setAuthor( "tul_urte", "tul_urte@brentcroft.com" )
                    .setMessage( "First commit" )
                    .call();

            firstCommitId = rev.getId().getName();

        }
        catch (Exception e)
        {
            throw new RuntimeException( e );
        }

        return self();
    }
}
