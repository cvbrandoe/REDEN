<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
    xmlns:ign="http://example.com/namespace/" 
    xmlns:xs="http://www.w3.org/2001/XMLSchema" xpath-default-namespace="http://www.tei-c.org/ns/1.0" 
    xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" 
    xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"
    xmlns:dbpedia-owl="http://dbpedia.org/ontology/"
    xmlns:rlsp="http://data.ign.fr/def/relationsspatiales#">
    
    <xsl:output method="xml" encoding="UTF-8" indent="yes" />
    
     <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>  
    
    <xsl:template match="bag">
    	<!-- <xsl:variable name="precedingBag" select="preceding-sibling::bag[1]"/> -->
    	<!-- <xsl:variable name="precedingPC" select="preceding-sibling::*[@force='strong'][1]"/> -->
    	<xsl:variable name="precedingDate" select="preceding-sibling::date[1]"/>
    	<!-- <xsl:variable name="followingBag" select="following-sibling::bag[1]"/> -->
    	<xsl:variable name="followingPC" select="following-sibling::*[@force='strong'][1]"/>
    	<xsl:variable name="followingDate" select="following-sibling::date[1]"/>
    	<xsl:variable name="currentPosition" select="ign:getPosition(.)"/>
    	
    	<!-- Si la date suivante est dans la même phrase et est plus proche que la date précédente, on prend la
    	date suivante. Sinon on prend la date précédente. -->
    	<xsl:choose>
    		<xsl:when test="abs(ign:getPosition($precedingDate) - $currentPosition) > 
    			abs(ign:getPosition($followingDate) - $currentPosition) and 
   				ign:getPosition($followingPC) > ign:getPosition($followingDate)">
    				<!-- On utilise la date suivante -->  			
			        <xsl:copy>
		        		<xsl:attribute name="date"><xsl:value-of select="$followingDate/@when"/></xsl:attribute>
			            <xsl:apply-templates select="@*|node()"/>
			        </xsl:copy>
    			</xsl:when>
    		<xsl:otherwise>    			
		        <xsl:copy>
		        	<xsl:attribute name="date"><xsl:value-of select="$precedingDate/@when"/></xsl:attribute>
		            <xsl:apply-templates select="@*|node()"/>
		        </xsl:copy>
    		</xsl:otherwise>
    	</xsl:choose>
    </xsl:template>
    
    
    
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