package com.brentcroft.trufflehog;

import com.brentcroft.trufflehog.fixture.GivenState;
import com.brentcroft.trufflehog.fixture.ThenOutcome;
import com.brentcroft.trufflehog.fixture.WhenAction;
import com.tngtech.jgiven.junit.ScenarioTest;
import org.junit.Test;

public class TrufflerTest extends ScenarioTest< GivenState, WhenAction, ThenOutcome >
{
    String lucidGit = "D:/object-detection/lucid/.git";

    String localProject = "./.git";

    @Test
    public void truffles()
    {
        given()
                .a_truffler()
                //.earliest_commit("ed5f93ae58b102719a4fb0b685e9f0ea472475e9")
                .max_depth( 10000 )
                .git_directory( localProject )
                .entropy_sniffer()
                .entropy_base64_threshold( 4 )
                .entropy_hex_threshold( 1 )
                .regex_sniffer( "regex.json" )
                //.logs_report ()
                .writes_xml_report_to( "truffler-report.xml" );

        when()
                .truffle();

        then()
                .report_is_created();
    }

    @Test
    public void handles_first_commit()
    {
        given()
            .a_truffler()
            .temp_git_directory( )
            .first_commit()
            .entropy_sniffer()
            .logs_report ()
            .writes_xml_report_to( "truffler-report.xml" );

        when()
                .truffle();
    }
}