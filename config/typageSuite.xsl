<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
    xmlns:ign="http://example.com/namespace/" 
    xmlns:xs="http://www.w3.org/2001/XMLSchema" xpath-default-namespace="http://www.tei-c.org/ns/1.0" 
    xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" 
    xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#">
    
    <xsl:output method="xml" encoding="UTF-8" indent="yes" />
    
    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>
    
    <xsl:template match="name[preceding-sibling::*[position() >= 1 and not(position() > 2)][@lemma='le' or @lemma='la' 
    or @lemma='les' or text()='du'] or preceding-sibling::*[1][(@subtype='orientation' or @type='orientation') 
    and descendant::*[@lemma='oriental' or 
    @lemma='occidental' or @lemma='méridional' or @lemma='septentrional']] or 
    following-sibling::*[1][(@subtype='orientation' or @type='orientation') and descendant::*[@lemma='oriental' or 
    @lemma='occidental' or @lemma='méridional' or @lemma='septentrional']]]">
    	<xsl:variable name="type" as="xs:string">
    		<xsl:choose>
    			<xsl:when test="preceding-sibling::*[position() > 0 and not(position() > 20)][
    			descendant-or-self::*[@lemma = 'vallée' or @lemma = 'source'
                     or @lemma = 'rivière' or @lemma = 'fleuve' or @lemma = 'rive' or @lemma = 'bord' or @lemma = 'coude']]">
                     <xsl:text>bodyOfWater</xsl:text>
                </xsl:when>
    			<xsl:when test="following-sibling::*[position() > 0 and not(position() > 20)][
    			descendant-or-self::*[@lemma = 'vallée' or @lemma = 'source'
                     or @lemma = 'rivière' or @lemma = 'fleuve' or @lemma = 'rive' or @lemma = 'bord' or @lemma = 'coude']]">
                     <xsl:text>bodyOfWater</xsl:text>
                </xsl:when>
                <xsl:otherwise>
                        <xsl:text>territoryOrNaturalPlace</xsl:text></xsl:otherwise>
    		</xsl:choose>
    	</xsl:variable>
        <xsl:element name="name">
            <xsl:attribute name="typage">
                <xsl:choose>
                    <xsl:when test="@typage">
                        <xsl:value-of select="@typage"/>
                        <xsl:copy-of select="$type"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:copy-of select="$type"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:attribute>
            <xsl:apply-templates select="@xml:id | node()" />
        </xsl:element>
    </xsl:template>
    
</xsl:stylesheet>