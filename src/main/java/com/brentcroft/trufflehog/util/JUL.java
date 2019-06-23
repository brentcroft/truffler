package com.brentcroft.trufflehog.util;

import java.util.Date;
import java.util.logging.*;

public class JUL
{
    private static final String format = "[%1$tF %1$tT] %3$s %n";

    private static final SimpleFormatter formatter = new SimpleFormatter () {

        @Override
        public synchronized String format(LogRecord lr) {
            return String.format(format,
                    new Date (lr.getMillis()),
                    lr.getLevel().getLocalizedName(),
                    lr.getMessage()
            );
        }
    };

    public static void install()
    {
        try
        {
            for ( Handler h : Logger.getLogger ( "" ).getHandlers () )
            {
                h.setFormatter ( formatter );
            }

            Logger.getLogger ( JUL.class.getName () ).info ( "installed" );
        }
        catch (Throwable e)
        {
            Logger.getLogger ( JUL.class.getName () ).log ( Level.WARNING,e.getMessage (), e);
        }
    }
}
