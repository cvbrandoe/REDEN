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
		<xsl:variable name="followingBagPrecededByDe" select="ign:getFollowingBagPrecededByDe(current())" as="element()?"/>
		<xsl:variable name="followingBag" select="./following-sibling::bag[1]" as="element()?"/>
		<xsl:variable name="followingOrientationPosition" select="ign:getPosition($followingOrientation)" as="xs:integer"/>
		<xsl:variable name="followingBagPrecededByDePosition" select="ign:getPosition($followingBagPrecededByDe)" as="xs:integer"/>
		<xsl:variable name="followingBagPosition" select="ign:getPosition($followingBag)" as="xs:integer"/>
		<xsl:variable name="currentOrientationPosition" select="ign:getPosition(current())" as="xs:integer"/>
		<xsl:variable name="currentPosition" select="current()"/>					
	    <xsl:copy>
			<xsl:apply-templates select="@*|node()"/>
			<xsl:choose>
				<xsl:when test=".[following-sibling::*[1][@lemma='par']]">
					<xsl:variable name="precedingBag" select="current()/preceding-sibling::bag[1]" as="element()"/>	
					<xsl:variable name="followingFollowingBag" select="$followingBag/following-sibling::bag[1]" as="element()"/>
					<!-- <num>3</num>	
					<root><xsl:copy-of select="$followingFollowingBag"></xsl:copy-of></root>
					<orientation><xsl:copy-of select="."></xsl:copy-of></orientation>
					<target><xsl:copy-of select="$followingBag"></xsl:copy-of></target>
					<num>3</num>	
					<root><xsl:copy-of select="$followingBag"></xsl:copy-of></root>
					<orientation><xsl:copy-of select="."></xsl:copy-of></orientation>
					<target><xsl:copy-of select="$precedingBag"></xsl:copy-of></target> -->	
			    	<xsl:copy-of select="ign:createRLSP($followingFollowingBag, $currentPosition, $followingBag, 3)"/>
			    	<xsl:copy-of select="ign:createRLSP($followingBag, $currentPosition, $precedingBag, 3)"/>
				</xsl:when>
				<xsl:when test=".[preceding-sibling::*[position() > 0 and not(position() > 3)][text()='.']]">
					<!-- <num>2</num>
					<root><xsl:copy-of select="$followingBagPrecededByDe/following-sibling::bag[1]"></xsl:copy-of></root>
					<orientation><xsl:copy-of select="."></xsl:copy-of></orientation>
					<target><xsl:copy-of select="$followingBagPrecededByDe"></xsl:copy-of></target> -->
			    	<xsl:copy-of select="ign:createRLSP($followingBagPrecededByDe/following-sibling::bag[1], 
			    	$currentPosition, $followingBagPrecededByDe, 2)"/>
				</xsl:when>
				<xsl:when test="
				($followingOrientationPosition = 0 or ($followingOrientationPosition > $followingBagPrecededByDePosition and
				$followingOrientationPosition > ign:getPosition($followingBagPrecededByDe/following-sibling::bag[1]))) and 
				(.[descendant-or-self::*[@lemma='de']] or (.[following-sibling::*[1][@lemma='de']]) or 
				(not($followingBagPrecededByDePosition - $currentOrientationPosition > 10) 
				and $followingBagPrecededByDe[preceding-sibling::*[position() > 0 and not(position() > 2)]/text()='.']) or 
				($followingBagPrecededByDe[preceding-sibling::*[position() > 0 and not(position() > 2)][@subtype='motion_initial']]))">
					<xsl:variable name="followingDot" select="$followingBagPrecededByDe/following-sibling::*[text()='.'][1]"/>
					<xsl:variable name="followingDotOrOrientationPosition" as="xs:integer">
						<xsl:choose>
							<xsl:when test="ign:getPosition($followingDot) > $followingOrientationPosition">
								<xsl:sequence select="$followingOrientationPosition"/>
							</xsl:when>
							<xsl:otherwise>
								<xsl:sequence select="ign:getPosition($followingDot)"/>
							</xsl:otherwise>
						</xsl:choose>				
					</xsl:variable>
					<!-- <dot><xsl:value-of select="$followingDotPosition - $followingBagPrecededByDePosition"/></dot> -->
					<xsl:for-each select="$followingBagPrecededByDe/following-sibling::*[position() > 0 and 
					not(position() > ($followingDotOrOrientationPosition - $followingBagPrecededByDePosition))][name()='bag']">
				    	<xsl:copy-of select="ign:createRLSP(current(), $currentPosition, $followingBagPrecededByDe, 1)"/>							
					</xsl:for-each>
				</xsl:when> 
				<xsl:when test="current()[following-sibling::*[position() > 0 and not(position() > 6)][@subtype='motion_final']]">
					<xsl:variable name="precedingBag" select="current()/preceding-sibling::bag[1]" as="element()"/>
					<!-- <num>4</num>				
					<root><xsl:copy-of select="$followingBag"></xsl:copy-of></root>
					<orientation><xsl:copy-of select="."></xsl:copy-of></orientation>
					<target><xsl:copy-of select="$precedingBag"></xsl:copy-of></target> -->
			    	<xsl:copy-of select="ign:createRLSP($followingBag, $currentPosition, $precedingBag, 4)"/>
				</xsl:when>
				<xsl:when test="current()[preceding-sibling::*[position() > 0 and not(position() > 6)][@subtype='motion_final']]">
					<xsl:variable name="precedingBag" select="current()/preceding-sibling::bag[1]" as="element()"/>			
					<xsl:variable name="precedingPrecedingBag" select="$precedingBag/preceding-sibling::bag[1]" as="element()"/>							
					<!-- <root><xsl:copy-of select="$precedingBag"></xsl:copy-of></root>
					<orientation><xsl:copy-of select="."></xsl:copy-of></orientation>
					<target><xsl:copy-of select="$precedingPrecedingBag"></xsl:copy-of></target> -->
			    	<xsl:copy-of select="ign:createRLSP($precedingBag, $currentPosition, $precedingPrecedingBag, 4)"/>
				</xsl:when>
			</xsl:choose>		
	    </xsl:copy>	
    </xsl:template> 
    
    <xsl:function name="ign:createRLSP" as="element()+">
    	<xsl:param name="root" as="element()"/>
    	<xsl:param name="orientation" as="element()"/>
    	<xsl:param name="target" as="element()"/>
    	<xsl:param name="paternNumber" as="xs:integer"/>
    	
    	<xsl:for-each select="$root/descendant-or-self::*[@xml:id]/@xml:id">
    		<xsl:variable name="rootId" select="current()" as="xs:integer"/>
	    	<xsl:for-each select="$target/descendant-or-self::*[@xml:id]/@xml:id">	    		
		    	<xsl:element name="rlsp">
		    		<xsl:attribute name="root"><xsl:value-of select="$rootId" /></xsl:attribute>
		    		<xsl:attribute name="orientation"><xsl:value-of select="$orientation/descendant::*[
		                    contains(lower-case(text()),'nord') or contains(lower-case(text()),'sud') or contains(lower-case(text()),'est') or contains(lower-case(text()),'ouest')]" /></xsl:attribute>
		    		<xsl:attribute name="target"><xsl:value-of select="current()" /></xsl:attribute>
		    		<xsl:attribute name="paternNumber"><xsl:value-of select="$paternNumber" /></xsl:attribute>
		    	</xsl:element>
	    	</xsl:for-each>
    	</xsl:for-each>
    </xsl:function>
    
    <xsl:function name="ign:getFollowingBagPrecededByDe" as="element()?">
    	<xsl:param name="currentNode" as="element()"/>
    	<xsl:choose>
			<xsl:when test="$currentNode[descendant-or-self::*[@lemma='de']]">
				<xsl:sequence select="$currentNode/following-sibling::bag[1]"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:sequence select="$currentNode/following-sibling::bag[preceding-sibling::*[1][@lemma='de']][1]"/>
			</xsl:otherwise>
		</xsl:choose>
    </xsl:function>
    
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