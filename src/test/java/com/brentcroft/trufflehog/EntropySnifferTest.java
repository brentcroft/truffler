package com.brentcroft.trufflehog;

import com.brentcroft.trufflehog.fixture.GivenState;
import com.brentcroft.trufflehog.fixture.ThenOutcome;
import com.brentcroft.trufflehog.fixture.WhenAction;
import com.tngtech.jgiven.annotation.Hidden;
import com.tngtech.jgiven.junit.ScenarioTest;
import org.junit.After;
import org.junit.Test;

public class EntropySnifferTest extends ScenarioTest< GivenState, WhenAction, ThenOutcome >
{
    @After
    @Hidden
    public void cleanUp()
    {
        given().removeTemporaryFiles();
    }


    @Test
    public void sniffs_entropy()
    {
        given()
                .a_truffler()
                .a_temp_git_directory()
                .an_entropy_sniffer("truffler/entropy-char-bases.json")
//                .a_log_receiver()
                .a_text_receiver()
                .an_initial_commit_of( "commit-001.txt", "thequickbrownfoxjumpedovertheelephant1234567890" )
                .another_commit_of( "commit-002.txt", "abcdefghijklmnopqrstuvwxyz0123456789" );

        when()
                .truffle();

        then()
                .txt_report_contains_$_commits( 2 );
    }

    @Test
    public void ignores_known_strings()
    {
        given()
                .a_truffler()
                .a_temp_git_directory()
                .an_entropy_sniffer("truffler/entropy-char-bases.json")
                .ignored_strings("abcdefghijklmnopqrstuvwxyz0123456789")
//                .a_log_receiver()
                .a_text_receiver()
                .an_initial_commit_of( "commit-001.txt", "thequickbrownfoxjumpedovertheelephant1234567890" )
                .another_commit_of( "commit-002.txt", "abcdefghijklmnopqrstuvwxyz0123456789" );

        when()
                .truffle();

        then()
                .txt_report_contains_$_commits( 1 );
    }
}