package com.brentcroft.trufflehog;

import com.brentcroft.trufflehog.fixture.GivenState;
import com.brentcroft.trufflehog.fixture.ThenOutcome;
import com.brentcroft.trufflehog.fixture.WhenAction;
import com.tngtech.jgiven.junit.ScenarioTest;
import org.junit.Test;

public class DefaultTrufflerTest extends ScenarioTest< GivenState, WhenAction, ThenOutcome >
{

    @Test
    public void truffles_by_default()
    {
        given()
                .a_default_truffler();

        when()
                .truffle();

        then()
                .report_is_created();
    }
}