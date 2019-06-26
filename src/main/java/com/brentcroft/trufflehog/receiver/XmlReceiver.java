package com.brentcroft.trufflehog.receiver;


import com.brentcroft.trufflehog.model.CommitIssues;
import com.brentcroft.trufflehog.model.Receiver;
import com.brentcroft.trufflehog.util.TrufflerException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import static java.lang.String.format;


@RequiredArgsConstructor
@ToString
@Getter
public class XmlReceiver implements Receiver
{
    public static final SimpleDateFormat SDF = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" );

    private final String filename;
    private Document document;
    private Element element;

    @Override
    public void open()
    {
        try
        {
            document = DocumentBuilderFactory
                    .newInstance()
                    .newDocumentBuilder()
                    .newDocument();

            element = document.createElement( "truffle" );
            element.setAttribute( "created", SDF.format( new Date() ) );

            document.appendChild( element );

        } catch( ParserConfigurationException e )
        {
            throw new TrufflerException( e );
        }
    }


    @Override
    public void receive( CommitIssues commitIssues )
    {
        Element ciElement = document.createElement( "commit" );

        RevCommit commit = commitIssues.getCommit();

        ciElement.setAttribute( "sha", commit.getName() );

        PersonIdent ident = commit.getCommitterIdent();
        ciElement.setAttribute( "date", SDF.format( ident.getWhen() ) );
        ciElement.setAttribute( "from", format( "%s", ident.getName() ) );


        commitIssues
                .getDiffIssues()
                .forEach( di -> {
                    Element diElement = document.createElement( "diff" );


                    String newPath = di.getDiffEntry().getNewPath();
                    String oldPath = di.getDiffEntry().getOldPath();

                    if( newPath.equals( oldPath ) )
                    {
                        diElement.setAttribute( "path", newPath );
                    }
                    else
                    {
                        if( ! "/dev/null".equals( newPath ) )
                        {
                            diElement.setAttribute( "new-path", newPath );
                        }
                        if( ! "/dev/null".equals( oldPath ) )
                        {
                            diElement.setAttribute( "path", oldPath );
                        }
                    }
                    ciElement.appendChild( diElement );

                    di
                            .getIssues()
                            .forEach( issue -> {

                                Element iElement = document.createElement( issue.getTag() );
                                Text iText = document.createTextNode( issue.getText() );

                                iElement.appendChild( iText );
                                diElement.appendChild( iElement );

                                issue
                                        .getAttributes()
                                        .forEach( ( k, v ) -> {
                                            if( v instanceof Double )
                                            {
                                                iElement.setAttribute( k, format( "%.4f", v ) );
                                            }
                                            else
                                            {
                                                iElement.setAttribute( k, format( "%s", v ) );
                                            }
                                        } );
                            } );

                } );

        element.appendChild( ciElement );
    }

    @Override
    public void close()
    {
        //never closes
    }

    public String serialize()
    {
        try
        {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();

            transformer.setOutputProperty( OutputKeys.INDENT, "yes" );
            transformer.setOutputProperty( "{http://xml.apache.org/xslt}indent-amount", "2" );

            StreamResult result = new StreamResult( new StringWriter() );

            DOMSource source = new DOMSource( document );
            transformer.transform( source, result );

            String xmlText = result.getWriter().toString();

            if( ! ( filename == null || filename.isEmpty() ) )
            {
                try
                {
                    FileWriter fw = new FileWriter( filename );

                    fw.write( xmlText );

                    fw.close();

                } catch( IOException e )
                {
                    throw new TrufflerException( e );
                }
            }

            return xmlText;

        } catch( TransformerException e )
        {
            throw new TrufflerException( e );
        }
    }
}
