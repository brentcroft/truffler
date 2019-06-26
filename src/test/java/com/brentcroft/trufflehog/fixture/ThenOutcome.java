package com.brentcroft.trufflehog.fixture;

import com.brentcroft.trufflehog.Truffler;
import com.brentcroft.trufflehog.receiver.TxtReceiver;
import com.brentcroft.trufflehog.receiver.XmlReceiver;
import com.tngtech.jgiven.Stage;
import com.tngtech.jgiven.annotation.ExpectedScenarioState;
import lombok.Getter;
import lombok.extern.java.Log;

import java.util.List;

import static org.fest.assertions.api.Assertions.assertThat;

@Getter
@Log
public class ThenOutcome extends Stage< ThenOutcome >
{
    @ExpectedScenarioState
    Truffler truffler;

    @ExpectedScenarioState
    XmlReceiver xmlReceiver;

    @ExpectedScenarioState
    TxtReceiver txtReceiver;

    public ThenOutcome report_is_created()
    {
        assertThat( xmlReceiver ).isNotNull();

        String report = xmlReceiver.serialize();

        assertThat( report ).isNotNull();

        return self();
    }

    public ThenOutcome txt_report_contains_$_commits( int numCommits )
    {
        assertThat(txtReceiver).isNotNull();

        List<String> received = txtReceiver.getReceived();

        assertThat(received.size()).isEqualTo( numCommits );

        return self();
    }

    public ThenOutcome txt_report_commit_$_contains_text( int i, String... text )
    {
        assertThat(txtReceiver).isNotNull();

        List<String> received = txtReceiver.getReceived();

        String commitText = received.get( i );

        for ( String t : text)
        {
            assertThat(commitText).contains( t );
        }

        return self();
    }
}
