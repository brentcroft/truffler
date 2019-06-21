package com.brentcroft.trufflehog;


import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import static java.lang.String.format;


@RequiredArgsConstructor
@ToString
@Getter
public class XmlReceiver implements Truffler.Receiver
{
    public static final SimpleDateFormat SDF = new SimpleDateFormat ( "yyyy-MM-dd HH:mm:ss" );

    private final String filename;
    private Document document;
    private Element element;

    @Override
    public void open ( )
    {
        try
        {
            document = DocumentBuilderFactory
                    .newInstance ()
                    .newDocumentBuilder ()
                    .newDocument ();

            element = document.createElement ( "truffle" );
            element.setAttribute ( "created", SDF.format ( new Date () ) );

            document.appendChild ( element );

        } catch ( ParserConfigurationException e )
        {
            throw new TrufflerException ( e );
        }
    }


    @Override
    public void receive ( Truffler.CommitIssues commitIssues )
    {
        Element ciElement = document.createElement ( "commit" );

        RevCommit commit = commitIssues.getCommit ();

        ciElement.setAttribute ( "sha", commit.getName () );

        PersonIdent ident = commit.getCommitterIdent ();
        ciElement.setAttribute ( "date", SDF.format ( ident.getWhen () ) );
        ciElement.setAttribute ( "from", format( "%s", ident.getName () ) );


        commitIssues
                .getDiffIssues ()
                .forEach ( di -> {
                    Element diElement = document.createElement ( "diff" );

                    diElement.setAttribute ( "path", di.getDiffEntry ().getOldPath () );
                    ciElement.appendChild ( diElement );

                    di
                            .getIssues ()
                            .forEach ( i -> {

                                Element iElement = document.createElement ( i.getTag () );
                                Text iText = document.createTextNode ( i.getText () );

                                iElement.appendChild ( iText );
                                diElement.appendChild ( iElement );

                                i
                                        .getAttributes ()
                                        .forEach ( ( k, v ) -> {
                                            iElement.setAttribute ( k, format("%s",v) );
                                        } );

                            } );

                } );

        element.appendChild ( ciElement );
    }

    @Override
    public void close ()
    {
    }

    public String serialize()
    {
        try
        {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();

            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

            //initialize StreamResult with File object to save to file
            StreamResult result = new StreamResult(new StringWriter ());
            DOMSource source = new DOMSource(document);
            transformer.transform(source, result);
            return result.getWriter().toString();
        }
        catch ( TransformerException e )
        {
            throw new TrufflerException ( e );
        }
    }
}
