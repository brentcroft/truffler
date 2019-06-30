package com.brentcroft.trufflehog.receiver;


import com.brentcroft.trufflehog.model.CommitIssues;
import com.brentcroft.trufflehog.util.Local;
import com.brentcroft.trufflehog.util.TrufflerException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

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

    @Setter
    private String xsltUri;
    private String serialization;

    @Override
    public void open( Map< String, String > attr )
    {
        try
        {
            document = DocumentBuilderFactory
                    .newInstance()
                    .newDocumentBuilder()
                    .newDocument();

            if( Objects.nonNull( xsltUri ) && ! xsltUri.isEmpty() )
            {
                document
                        .appendChild(
                                document
                                        .createProcessingInstruction(
                                                "xml-stylesheet",
                                                format( "type=\"text/xsl\" href=\"%s\"", xsltUri ) ) );
            }

            element = document.createElement( "truffle" );
            element.setAttribute( "created", SDF.format( new Date() ) );

            if( Objects.nonNull( attr ) )
            {
                attr.forEach( ( k, v ) -> element.setAttribute( k, v ) );
            }

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

                    Element textElement = document.createElement( "text" );
                    diElement.appendChild( textElement );

                    textElement.appendChild( document.createCDATASection( di.getDiffText() ) );


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
        preCloseNotification();

        serialization = serialize();
    }

    public interface CloseListener
    {
        void closing( XmlReceiver xmlReceiver );
    }

    private void preCloseNotification()
    {
        closeListeners.forEach( cl -> {
            cl.closing( XmlReceiver.this );
        } );
    }

    private List< CloseListener > closeListeners = new ArrayList<>();

    public XmlReceiver withCloseListener( CloseListener cl )
    {
        closeListeners.add( cl );
        return this;
    }


    private String getTransformResult( Transformer transformer ) throws TransformerException
    {

        StreamResult result = new StreamResult( new StringWriter() );

        DOMSource source = new DOMSource( document );
        transformer.transform( source, result );

        return result.getWriter().toString();
    }


    private void maybeSave( String filename, String text )
    {
        try
        {
            File parentDir = new File( filename ).getParentFile();
            if( ! parentDir.exists() )
            {
                parentDir.mkdirs();
            }

            FileWriter fw = new FileWriter( filename );

            fw.write( text );

            fw.close();

        } catch( IOException e )
        {
            throw new TrufflerException( e );
        }
    }

    public String serialize()
    {
        try
        {
            TransformerFactory factory = TransformerFactory.newInstance();

            Transformer transformer = factory.newTransformer();

            transformer.setOutputProperty( OutputKeys.INDENT, "yes" );
            transformer.setOutputProperty( "{http://xml.apache.org/xslt}indent-amount", "4" );

            String xmlText = getTransformResult( transformer );

            maybeSave( filename, xmlText );


            if( Objects.nonNull( xsltUri ) && ! xsltUri.isEmpty() )
            {
                Templates templates = factory.newTemplates(
                        new StreamSource(
                                new StringReader(
                                        Local.getFileText( xsltUri ) ) ) );

                String xml2Text = getTransformResult( templates.newTransformer() );

                maybeSave( filename.replace( ".xml", ".html" ), xml2Text );
            }


            return xmlText;

        } catch( TransformerException e )
        {
            throw new TrufflerException( e );
        }
    }
}
