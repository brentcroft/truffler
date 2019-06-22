package com.brentcroft.trufflehog.fixture;

import com.brentcroft.trufflehog.Truffler;
import com.tngtech.jgiven.Stage;
import com.tngtech.jgiven.annotation.ExpectedScenarioState;
import com.tngtech.jgiven.annotation.ProvidedScenarioState;
import lombok.extern.java.Log;


@Log
public class WhenAction extends Stage< WhenAction >
{
    @ExpectedScenarioState
    Truffler truffler;

    @ExpectedScenarioState
    Truffler.Receiver receiver;

    @ProvidedScenarioState
    String reportSerialization;

    public void truffle ()
    {
        truffler.truffle();

        reportSerialization = receiver.serialize();
    }
}
