package com.brentcroft.trufflehog.fixture;

import com.brentcroft.trufflehog.Truffler;
import com.tngtech.jgiven.Stage;
import com.tngtech.jgiven.annotation.ExpectedScenarioState;
import lombok.Getter;
import lombok.extern.java.Log;

import java.util.List;

import static java.lang.String.format;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

@Getter
@Log
public class ThenOutcome extends Stage< ThenOutcome >
{

    @ExpectedScenarioState
    Truffler truffler;

    @ExpectedScenarioState
    List< String > localBranches;


    public void repository_is_open ()
    {
        assertNotNull ( truffler );
    }

    public void there_are_local_branches ()
    {
        assertThat ( localBranches )
                .isNotNull ()
                .isNotEmpty ();

        log.info ( format ( "Local branches: %s", localBranches ) );
    }
}
