package com.brentcroft.trufflehog.fixture;

import com.brentcroft.trufflehog.Truffler;
import com.tngtech.jgiven.Stage;
import com.tngtech.jgiven.annotation.ExpectedScenarioState;
import lombok.extern.java.Log;


@Log
public class WhenAction extends Stage< WhenAction >
{
    @ExpectedScenarioState
    Truffler truffler;

    public void truffle_repository ()
    {
        truffler.truffle();
    }
}
