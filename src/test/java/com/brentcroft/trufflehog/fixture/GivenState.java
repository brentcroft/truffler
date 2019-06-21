package com.brentcroft.trufflehog.fixture;

import com.brentcroft.trufflehog.EntropySniffer;
import com.brentcroft.trufflehog.Truffler;
import com.brentcroft.trufflehog.XmlReceiver;
import com.tngtech.jgiven.Stage;
import com.tngtech.jgiven.annotation.ProvidedScenarioState;




public class GivenState extends Stage< GivenState >
{
    @ProvidedScenarioState
    Truffler truffler;

    @ProvidedScenarioState
    Truffler.Receiver receiver;

    @ProvidedScenarioState
    Truffler.Sniffer entropySniffer;

    public GivenState a_truffler( )
    {
        truffler = new Truffler ();

        return self ();
    }


    public GivenState git_directory ( String pathToGitDirectory )
    {
        truffler.setRepositoryDirectory ( pathToGitDirectory );

        return self ();
    }

    public GivenState max_depth ( int depth )
    {
        truffler.setMaxDepth ( depth );

        return self ();
    }

    public GivenState writes_xml_report_to ( String filename )
    {
        receiver =  new XmlReceiver ( filename );
        truffler.getReceivers ().add ( receiver );

        return self ();
    }

    public GivenState entropy_sniffer ()
    {
        entropySniffer = new EntropySniffer();

        truffler.getSniffers ().add ( entropySniffer );

        return self ();
    }


    public GivenState entropy_base64_threshold ( double base64T)
    {
        EntropySniffer.BASE64_THRESHOLD = base64T;

        return self ();
    }

    public GivenState entropy_hex_threshold ( double hexT)
    {
        EntropySniffer.HEX_THRESHOLD = hexT;

        return self ();
    }
}
