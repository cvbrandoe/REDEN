<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
    xmlns:ign="http://example.com/namespace/" 
    xmlns:xs="http://www.w3.org/2001/XMLSchema" xpath-default-namespace="http://www.tei-c.org/ns/1.0" 
    xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" 
    xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#">
    <!-- On utilise l'attribut typage pour noter le type dbpedia -->
    <xsl:output method="xml" encoding="UTF-8" indent="yes" />
    
    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>
    
    <xsl:template match="*[name()='geogName' and (descendant::*[@lemma = 'massif' or @lemma = 'vallée' or @lemma = 'mont'
                or @lemma = 'plateau' or @lemma = 'chaîne' or @lemma = 'plaine' or @lemma = 'source'])]//name">
        
        <xsl:element name="name">
            <xsl:attribute name="typage">
                <xsl:choose>
                    <xsl:when test="ancestor::geogName[1][descendant::*[@lemma = 'massif' 
                    or @lemma = 'mont' or @lemma = 'chaîne']]">
                        <xsl:text>mountainOrVolcano</xsl:text>
                    </xsl:when>
                    <xsl:when test="ancestor::geogName[1][descendant::*[@lemma = 'vallée' or @lemma = 'source'
                     or @lemma = 'rivière' or @lemma = 'fleuve' or @lemma = 'rive' or @lemma = 'bord' or @lemma = 'coude']]">
                        <xsl:text>bodyOfWater</xsl:text>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:text>settlement</xsl:text>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:attribute>
            <xsl:apply-templates select="@* | node()" />
        </xsl:element>
    </xsl:template>
    
</xsl:stylesheet>