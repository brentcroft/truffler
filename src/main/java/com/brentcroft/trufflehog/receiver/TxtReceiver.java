package com.brentcroft.trufflehog.receiver;

import com.brentcroft.trufflehog.model.CommitIssues;
import lombok.Getter;
import lombok.extern.java.Log;

import java.util.ArrayList;
import java.util.List;

@Log
@Getter
public class TxtReceiver implements Receiver
{
    private final List< String > received = new ArrayList<>();

    @Override
    public void receive( CommitIssues commitIssues )
    {
        received.add( commitIssues.toString() );
    }
}
