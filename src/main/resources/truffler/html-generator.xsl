<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:template match="truffle">
        <html>
            <head>
                <style>
                    body, div, td {
                        font-family: "Arial", san-serif;
                        margin: 10px;
                    }
                    .issues {
                        margin: 5pt;
                    }
                    table, textarea {
                        width: 100%;
                    }
                    td:nth-child(1)
                    {
                        font-weight: bold;
                        width: 30%;
                    }
                    td:nth-child(2)
                    {
                        font-weight: bold;
                        width: 5%;
                    }

                    .commit {
                        font-weight: bold;
                        border-top: 1px solid;
                    }

                    .path
                    {
                        border: 1pt dotted black;
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
                    }

                    #rightColumn {
                        flex: 0 0 70%;
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
                        if ( ! ks.value.includes( text ) &amp;&amp; ! ksE.value.includes( text ) )
                        {
                            ks.value += "\n" + text
                        }
                    }

                    function selectText( text )
                    {
                        if ( window.find( text, true ) )
                        {

                        }
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
                        <p>
                            Known Strings
                        </p>
                        <p>
                            New strings to add:
                        </p>
                        <textarea id="knownStrings" rows="15">
                        </textarea>
                        <p>
                            Existing in file: <code>truffler/entropy-known-strings.txt</code>:
                        </p>
                        <textarea id="existingKnownStrings" rows="15" disabled="true">
                            <xsl:value-of select="entropy/known-strings"/>
                        </textarea>
                    </div>
                    <div id="rightColumn">
                        <p>
                            Issues:
                        </p>
                        <div class="commit-list">
                            <xsl:apply-templates select="commit"/>
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
                    <td colspan="3">Path: <span class="path"><xsl:value-of select="@path"/><xsl:value-of select="@new-path"/></span></td>
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
                                    <input type="button" value="add" onclick="addKnownString( '{.}' )"/>
                                </xsl:when>
                                <xsl:when test="( name() = 'regex' )">
                                    <input type="button" value="per" onclick="addKnownString( '{.}' )"/>
                                </xsl:when>
                            </xsl:choose>
                        </td>
                        <td>
                            <span class="detail-value" onclick="selectText( '{.}' )"><xsl:value-of select="."/></span>
                        </td>
                    </tr>
                </xsl:for-each>
            </table>
            <textarea class="diff-text">
                <xsl:value-of select="text"/>
            </textarea>
        </div>
    </xsl:template>
</xsl:stylesheet>