package com.brentcroft.trufflehog;

import com.brentcroft.trufflehog.fixture.GivenState;
import com.brentcroft.trufflehog.fixture.ThenOutcome;
import com.brentcroft.trufflehog.fixture.WhenAction;
import com.tngtech.jgiven.junit.ScenarioTest;
import org.junit.Test;

import java.io.File;

public class HistoryRewriterTest extends ScenarioTest< GivenState, WhenAction, ThenOutcome >
{

    @Test
    public void creates_mirror_repository()
    {
        given()
                .a_truffler()
                .git_directory( new File(".git").getAbsolutePath() )
                .clone_mirror("e:/temp/mirror");
    }

}
