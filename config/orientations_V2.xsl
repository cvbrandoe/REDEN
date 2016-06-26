<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
    xmlns:ign="http://example.com/namespace/" 
    xmlns:xs="http://www.w3.org/2001/XMLSchema" xpath-default-namespace="http://www.tei-c.org/ns/1.0" 
    xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" 
    xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#" >
    <xsl:output method="xml" encoding="UTF-8" indent="yes" />
	<!-- 
      Notation :
      T1, T2, ..., TN => toponymes
      O => orientation (nod, sud, etc.)
      # => quelques mots (difficile de définir la bonne limite)
      A|B => A ou B    
      
      Pour un bag l'orientation "Nord" (respectivement pr Sud, Ouest et Est) signifie :
        - si @position = start -> T1 au Nord de T2
        - si @position = end   -> T2 au Nord de T1
    -->     
    
    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>
    
    <xsl:template match="*[(@type='orientation' or @subtype='orientation') 
    	and preceding-sibling::*[position() > 0 and not(position() > 2) 
    		and descendant-or-self::*[@lemma='vers' or @lemma='à' or @lemma='par']]]">
    	<xsl:variable name="followingOrientation" select="./following-sibling::*[(@type='orientation' or @subtype='orientation') 
    	and preceding-sibling::*[position() > 0 and not(position() > 2) 
    		and descendant-or-self::*[@lemma='vers' or @lemma='à' or @lemma='par']]][1]" as="element()?"/>
		<xsl:variable name="followingBagPrecededByDe" select="./following-sibling::bag[preceding-sibling::*[1][@lemma='de']][1]" as="element()?"/>
		<xsl:variable name="followingBag" select="./following-sibling::bag[1]" as="element()?"/>
		<xsl:variable name="followingOrientationPosition" select="ign:getPosition($followingOrientation)" as="xs:integer"/>
		<xsl:variable name="followingBagPrecededByDePosition" select="ign:getPosition($followingBagPrecededByDe)" as="xs:integer"/>
		<xsl:variable name="followingBagPosition" select="ign:getPosition($followingBag)" as="xs:integer"/>
		<xsl:choose>
			<xsl:when test="$followingOrientationPosition = 0 or (($followingOrientationPosition > $followingBagPrecededByDePosition)
			and not($followingBagPrecededByDePosition > $followingBagPosition))">
				<root><xsl:copy-of select="$followingBagPrecededByDe"></xsl:copy-of></root>
				<!-- test -->
				<orientation><xsl:copy-of select="."></xsl:copy-of></orientation>
				<target><xsl:copy-of select="$followingBagPrecededByDe/following-sibling::bag[1]"></xsl:copy-of></target>
			</xsl:when>
			<xsl:otherwise>
			    <xsl:copy>
			        <xsl:apply-templates select="@*|node()"/>
			    </xsl:copy>
	    	</xsl:otherwise>
		</xsl:choose>
    </xsl:template> 
    
    <!-- Return true if the element is followed or preceded by a negation. If there is a non negated verb between 
    the element and the negated verb, it return false (in order to take into account the context of 
    the element and not to seek to far away in the long sentences). If there is no negation it return false. -->
    <!-- A revoir -->
   <!--  <xsl:function name="ign:isPrecededOrFollowedByNegation" as="xs:boolean">
    	<xsl:param name="node" as="element()"/>
    	<xsl:variable name="precedingDot" as="element()?" select="$node/preceding-sibling::*[descendant-or-self::*[.='.']][1]"/>
    	<xsl:variable name="followingDot" as="element()" select="$node/following-sibling::*[descendant-or-self::*[.='.']][1]"/>
    	<xsl:variable name="maxLeftPosition" as="xs:integer" select="ign:getPosition($node) - ign:getPosition($precedingDot)"/>
    	<xsl:variable name="maxRightPosition" as="xs:integer" select="ign:getPosition($followingDot) - ign:getPosition($node)"/>
    	<xsl:variable name="negation" select="$node/preceding-sibling::*[position() > 0 and not(position() > $maxLeftPosition)]/
    		@lemma='ne' or $node/following-sibling::*[position() > 0 and not(position() > $maxRightPosition)]/
    		@lemma='ne'"/>
    </xsl:function> -->
    
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