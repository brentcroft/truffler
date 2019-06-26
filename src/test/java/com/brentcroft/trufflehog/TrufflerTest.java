package com.brentcroft.trufflehog;

import com.brentcroft.trufflehog.fixture.GivenState;
import com.brentcroft.trufflehog.fixture.ThenOutcome;
import com.brentcroft.trufflehog.fixture.WhenAction;
import com.tngtech.jgiven.annotation.Hidden;
import com.tngtech.jgiven.junit.ScenarioTest;
import org.junit.After;
import org.junit.Test;

public class TrufflerTest extends ScenarioTest< GivenState, WhenAction, ThenOutcome >
{
    String localProject = ".";

    @After
    @Hidden
    public void cleanUp()
    {
        given().removeTemporaryFiles();
    }

    @Test
    public void truffles()
    {
        given()
                .a_truffler()
                //.to_earliest_commit("ed5f93ae58b102719a4fb0b685e9f0ea472475e9")
                .max_depth( 10000 )
                .git_directory( localProject )
                .entropy_sniffer()
                .entropy_base64_threshold( 4 )
                .entropy_hex_threshold( 1 )
                .a_regex_sniffer( "regex.json" )
                .a_log_receiver()
                .writes_xml_report_to( "truffler-report.xml" );

        when()
                .truffle();

        then()
                .report_is_created();
    }

    @Test
    public void detects_first_commit()
    {
        given()
                .a_truffler()
                .temp_git_directory( )
                .an_initial_commit_of("commit-01.txt","after many a summer, summer, summer")
                .a_regex_sniffer( "t1", "su[m]{2}er" )
                .a_text_receiver()
                .a_log_receiver();

        when()
                .truffle();

        then()
                .txt_report_contains_$_commits( 1 )
                .txt_report_commit_$_contains_text(0, "issues=[1]", "count=3");
    }




    @Test
    public void detects_another_commit()
    {
        given()
                .a_truffler()
                .temp_git_directory( )
                .an_initial_commit_of("commit-01.txt","after many a summer")
                .another_commit_of("commit-02.txt","the leaves were tired")
                .a_regex_sniffer(
                        "t1", "su[m]{2}er",
                        "t2", "lea[v]es"
                )
                .a_text_receiver()
                .a_log_receiver();

        when()
                .truffle();


        then()
                .txt_report_contains_$_commits( 2 )
                .txt_report_commit_$_contains_text(0, "issues=[1]", "count=1")
                .txt_report_commit_$_contains_text(1, "issues=[1]", "count=1");
    }

    @Test
    public void detects_another_overwriting_commit()
    {
        given()
                .a_truffler()
                .temp_git_directory( )
                .an_initial_commit_of("commit-01.txt","after many a summer")
                .another_commit_of("commit-01.txt","the leaves were tired")
                .a_regex_sniffer(
                        "t1", "su[m]{2}er",
                        "t2", "lea[v]es"
                )
                .a_text_receiver()
                .a_log_receiver();

        when()
                .truffle();


        then()
                .txt_report_contains_$_commits( 2 )
                .txt_report_commit_$_contains_text(0, "issues=[2]", "count=1")
                .txt_report_commit_$_contains_text(1, "issues=[1]", "count=1");
    }

    @Test
    public void detects_up_to_earliest_commit()
    {
        given()
                .a_truffler()
                .temp_git_directory( )
                .an_initial_commit_of("commit-01.txt","after many a summer")
                .another_commit_of("commit-02.txt","the leaves were tired")
                .another_commit_of("commit-03.txt","the earth was dark")
                .another_commit_of("commit-04.txt","and the sky was grey")
                .a_regex_sniffer(
                        "t1", "su[m]{2}er",
                        "t2", "lea[v]es",
                        "t3", "ear[t]h",
                        "t4", "s[k]y"
                )
                .a_text_receiver()
                .a_log_receiver()
                .to_earliest_commit(1);

        when()
                .truffle();


        then()
                .txt_report_contains_$_commits( 3 )
                .txt_report_commit_$_contains_text(0, "sky")
                .txt_report_commit_$_contains_text(1, "earth")
                .txt_report_commit_$_contains_text(2, "leaves");
    }
}