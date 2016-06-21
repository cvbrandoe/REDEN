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
    
    <xsl:template match="s">
        <xsl:apply-templates select="node()" />
    </xsl:template>
    <xsl:template match="rs">
        <xsl:apply-templates select="node()" />
    </xsl:template>
    <xsl:template match="phr">
        <xsl:apply-templates select="node()" />
    </xsl:template>
    <xsl:template match="certainty">
        <xsl:apply-templates select="node()" />
    </xsl:template>
    <xsl:template match="placeName">
        <xsl:apply-templates select="node()" />
    </xsl:template>
    <xsl:template match="geogName">
        <xsl:apply-templates select="node()" />
    </xsl:template>
    <xsl:template match="geogFeat">
        <xsl:apply-templates select="node()" />
    </xsl:template>
    <xsl:template match="term[descendant::*[@xml:id]]">
        <xsl:apply-templates select="node()" />
    </xsl:template>
    
</xsl:stylesheet>