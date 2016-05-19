<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
    xmlns:ign="http://example.com/namespace/" 
    xmlns:xs="http://www.w3.org/2001/XMLSchema" xpath-default-namespace="http://www.tei-c.org/ns/1.0" 
    xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" 
    xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#">
    
    <xsl:output method="xml" encoding="UTF-8" indent="yes" />
    
    <xsl:template match="/">
        <xsl:for-each select="//*[(@type='orientation' or @subtype='orientation') and 
            child::*[@lemma='occidental' or @lemma='oriental' or @lemma='méridional' or @lemma='septentrional']]">
            <xsl:variable name="precedingBag" select="preceding-sibling::bag[1]"/>
            <xsl:variable name="followingBag" select="following-sibling::bag[1]"/>  
            <xsl:variable name="container" select="ign:closest($precedingBag, $followingBag, .)" as="element()"/>
            <xsl:if test="count($container/child::*[@xml:id]) = 1">
                <xsl:variable name="followingBag" select="$container/following-sibling::bag[1]" />
                <test>  
                    <xsl:copy-of select="$container"/>
                    <xsl:copy-of select="."/>           
                    <xsl:copy-of select="$container/following-sibling::bag[1]"/>           
                </test>
            </xsl:if>
        </xsl:for-each>
    </xsl:template>
    
    <!-- Return the firstElement if it's the closest from the pivot, else the second -->
    <xsl:function name="ign:closest" as="element()">
        <xsl:param name="firstElement" as="element()"/>
        <xsl:param name="secondElement" as="element()"/>
        <xsl:param name="pivot" as="element()"/>
        
        <xsl:variable name="firstElementPosition" select="ign:getPosition($firstElement)" as="xs:integer"/>
            <xsl:variable name="secondElementPosition" select="ign:getPosition($secondElement)" as="xs:integer"/>
            <xsl:variable name="pivotPosition" select="ign:getPosition($pivot)" as="xs:integer"/>
            <xsl:choose>
                <xsl:when test="abs($pivotPosition - $firstElementPosition) > 
                    abs($secondElementPosition - $pivotPosition)">
                    <xsl:sequence select="$secondElement" />
                </xsl:when>
                <xsl:otherwise>
                    <xsl:sequence select="$firstElement" />
                </xsl:otherwise>
            </xsl:choose>
    </xsl:function>
    
    <!-- Return the position of the node in the XML tree -->
    <xsl:function name="ign:getPosition" as="xs:integer">
        <xsl:param name="Object" as="element()?" />
        <xsl:if test="$Object">
            <xsl:sequence select="count($Object/preceding-sibling::*)+1"/>
        </xsl:if>
        <xsl:if test="not($Object)">
            <xsl:sequence select="0"/>
        </xsl:if>
    </xsl:function>
</xsl:stylesheet>