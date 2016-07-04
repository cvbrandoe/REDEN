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
    
    <xsl:function name="ign:getStartingPoint" as="element()?">
    	<xsl:param name="orientation" as="element()"/>
    	<xsl:variable name="precedingDot" as="element()?" select="$orientation/preceding-sibling::*[text()='.'][1]"/>
    	<xsl:variable name="followingDot" as="element()" select="$orientation/following-sibling::*[text()='.'][1]"/>
    	<xsl:variable name="followingFollowingDot" as="element()?" select="$followingDot/following-sibling::*[text()='.'][1]"/>
    	<!-- Dans les commentaires suivants, la phrase est celle contenant l'orientation -->
    	<!-- 'de' ne doit pas être pécédé d'un nom -->
    	<xsl:choose>
    		<!-- On regarde en arrière et en avant si un toponyme de la phrase est précédé par 'de' -->
    		<xsl:when test="$orientation/preceding-sibling::*[position() > 0 and not(position() > 
	    		(ign:getPosition($orientation) - ign:getPosition($precedingDot) + 1))]
	    		[name()='bag' and preceding-sibling::*[position() > 0 and not(position() > 3)][descendant-or-self::*[@lemma='de' and not(parent::*[name()='date'])] 
		    		and not(preceding-sibling::*[1]/descendant-or-self::*[@type='N'])]] or 
		    		$orientation/following-sibling::*[position() > 0 and not(position() > 
	    		(ign:getPosition($followingDot) - ign:getPosition($orientation) + 1))]
	    		[name()='bag' and preceding-sibling::*[position() > 0 and not(position() > 3)][descendant-or-self::*[@lemma='de' and not(parent::*[name()='date'])] 
		    		and not(preceding-sibling::*[1]/descendant-or-self::*[@type='N'])]]">
    			<xsl:variable name="precedingBagPrecededByDe" select="$orientation/preceding-sibling::*
		    		[name()='bag' and preceding-sibling::*[position() > 0 and not(position() > 3)][descendant-or-self::*[@lemma='de' and not(parent::*[name()='date'])] 
		    		and not(preceding-sibling::*[1]/descendant-or-self::*[@type='N'])]][1]"/>
    			<xsl:variable name="followingBagPrecededByDe" select="$orientation/following-sibling::*
		    		[name()='bag' and preceding-sibling::*[position() > 0 and not(position() > 3)][descendant-or-self::*[@lemma='de' and not(parent::*[name()='date'])] 
		    		and not(preceding-sibling::*[1]/descendant-or-self::*[@type='N'])]][1]"/>
				<xsl:sequence select="ign:chooseClosestBag($precedingBagPrecededByDe, $orientation, $followingBagPrecededByDe)"/>
    		</xsl:when>
    		<!-- Sinon on regarde dans les phrases précédentes et suivantes -->
    		<xsl:otherwise>
    			<xsl:variable name="followingBagPrecededByDe" select="$followingDot/following-sibling::*
		    		[name()='bag' and preceding-sibling::*[position() > 0 and not(position() > 3)][descendant-or-self::*[@lemma='de' and not(parent::*[name()='date'])] 
		    		and not(preceding-sibling::*[1]/descendant-or-self::*[@type='N'])]][1]"/>
    			<xsl:variable name="precedingBagPrecededByDe" select="$precedingDot/preceding-sibling::*
    				[name()='bag' and preceding-sibling::*[position() > 0 and not(position() > 3)][descendant-or-self::*[@lemma='de' and not(parent::*[name()='date'])] 
    				and not(preceding-sibling::*[1]/descendant-or-self::*[@type='N'])]][1]"/>    				
				<xsl:sequence select="ign:chooseClosestBag($precedingBagPrecededByDe, $orientation, $followingBagPrecededByDe)"/>
    		</xsl:otherwise>
    	</xsl:choose>
    </xsl:function>
    
    <xsl:function name="ign:chooseClosestBag" as="element()?">
    	<xsl:param name="precedingBagPrecededByDe" as="element()?"/>
    	<xsl:param name="orientation" as="element()"/>
    	<xsl:param name="followingBagPrecededByDe" as="element()?"/>
	    <xsl:choose>
			<xsl:when test="$followingBagPrecededByDe and not($precedingBagPrecededByDe)">
				<xsl:sequence select="$followingBagPrecededByDe"/>
			</xsl:when>
			<xsl:when test="not($followingBagPrecededByDe) and $precedingBagPrecededByDe">
				<xsl:sequence select="$precedingBagPrecededByDe"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:choose>
					<xsl:when test="ign:getPosition($followingBagPrecededByDe) - ign:getPosition($orientation) > 
					ign:getPosition($orientation) - ign:getPosition($precedingBagPrecededByDe)">
						<xsl:sequence select="$precedingBagPrecededByDe"/>
					</xsl:when>
					<xsl:otherwise>
						<xsl:sequence select="$followingBagPrecededByDe"/>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:otherwise>
		</xsl:choose>
    </xsl:function>
    
    <xsl:function name="ign:getNextWaypoints" as="element()*">
    	<xsl:param name="orientation" as="element()"/>
    	<xsl:variable name="followingOrientation" as="element()?" select="$orientation/following-sibling::*
    		[(@type='orientation' or @subtype='orientation')][1]"/>
    	<xsl:variable name="precedingOrientation" as="element()?" select="$orientation/preceding-sibling::*
    		[(@type='orientation' or @subtype='orientation')][1]"/>
   		<xsl:choose>
   			<!-- On regarde en avant -->
   			<xsl:when test="$followingOrientation and $orientation/following-sibling::*
   				[position() > 0 and not(position() > 
   				(ign:getPosition($followingOrientation) - ign:getPosition($orientation) + 1))][name()='bag']">
   				<xsl:sequence select="$orientation/following-sibling::*[position() > 0 and not(position() > 
   				(ign:getPosition($followingOrientation) - ign:getPosition($orientation) + 1))]
   				[name()='bag' and not(preceding-sibling::*[position() > 0 and not(position() > 3)][descendant-or-self::*[@lemma='de'] 
    				and not(preceding-sibling::*[1]/descendant-or-self::*[@type='N'])])]"/>
			</xsl:when>
   			<!-- On regarde en arrière -->
   			<xsl:when test="$precedingOrientation and $orientation/preceding-sibling::*
   				[position() > 0 and not(position() > 
   				(ign:getPosition($orientation) - ign:getPosition($precedingOrientation) + 1))][name()='bag']">
   				<xsl:sequence select="$orientation/preceding-sibling::*[position() > 0 and not(position() > 
   				(ign:getPosition($orientation) - ign:getPosition($precedingOrientation) + 1))]
   				[name()='bag' and not(preceding-sibling::*[position() > 0 and not(position() > 3)][descendant-or-self::*[@lemma='de'] 
    				and not(preceding-sibling::*[1]/descendant-or-self::*[@type='N'])])]"/>
			</xsl:when>
   		</xsl:choose>
    </xsl:function>
    
    <xsl:template match="*[(@type='orientation' or @subtype='orientation') 
    	and preceding-sibling::*[position() > 0 and not(position() > 2) 
    		and descendant-or-self::*[@lemma='vers' or @lemma='à' or @lemma='par']]]">
    		
		<xsl:variable name="orientation" select="current()"/>    	
		<xsl:variable name="followingOrientation" 
			select="current()/following-sibling::*[@type='orientation' or @subtype='orientation'][1]" as="element()?"/>    	
    	<xsl:variable name="startingPoint" as="element()?" select="ign:getStartingPoint($orientation)"/>
    	<xsl:copy>
			<xsl:apply-templates select="@*|node()"/>
			
			<xsl:if test="$startingPoint">
				<xsl:variable name="nextBags" as="element()*">
					<xsl:choose>
						<xsl:when test="ign:getPosition($startingPoint) > ign:getPosition($orientation)">
							<!-- <xsl:sequence select="$startingPoint/following-sibling::*[text()='.'][1]"/> -->
							<xsl:variable name="followingDot" as="element()">
								<xsl:choose>
									<xsl:when test="ign:getPosition($followingOrientation/preceding-sibling::*[text()='.'][1]) > 
									ign:getPosition($startingPoint/following-sibling::*[text()='.'][1])">
										<xsl:sequence select="$followingOrientation/preceding-sibling::*[text()='.'][1]"/>
									</xsl:when>
									<xsl:otherwise>
										<xsl:sequence select="$startingPoint/following-sibling::*[text()='.'][1]"/>
									</xsl:otherwise>
								</xsl:choose>
							</xsl:variable>
							<xsl:sequence select="$startingPoint/following-sibling::*
								[position() > 0 and not(position() > (ign:getPosition($followingDot) - ign:getPosition($startingPoint) + 1))]
								[name()='bag']"/> 
						</xsl:when>
						<xsl:otherwise>
							<!-- <xsl:sequence select="$orientation/following-sibling::*[text()='.'][1]"/> -->
							<xsl:variable name="followingDot" as="element()">
								<xsl:choose>
									<xsl:when test="ign:getPosition($followingOrientation/preceding-sibling::*[text()='.'][1]) > 
									ign:getPosition($orientation/following-sibling::*[text()='.'][1])">
										<xsl:sequence select="$followingOrientation/preceding-sibling::*[text()='.'][1]"/>
									</xsl:when>
									<xsl:otherwise>
										<xsl:sequence select="$orientation/following-sibling::*[text()='.'][1]"/>
									</xsl:otherwise>
								</xsl:choose>
							</xsl:variable>
							<xsl:sequence select="$orientation/following-sibling::*
								[position() > 0 and not(position() > (ign:getPosition($followingDot) - ign:getPosition($orientation) + 1))]
								[name()='bag']"/> 
						</xsl:otherwise>
					</xsl:choose>
				</xsl:variable>
				<xsl:if test="$nextBags and count($nextBags) > 0">
					<xsl:for-each select="$nextBags">	
						<xsl:copy-of select="ign:createRLSP(current(), $orientation, $startingPoint, 0)" />
					</xsl:for-each>
				</xsl:if>
				<!-- <rlsp>
					<start>
						<xsl:copy-of select="$startingPoint/descendant::*[@xml:id][1]/text()"/>
					</start>
					<xsl:if test="$nextBags and count($nextBags) > 0">
							<xsl:for-each select="$nextBags">							
								<waypoint>
									<xsl:copy-of select="current()/descendant::*[@xml:id][1]/text()"/>
								</waypoint>
							</xsl:for-each>
					</xsl:if>
				</rlsp>  -->
			</xsl:if>
    	</xsl:copy>	    	
    	
    </xsl:template> 
    
    <xsl:function name="ign:createRLSP" as="element()*">
    	<xsl:param name="root" as="element()?"/>
    	<xsl:param name="orientation" as="element()"/>
    	<xsl:param name="target" as="element()?"/>
    	<xsl:param name="paternNumber" as="xs:integer"/>
    	
    	<xsl:if test="$root and $target">
	    	<xsl:for-each select="$root/descendant-or-self::*[@xml:id]">
	    		<xsl:variable name="rootElement" select="current()" as="element()"/>
	    		<xsl:variable name="rootId" select="current()/@xml:id" as="xs:integer"/>
		    	<xsl:for-each select="$target/descendant-or-self::*[@xml:id]">	    		
			    	<xsl:element name="rlsp">
			    		<xsl:attribute name="rootText"><xsl:value-of select="$rootElement/text()" /></xsl:attribute>
			    		<xsl:attribute name="targetText"><xsl:value-of select="current()/text()" /></xsl:attribute>
			    		<xsl:attribute name="root"><xsl:value-of select="$rootId" /></xsl:attribute>
			    		<xsl:attribute name="orientation"><xsl:value-of select="$orientation/descendant::*[
			                    contains(lower-case(text()),'nord') or contains(lower-case(text()),'sud') or contains(lower-case(text()),'est') or contains(lower-case(text()),'ouest')]" /></xsl:attribute>
			    		<xsl:attribute name="target"><xsl:value-of select="current()/@xml:id" /></xsl:attribute>
			    		<xsl:attribute name="paternNumber"><xsl:value-of select="$paternNumber" /></xsl:attribute>
			    	</xsl:element>
		    	</xsl:for-each>
	    	</xsl:for-each>
    	</xsl:if>
    </xsl:function>
    
    <xsl:function name="ign:getFollowingBagPrecededByDe" as="element()?">
    	<xsl:param name="currentNode" as="element()"/>
    	<xsl:choose>
			<xsl:when test="$currentNode[descendant-or-self::*[@lemma='de']]">
				<xsl:sequence select="$currentNode/following-sibling::bag[1]"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:sequence select="$currentNode/following-sibling::bag[preceding-sibling::*[position() > 0 and not(position() > 2)][@lemma='de']][1]"/>
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