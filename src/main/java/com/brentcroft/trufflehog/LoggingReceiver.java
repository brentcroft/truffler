package com.brentcroft.trufflehog;

import lombok.extern.java.Log;

import static java.lang.String.format;

@Log
public class LoggingReceiver implements Truffler.Receiver
{

    @Override
    public void receive ( Truffler.CommitIssues commitIssues )
    {
        log.info (
                format (
                        "%s", commitIssues
                )
        );
    }


}
