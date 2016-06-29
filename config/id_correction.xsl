<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
	xpath-default-namespace="http://www.tei-c.org/ns/1.0"
	xmlns:ign="http://example.com/namespace/"
	xmlns:xs="http://www.w3.org/2001/XMLSchema" >
    
    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>
    
	<xsl:template match="name[@xml:id]">
		<xsl:variable name="firstAncestorBeforeRoot" select="ign:getFirstAncestorBeforeRoot(current())" />
		<xsl:copy-of select="$firstAncestorBeforeRoot/name()"/>
		<xsl:variable name="xmlId" as="attribute()">
			<xsl:attribute name="xml:id">
				<xsl:copy-of select="count($firstAncestorBeforeRoot/preceding-sibling::*[descendant-or-self::*[@xml:id]])"/>
			</xsl:attribute>
		</xsl:variable>
		<!--  -->
		<xsl:copy>
			<xsl:copy-of select="$xmlId"/>
			<xsl:apply-templates select="node()"/>
		</xsl:copy>
	</xsl:template>
	
	<xsl:function name="ign:getFirstAncestorBeforeRoot" as="element()">
		<xsl:param name="node" as="element()"/>
		<xsl:for-each select="$node/ancestor::*">
			<xsl:if test="current()/parent::*[name()='TEI']">
				<xsl:sequence select="current()"/>
			</xsl:if>
		</xsl:for-each>
	</xsl:function>
	
    <!-- Return the absolute position of the node in the XML tree -->
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