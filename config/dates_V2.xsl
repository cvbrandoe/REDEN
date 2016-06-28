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
    	<xsl:variable name="precedingBag" select="preceding-sibling::bag[1]"/> 
    	<xsl:variable name="followingBag" select="following-sibling::bag[1]"/> 
    	<xsl:variable name="precedingPC" select="preceding-sibling::*[@force='strong'][1]"/>
    	<xsl:variable name="precedingDate" select="preceding-sibling::date[1]"/>
    	<xsl:variable name="followingPC" select="following-sibling::*[@force='strong'][1]"/>
    	<xsl:variable name="followingDate" select="following-sibling::date[1]"/>
    	<xsl:variable name="currentPosition" select="ign:getPosition(.)"/>
    	    	
    	<xsl:choose>
    		<!-- Si aucune date n'est dans la même phrase, on ne s'en sert pas -->
    		<xsl:when test="ign:getPosition($precedingPC) - ign:getPosition($precedingDate) > 0 and 
    			ign:getPosition($followingDate) - ign:getPosition($followingPC) > 0">			
		        <xsl:copy>
		            <xsl:apply-templates select="@*|node()"/>
		        </xsl:copy>
	        </xsl:when>
	    	<!-- Si la date suivante est dans la même phrase et est plus proche que la date précédente, on prend la
	    	date suivante. Sinon on prend la date précédente. -->
    		<xsl:when test="abs(ign:getPosition($precedingDate) - $currentPosition) > 
    			abs(ign:getPosition($followingDate) - $currentPosition) and 
   				ign:getPosition($followingPC) > ign:getPosition($followingDate)">
   				<xsl:choose>
   					<!-- Si un bag est situé juste avant la date, on ne se sert pas de la date -->
   					<xsl:when test="ign:getPosition($followingDate) > ign:getPosition($followingBag) or 
   						ign:getPosition($followingDate) > ign:getPosition($followingPC)">   					 			
				        <xsl:copy>
				            <xsl:apply-templates select="@*|node()"/>
				        </xsl:copy>
   					</xsl:when>
   					<xsl:otherwise><!-- On utilise la date suivante -->  			
				        <xsl:copy>
			        		<xsl:copy-of select="ign:create_date($followingDate)"/>
				            <xsl:apply-templates select="@*|node()"/>
				        </xsl:copy>
			        </xsl:otherwise>
   				</xsl:choose>    				
   			</xsl:when>
    		<xsl:otherwise> 
	    		<xsl:choose>
	   					<!-- Si un bag est situé juste après la date, on ne se sert pas de la date -->
	   					<xsl:when test="ign:getPosition($precedingBag) > ign:getPosition($precedingDate) or 
	   						ign:getPosition($precedingPC) > ign:getPosition($precedingDate)">   					 			
					        <xsl:copy>
					            <xsl:apply-templates select="@*|node()"/>
					        </xsl:copy>
	   					</xsl:when>
	   					<xsl:otherwise>
			   				<!-- on se sert de la date précédente -->  			
					        <xsl:copy>
				        		<xsl:copy-of select="ign:create_date($precedingDate)"/>
					            <xsl:apply-templates select="@*|node()"/>
					        </xsl:copy>
				        </xsl:otherwise>
	   				</xsl:choose>   
    		</xsl:otherwise>
    	</xsl:choose>
    </xsl:template>
    
    <xsl:function name="ign:create_date" as="attribute()*">
    	<xsl:param name="date" as="element()?"/>
    	<xsl:if test="$date">
	    	<xsl:attribute name="date">
				<xsl:choose>
					<xsl:when test="ign:has_motion_initial($date)">
						<xsl:text>motion_initial</xsl:text>
					</xsl:when>
					<xsl:otherwise>
						<xsl:text>motion_final</xsl:text>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:attribute>
			<xsl:choose>
				<xsl:when test="$date[@when]">
					<xsl:attribute name="when">
						<xsl:value-of select="$date/@when"/>
					</xsl:attribute>
				</xsl:when>
				<xsl:otherwise>
					<xsl:attribute name="from">
						<xsl:value-of select="$date/@from"/>
					</xsl:attribute>
					<xsl:attribute name="to">
						<xsl:value-of select="$date/@to"/>
					</xsl:attribute>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:if>
    </xsl:function>    
    
    <!-- Return true if the word is in a sentence where there is a motion_initial verb -->
    <xsl:function name="ign:has_motion_initial" as="xs:boolean">    
        <xsl:param name="word" as="element()" />
        <xsl:variable name="precedingPC" select="$word/preceding-sibling::*[@force='strong'][1]"/>        
    	<xsl:variable name="followingPC" select="$word/following-sibling::*[@force='strong'][1]"/>
    	<xsl:choose>
    			<xsl:when test="$precedingPC/following-sibling::*[position() > 0 and not(position() > ign:getPosition($followingPC)
    				- ign:getPosition($precedingPC))][@subtype='motion_initial']">
    				<xsl:value-of select="true()"/>
    			</xsl:when>
    			<xsl:otherwise><xsl:value-of select="false()"/></xsl:otherwise>
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