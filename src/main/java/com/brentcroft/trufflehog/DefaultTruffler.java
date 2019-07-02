package com.brentcroft.trufflehog;

import com.brentcroft.trufflehog.receiver.XmlReceiver;
import com.brentcroft.trufflehog.sniffer.EntropySniffer;
import com.brentcroft.trufflehog.sniffer.RegexSniffer;
import com.brentcroft.trufflehog.util.Local;
import lombok.Getter;
import lombok.extern.java.Log;
import org.w3c.dom.Element;

import java.util.List;
import java.util.logging.Level;

import static java.lang.String.format;

@Getter
@Log
public class DefaultTruffler extends Truffler
{
    private static final String ENTROPY_JSON_CHAR_BASES = "truffler/entropy-char-bases.json";
    private static final String ENTROPY_KNOWN_STRINGS = "truffler/entropy-known-strings.txt";
    private static final String REGEX_RULES_JSON = "truffler/regex-rules.json";
    private static final String XSL_OUT_FILENAME = "truffler/html-generator.xsl";

    private static final String XML_OUT_FILENAME = "target/truffler/truffles.xml";

    protected EntropySniffer entropySniffer = new EntropySniffer();
    protected RegexSniffer regexSniffer = new RegexSniffer();
    protected XmlReceiver xmlReceiver = new XmlReceiver();


    public DefaultTruffler()
    {
        setMaxDepth( 20 );

        List< String > knownStrings = null;

        try
        {
            knownStrings = Local.getFileLines( ENTROPY_KNOWN_STRINGS );
        }
        catch (Exception e)
        {
            log.log( Level.WARNING, "", e );
        }


        getSniffers()
                .add(
                        entropySniffer
                                .withKnownStrings( knownStrings )
                                .withJsonCharBases( Local.getFileText( ENTROPY_JSON_CHAR_BASES ) ) );

        getSniffers()
                .add(
                        regexSniffer
                                .withRegexJsonText( Local.getFileText( REGEX_RULES_JSON ) ) );

        getReceivers()
                .add(
                        xmlReceiver
                                .withXmlFilename( XML_OUT_FILENAME )
                                .withXslUri( XSL_OUT_FILENAME ) );


        xmlReceiver.withCloseListener( xr -> {

            Element entropyElement = xmlReceiver.getDocument().createElement( "entropy" );

            xmlReceiver.getDocument().getDocumentElement().appendChild( entropyElement );

            {
                Element charBasesElement = xmlReceiver.getDocument().createElement( "char-bases" );
                entropyElement.appendChild( charBasesElement );

                entropySniffer
                        .getCharBases()
                        .forEach( cb -> {
                            Element charBaseElement = xmlReceiver.getDocument().createElement( cb.getName() );
                            charBasesElement.appendChild( charBaseElement );

                            charBaseElement.setAttribute( "threshold", format( "%s", cb.getThreshold() ) );
                            charBaseElement.setAttribute( "maxLength", format( "%s", cb.getMaxLength() ) );

                            charBaseElement.appendChild( xmlReceiver.getDocument().createCDATASection( cb.getCharset() ) );
                        } );
            }
            {
                Element knownStringsElement = xmlReceiver.getDocument().createElement( "known-strings" );
                entropyElement.appendChild( knownStringsElement );

                knownStringsElement
                        .appendChild(
                                xmlReceiver
                                        .getDocument()
                                        .createCDATASection( String.join( "\n", entropySniffer.getKnownStrings() ) ) );
            }
        } );
    }
}
