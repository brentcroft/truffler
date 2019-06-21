package com.brentcroft.trufflehog.fixture;

import com.brentcroft.trufflehog.Truffler;
import com.tngtech.jgiven.Stage;
import com.tngtech.jgiven.annotation.ProvidedScenarioState;
import lombok.Getter;
import lombok.RequiredArgsConstructor;


public class GivenState extends Stage< GivenState >
{
    @ProvidedScenarioState
    Truffler truffler = new Truffler ();

    @ProvidedScenarioState
    Walker walker;

    @RequiredArgsConstructor
    @Getter
    class Walker
    {
        private final int depth;
    }


    public GivenState repository_directory ( String pathToGitDirectory )
    {
        truffler.setRepositoryDirectory ( pathToGitDirectory );

        return self ();
    }

    public GivenState walker_of_depth ( int depth )
    {
        truffler.setMaxDepth ( depth );

        return self ();
    }
}
