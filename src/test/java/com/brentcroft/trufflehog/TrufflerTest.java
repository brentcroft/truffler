package com.brentcroft.trufflehog;

import com.brentcroft.trufflehog.fixture.GivenState;
import com.brentcroft.trufflehog.fixture.ThenOutcome;
import com.brentcroft.trufflehog.fixture.WhenAction;
import com.tngtech.jgiven.junit.ScenarioTest;
import org.junit.Test;

public class TrufflerTest extends ScenarioTest< GivenState, WhenAction, ThenOutcome >
{

    String tf = "D:/object-detection/tensorflow/.git";

    @Test
    public void walks_commits () throws Exception
    {
        given ()
                .walker_of_depth( 6 )
                .repository_directory ( "./.git" );

        when ()
                .truffle_repository ();

        then ()
                .repository_is_open ();

    }
}