package com.brentcroft.trufflehog.sniffer;

import com.brentcroft.trufflehog.model.Issue;
import com.brentcroft.trufflehog.model.Sniffer;
import com.brentcroft.trufflehog.util.TrufflerException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.Repository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.stream.IntStream;

import static java.lang.String.format;

public class EntropySniffer implements Sniffer
{

    private static final String BASE64_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=@.";
    public static double BASE64_THRESHOLD = 4.5;

    private static final String HEX_CHARS = "1234567890abcdefABCDEF";
    public static double HEX_THRESHOLD = 2.9;

    public static int MIN_LENGTH = 20;

    private Set< String > knownStrings = new HashSet<> ();


    @Override
    public List< Issue > sniff ( Repository repo, DiffEntry diffEntry )
    {

        return investigateEntry ( repo, diffEntry );
    }


    private List< Issue > investigateEntry ( Repository repo, DiffEntry diffEntry )
    {

        String diffEntryText = getDiffEntryText ( repo, diffEntry );

        if ( Objects.isNull ( diffEntryText ) || diffEntryText.isEmpty () )
        {
            return Collections.emptyList ();
        }

        List< Issue > issues = new ArrayList<> ();

        for ( String line : diffEntryText.split ( "\n" ) )
        {
            for ( String word : line.split ( " " ) )
            {
                for ( CharBase charBase : EntropyCharBase.values () )
                {

                    charBase.stringsOfSet ( word, MIN_LENGTH )
                            .stream ()
                            .filter ( text -> ! knownStrings.contains ( text ) )
                            .forEach ( text -> {

                                double entropy = getShannonEntropy ( charBase.getCharset (), text );

                                if ( entropy > charBase.getThreshold () )
                                {
                                    issues.add ( new Issue ( "entropy", text )
                                    {
                                        String TYPE = "op";
                                        String SCORE = "score";
                                        {
                                            getAttributes ().put ( TYPE, charBase.getName () );
                                            getAttributes ().put ( SCORE, entropy );
                                        }

                                        public String toString ()
                                        {
                                            return format (
                                                    "[%s=%.4f] %s, %s",
                                                    charBase.getName (),
                                                    entropy,
                                                    text,
                                                    getDiffEntryText () );
                                        }

                                        public String getDiffEntryText ()
                                        {
                                            return "";
                                        }
                                    } );
                                }
                            } );
                }
            }
        }
        return issues;
    }



    // see: https://stackoverflow.com/questions/3719631/log-to-the-base-2-in-python
    private double mathLog ( double value, double base )
    {
        return Math.log10 ( value ) / Math.log10 ( base );
    }


    private float getShannonEntropy ( String charset, String data )
    {

        if ( Objects.isNull ( data ) || data.length () == 0 )
        {
            return 0;
        }

        float[] entropy = {0};

        IntStream
                .range ( 0, charset.length () )
                .map ( charset::charAt )
                .forEach ( b -> {

                    long occurences = IntStream
                            .range ( 0, data.length () )
                            .filter ( i -> b == data.charAt ( i ) )
                            .count ();

                    if ( occurences > 0 )
                    {
                        double ratio = ( double ) occurences / data.length ();

                        entropy[ 0 ] += ratio * mathLog ( ratio, 2 );
                    }
                } );

        return - 1 * entropy[ 0 ];
    }


    interface CharBase
    {
        String getName ();

        String getCharset ();

        double getThreshold ();

        List< String > stringsOfSet ( String word, int length );
    }

    @RequiredArgsConstructor
    @Getter
    enum EntropyCharBase implements CharBase
    {

        BASE64 ( "b64", BASE64_CHARS, BASE64_THRESHOLD ),
        HEX ( "hex", HEX_CHARS, HEX_THRESHOLD );

        private final String name;
        private final String charset;
        private final double threshold;

        public List< String > stringsOfSet ( String word, int minLength )
        {
            int count = 0;
            StringBuilder letters = new StringBuilder ();
            List< String > strings = new ArrayList<> ();
            for ( char c : word.toCharArray () )
            {
                // include all charset chars
                if ( charset.indexOf ( c ) >= 0 )
                {
                    letters.append ( c );
                    count += 1;
                }
                // if exceeded length then collect and continue
                else if ( count > minLength )
                {
                    strings.add ( letters.toString () );
                    letters.setLength ( 0 );
                    count = 0;
                }
            }
            // if exceeded length then collect
            if ( count > minLength )
            {
                strings.add ( letters.toString () );
            }
            return strings;
        }
    }
}
