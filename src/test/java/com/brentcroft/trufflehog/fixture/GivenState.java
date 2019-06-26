package com.brentcroft.trufflehog.fixture;

import com.brentcroft.trufflehog.Truffler;
import com.brentcroft.trufflehog.receiver.LoggingReceiver;
import com.brentcroft.trufflehog.receiver.TxtReceiver;
import com.brentcroft.trufflehog.receiver.XmlReceiver;
import com.brentcroft.trufflehog.sniffer.EntropySniffer;
import com.brentcroft.trufflehog.sniffer.RegexSniffer;
import com.brentcroft.trufflehog.util.TrufflerException;
import com.tngtech.jgiven.Stage;
import com.tngtech.jgiven.annotation.ProvidedScenarioState;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.IntStream;

import static java.lang.String.format;


public class GivenState extends Stage< GivenState >
{
    @ProvidedScenarioState
    Truffler truffler;

    @ProvidedScenarioState
    XmlReceiver xmlReceiver;

    @ProvidedScenarioState
    TxtReceiver txtReceiver;


    @ProvidedScenarioState
    EntropySniffer entropySniffer;

    @ProvidedScenarioState
    RegexSniffer regexSniffer;


    @ProvidedScenarioState
    private String firstCommitId;

    @ProvidedScenarioState
    private List< String > commitIds = new ArrayList<>();


    private Path tempGitPath;

    public void removeTemporaryFiles()
    {
        if( Objects.nonNull( tempGitPath ) )
        {
            try
            {
                Files
                        .walk( tempGitPath )
                        .sorted( Comparator.reverseOrder() )
                        .map( Path::toFile )
                        .forEach( File::delete );

                System.out.println( "Cleaned up temporary directory" );
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


    public GivenState a_temp_git_directory()
    {
        if( tempGitPath != null )
        {
            throw new RuntimeException( "tempGitPath already exists" );
        }
        try
        {
            tempGitPath = Files.createTempDirectory( "truffler" );

            File gitDir = new File( tempGitPath.toFile(), ".git" );

            Repository repo = FileRepositoryBuilder.create( gitDir );

            repo.create();

        } catch( IOException e )
        {
            throw new RuntimeException( e );
        }

        git_directory( tempGitPath.toFile().getAbsolutePath() );

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

    public GivenState a_log_receiver()
    {
        truffler.getReceivers().add( new LoggingReceiver() );

        return self();
    }


    public GivenState a_text_receiver()
    {
        txtReceiver = new TxtReceiver();

        truffler.getReceivers().add( txtReceiver );

        return self();
    }

    public GivenState writes_xml_report_to( String filename )
    {
        xmlReceiver = new XmlReceiver( filename );

        truffler.getReceivers().add( xmlReceiver );

        return self();
    }

    public GivenState an_entropy_sniffer()
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

    public GivenState a_regex_sniffer( String jsonRegexPath )
    {
        regexSniffer = new RegexSniffer().withJsonRegexFile( jsonRegexPath );

        truffler.getSniffers().add( regexSniffer );

        return self();
    }

    public GivenState a_regex_sniffer( String... pairs )
    {
        if( Objects.isNull( pairs ) || pairs.length % 2 != 0 )
        {
            throw new RuntimeException( "pairs must have an even number of strings" );
        }

        Map< String, String > regexes = new HashMap<>();

        IntStream
                .range( 0, pairs.length / 2 )
                .forEach( i -> regexes.put( pairs[ i * 2 ], pairs[ i * 2 + 1 ] ) );

        regexSniffer = new RegexSniffer().withRegex( regexes );

        truffler.getSniffers().add( regexSniffer );

        return self();
    }

    public GivenState to_earliest_commit( int commitIndex )
    {
        if ( commitIndex >= commitIds.size())
        {
            throw new RuntimeException( format("commit index [%s] is not less than commitIds.size [%s]", commitIndex, commitIds.size()) );
        }

        truffler.setEarliestCommitId( commitIds.get( commitIndex ) );

        return self();
    }


    private void writeFile( String filename, String data )
    {
        File newFile = new File( tempGitPath.toFile(), filename );

        try( PrintWriter out = new PrintWriter( newFile ) )
        {
            out.println( data );
            out.flush();
        } catch( FileNotFoundException e )
        {
            throw new RuntimeException( e );
        }
    }


    public GivenState an_initial_commit_of( String filename, String data )
    {
        if( firstCommitId != null )
        {
            throw new RuntimeException( "firstCommitId already exists" );
        }

        try
        {
            writeFile( filename, data );

            Repository repo = truffler.openRepo();

            Git git = new Git( repo );

            git.add().addFilepattern( filename ).call();

            RevCommit rev = git
                    .commit()
                    .setAuthor( "tul_urte", "tul_urte@brentcroft.com" )
                    .setMessage( "Initial commit" )
                    .call();

            firstCommitId = rev.getId().getName();

            commitIds.add( firstCommitId );

        } catch( Exception e )
        {
            throw new RuntimeException( e );
        }

        return self();
    }

    public GivenState another_commit_of( String filename, String data )
    {
        try
        {
            writeFile( filename, data );

            Repository repo = truffler.openRepo();

            Git git = new Git( repo );

            git.add().addFilepattern( filename ).call();

            RevCommit rev = git
                    .commit()
                    .setAuthor( "tul_urte", "tul_urte@brentcroft.com" )
                    .setMessage( "Another commit" )
                    .call();

            commitIds.add( rev.getId().getName() );

        } catch( Exception e )
        {
            throw new RuntimeException( e );
        }

        return self();
    }

    public GivenState ignored_strings( String... knownStrings )
    {
        entropySniffer.getKnownStrings().addAll( Arrays.asList( knownStrings ) );

        return self();
    }
}
