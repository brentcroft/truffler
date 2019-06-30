package com.brentcroft.trufflehog;

import com.brentcroft.trufflehog.receiver.XmlReceiver;
import com.brentcroft.trufflehog.sniffer.EntropySniffer;
import com.brentcroft.trufflehog.sniffer.RegexSniffer;
import com.brentcroft.trufflehog.util.Local;
import org.w3c.dom.Element;

import java.util.stream.Collectors;

import static java.lang.String.format;

public class DefaultTruffler extends Truffler
{
    private static final String ENTROPY_JSON_CHAR_BASES = "truffler/entropy-char-bases.json";
    private static final String ENTROPY_KNOWN_STRINGS = "truffler/entropy-known-strings.txt";
    private static final String REGEX_RULES_JSON = "truffler/regex-rules.json";
    private static final String XSL_OUT_FILENAME = "truffler/html-generator.xsl";

    private static final String XML_OUT_FILENAME = "target/truffler/truffles.xml";


    public DefaultTruffler()
    {
        EntropySniffer entropySniffer = new EntropySniffer();

        {
             entropySniffer
                    .withKnownStrings( Local.getFileLines( ENTROPY_KNOWN_STRINGS ) )
                    .withJsonCharBases( Local.getFileText( ENTROPY_JSON_CHAR_BASES ) );

            getSniffers().add( entropySniffer );
        }

        {
            RegexSniffer regexSniffer = new RegexSniffer();

            regexSniffer.withJsonRegexText( Local.getFileText( REGEX_RULES_JSON ) );

            getSniffers().add( regexSniffer );
        }

        XmlReceiver xmlReceiver = new XmlReceiver( XML_OUT_FILENAME );

        {
            xmlReceiver.setXsltUri( XSL_OUT_FILENAME );

            getReceivers().add( xmlReceiver );
        }

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

                            charBaseElement.appendChild( xmlReceiver.getDocument().createCDATASection(cb.getCharset() ) );
                        });
            }
            {
                Element knownStringsElement = xmlReceiver.getDocument().createElement( "known-strings" );
                entropyElement.appendChild( knownStringsElement );

                knownStringsElement
                        .appendChild(
                                xmlReceiver
                                        .getDocument()
                                        .createCDATASection(
                                                entropySniffer
                                                        .getKnownStrings()
                                                        .stream()
                                                        .collect( Collectors.joining( "\n" ) ) ) );

            }
        } );
    }
}
