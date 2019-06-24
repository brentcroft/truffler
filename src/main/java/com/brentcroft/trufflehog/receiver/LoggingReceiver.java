package com.brentcroft.trufflehog.receiver;

import com.brentcroft.trufflehog.model.CommitIssues;
import com.brentcroft.trufflehog.model.Receiver;
import lombok.extern.java.Log;

import static java.lang.String.format;

@Log
public class LoggingReceiver implements Receiver
{

    @Override
    public void receive( CommitIssues commitIssues )
    {
        log.info(
                format(
                        "%s", commitIssues
                )
        );
    }
}
