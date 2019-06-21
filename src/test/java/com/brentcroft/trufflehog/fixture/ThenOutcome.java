package com.brentcroft.trufflehog.fixture;

import com.brentcroft.trufflehog.Truffler;
import com.tngtech.jgiven.Stage;
import com.tngtech.jgiven.annotation.ExpectedScenarioState;
import lombok.Getter;
import lombok.extern.java.Log;

import static org.fest.assertions.api.Assertions.assertThat;

@Getter
@Log
public class ThenOutcome extends Stage< ThenOutcome >
{

    @ExpectedScenarioState
    Truffler truffler;

    @ExpectedScenarioState
    String reportSerialization;

    public void report_is_created ()
    {
        assertThat(truffler).isNotNull ();

        assertThat ( reportSerialization ).isNotNull ().isNotEmpty ();
    }

}
