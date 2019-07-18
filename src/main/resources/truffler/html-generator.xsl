<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:template match="truffle">
        <html>
            <head>
                <style>
                    body, div, td, p {
                        font-family: "Arial", san-serif;
                        margin: 10px;
                    }
                    .diff {
                        padding-bottom: 10px;
                    }
                    table, textarea {
                        width: 100%;
                    }
                    td {
                        padding: 5px;
                    }
                    td:nth-child(1)
                    {
                        font-weight: bold;
                        width: 30%;
                    }
                    td:nth-child(2)
                    {
                        font-weight: bold;
                        width: 15%;
                    }

                    .commit {
                        font-weight: bold;
                        border-top: 1px solid;
                        padding-bottom: 10px;
                    }

                    .path
                    {
                        border: 1pt dotted black;
                        color: blue;
                        background: whitesmoke;
                        font-weight: normal;
                    }
                    .detail-notes
                    {
                        font-weight: normal;
                        font-style: italic;
                    }
                    .detail-value
                    {
                        background: gold;
                    }
                    .diff-text
                    {
                        background: lemonchiffon;
                    }


                    #knownStrings {
                        width: 100%;
                        height: 100%;
                    }

                    #columns {
                        display: flex;
                    }

                    #leftColumn {
                        flex: 1;
                        padding: 10px;
                        border-top: 1px dotted red;
                        border-left: 1px dotted red;
                    }

                    #rightColumn {
                        flex: 0 0 70%;
                        padding: 10px;
                        border-top: 1px dotted red;
                        border-left: 1px dotted red;
                    }

                    .panel-title
                    {
                        font-weight: bold;
                    }

                    .commit-list {
                        overflow: auto;
                        max-height: 80vh;
                    }
                </style>
                <script>
                    function addKnownString( text )
                    {
                        var ks = document.getElementById( "knownStrings" )
                        var ksE = document.getElementById( "existingKnownStrings" )
                        if ( ! containsLine( ks.value, text ) &amp;&amp; ! containsLine( ksE.value, text ) )
                        {
                            ks.value += "\n" + text
                        }
                    }

                    function containsLine( subject, text )
                    {
                        for ( var line in subject.split( "\n" ) )
                        {
                            if ( line.includes( text ) )
                            {
                                return true
                            }
                        }
                        return false
                    }

                    function removePath( path )
                    {
                        var ks = document.getElementById( "removePaths" )
                        if ( ! ks.value.includes( path ) )
                        {
                            ks.value += "\n" + path
                        }
                    }
                    function replaceString( text )
                    {
                        var ks = document.getElementById( "replaceStrings" )
                        if ( ! ks.value.includes( text ) )
                        {
                            ks.value += "\n" + text
                        }
                    }

                    function selectText( text )
                    {
                        window.find( text, true )
                    }

                </script>
            </head>
            <body>
                <p>
                    <b>Truffler Report:</b>
                    created=[<xsl:value-of select="@created"/>],
                    branch=[<xsl:value-of select="@branch"/>],
                    repo=[<xsl:value-of select="@repo"/>]
                </p>
                <div id="columns">
                    <div id="leftColumn">
                        <p class="panel-title">
                            Known Strings
                        </p>
                        <p>
                            New strings to exempt:
                        </p>
                        <textarea id="knownStrings" rows="10"> </textarea>
                        <p>
                            Existing exemptions:
                            <code>truffler/entropy-known-strings.txt</code>:
                        </p>
                        <textarea id="existingKnownStrings" rows="10" disabled="true">
                            <xsl:value-of select="entropy/known-strings"/>
                        </textarea>

                        <br/>
                        <hr/>

                        <p class="panel-title">
                            History Rewriting
                        </p>
                        <p>
                            Strings to replace (with tokens):
                        </p>
                        <textarea id="replaceStrings" rows="5">
                        </textarea>
                        <p>
                            Paths to remove:
                        </p>
                        <textarea id="removePaths" rows="5">
                        </textarea>
                    </div>
                    <div id="rightColumn">
                        <p class="panel-title">
                            Issues:
                        </p>
                        <div class="commit-list">
                            <xsl:choose>
                                <xsl:when test="commit">
                                    <xsl:apply-templates select="commit"/>
                                </xsl:when>
                                <xsl:otherwise>(no issues)</xsl:otherwise>
                            </xsl:choose>
                        </div>
                    </div>
                </div>
            </body>
        </html>
    </xsl:template>

    <xsl:template match="commit">
        <div>
            <span class="commit">
                <xsl:value-of select="@date"/> (
                from=<xsl:value-of select="@from"/>,
                sha=<xsl:value-of select="@sha"/> )
            </span>
            <br/>
            <xsl:apply-templates/>
        </div>
    </xsl:template>

    <xsl:template match="diff">
        <div class="diff">
            <table class="issues">
                <tr>
                    <td>
                        Path:
                    </td>
                    <td>
                        <input type="button" value="remove file node" onclick="removePath( '{@path}{@new-path}' )"/>
                    </td>
                    <td>
                        <span class="path"><xsl:value-of select="@path"/><xsl:value-of select="@new-path"/></span>
                    </td>
                </tr>
                <xsl:for-each select="*[ name() != 'text' ]">
                    <tr>
                        <td>
                            <span class="detail-notes">
                                <xsl:value-of select="name()"/> : (
                                    <xsl:for-each select="@*">
                                        <xsl:value-of select="name()"/>=<xsl:value-of select="."/>
                                        <xsl:if test="position() != last()">, </xsl:if>
                                    </xsl:for-each>
                                )
                            </span>
                        </td>
                        <td>
                            <xsl:choose>
                                <xsl:when test="( name() = 'entropy' )">
                                    <input type="button" value="exempt" onclick="addKnownString( '{.}' )"/>
                                </xsl:when>
                                <xsl:when test="( name() = 'regex' )">
                                    <input type="button" value="exempt" onclick="addKnownString( '{.}' )"/>
                                    &#160;
                                    <input type="button" value="overwrite" onclick="replaceString( '{.}' )"/>
                                </xsl:when>
                            </xsl:choose>
                        </td>
                        <td>
                            <span class="detail-value" onclick="selectText( '{.}' )"><xsl:value-of select="."/></span>
                        </td>
                    </tr>
                </xsl:for-each>
            </table>
            <xsl:if test="text">
                <textarea class="diff-text" disabled="true">
                    <xsl:value-of select="text"/>
                </textarea>
            </xsl:if>
        </div>
    </xsl:template>
</xsl:stylesheet>