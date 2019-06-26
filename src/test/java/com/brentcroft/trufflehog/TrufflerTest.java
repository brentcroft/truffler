package com.brentcroft.trufflehog;

import com.brentcroft.trufflehog.fixture.GivenState;
import com.brentcroft.trufflehog.fixture.ThenOutcome;
import com.brentcroft.trufflehog.fixture.WhenAction;
import com.tngtech.jgiven.junit.ScenarioTest;
import org.junit.Test;

public class TrufflerTest extends ScenarioTest< GivenState, WhenAction, ThenOutcome >
{

    @Test
    public void truffles()
    {
        given()
                .a_truffler()
                .max_depth( 10000 )
                .git_directory( "." )
                .an_entropy_sniffer()
                .entropy_base64_threshold( 4 )
                .entropy_hex_threshold( 1 )
                .a_regex_sniffer( "regex.json" )
                //.a_log_receiver()
                .writes_xml_report_to( "truffler-report.xml" );

        when()
                .truffle();

        then()
                .report_is_created();
    }
}