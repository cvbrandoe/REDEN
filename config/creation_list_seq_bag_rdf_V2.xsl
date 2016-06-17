<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
    xmlns:ign="http://example.com/namespace/" 
    xmlns:xs="http://www.w3.org/2001/XMLSchema" xpath-default-namespace="http://www.tei-c.org/ns/1.0" 
    xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" 
    xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"
    xmlns:dbpedia-owl="http://dbpedia.org/ontology/"
    xmlns:iti="http://data.ign.fr/def/itineraires#"
    xmlns:rlsp="http://data.ign.fr/def/relationsspatiales#">
    
    <xsl:output method="xml" encoding="UTF-8" indent="yes" />
    
    <xsl:template match="/">
        <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#">
            <xsl:for-each select="//*[(@type='orientation' or @subtype='orientation') 
            and (preceding-sibling::*[1][(@type='PREPDET' or (@type='DET' and preceding-sibling::*[1][@type='PREP']) 
            or @lemma='vers') and preceding-sibling::*[1][not(@type='N')] and preceding-sibling::*[2][not(@type='N')]])]">
                <xsl:call-template name="create_seq">
                    <xsl:with-param name="orientation" select="." />
                </xsl:call-template>
            </xsl:for-each> 
        </rdf:RDF>
    </xsl:template>
    
    <!-- Récupère les phrases attachées à cette orientation. Les bag formeront des list (une par phrase) et c'est listes formeront une séquence -->
    <xsl:template name="create_seq">
        <xsl:param name="orientation" as="element()" />
        <xsl:variable name="following_orientation" select="$orientation/following-sibling::*[(@type='orientation' or @subtype='orientation')
                    and (preceding-sibling::*[1][(@type='PREPDET' or (@type='DET' and preceding-sibling::*[1][@type='PREP']) or @lemma='vers') 
                        and preceding-sibling::*[1][not(@type='N')] and preceding-sibling::*[2][not(@type='N')]])][1]"/>
        <!-- pc forte avant l'orientation suivante -->
        <xsl:variable name="last_strong_pc" select="$following_orientation/preceding-sibling::*[@force='strong'][1]"/>
        <xsl:variable name="isFirstOrientation" select="count($orientation/preceding-sibling::*[(@type='orientation' or @subtype='orientation')
                    and (preceding-sibling::*[1][(@type='PREPDET' or (@type='DET' and preceding-sibling::*[1][@type='PREP']) or @lemma='vers') 
                        and preceding-sibling::*[1][not(@type='N')] and preceding-sibling::*[2][not(@type='N')]])]) = 0"/>
        <xsl:variable name="isLastOrientation" select="count($orientation/following-sibling::*[(@type='orientation' or @subtype='orientation')
                    and (preceding-sibling::*[1][(@type='PREPDET' or (@type='DET' and preceding-sibling::*[1][@type='PREP']) or @lemma='vers') 
                        and preceding-sibling::*[1][not(@type='N')] and preceding-sibling::*[2][not(@type='N')]])]) = 0"/>
        <xsl:variable name="preceding_strong_pc" select="$orientation/preceding-sibling::*[@force='strong'][1]"/>
        <!-- borne inf -->
        <xsl:variable name="preceding_strong_pc_position" select="ign:getPrecedingPCOrFirstPosition(
        	ign:getPosition($preceding_strong_pc), $isFirstOrientation)"/>
        <!-- borne sup -->
        <xsl:variable name="last_strong_pc_position" select="ign:getPosition($last_strong_pc)"/>
        <xsl:choose>
        	<xsl:when test="$isFirstOrientation and $last_strong_pc_position > $preceding_strong_pc_position">
        		<!-- following_pcs doit contenir tous les PC jusqu'à $last_strong_pc_position inclus. -->
        		<xsl:variable name="firstPCPrecedingSibling" select="$preceding_strong_pc/
        		preceding-sibling::*[@force='strong'][last()]/preceding-sibling::*[1]"/>
        		<xsl:variable name="following_pcs" select="$firstPCPrecedingSibling/following-sibling::*[not(position() > $last_strong_pc_position)][@force='strong']"/>
	            <xsl:variable name="lists" select="ign:get_list($following_pcs, $last_strong_pc_position, 
	                $last_strong_pc, $preceding_strong_pc_position)"/>
	            <xsl:element name="rdf:Seq">
	                <xsl:call-template name="create_route">
	                    <xsl:with-param name="lists" select="$lists" />
	                </xsl:call-template>
	            </xsl:element>  
        	</xsl:when>
	        <xsl:when test="$last_strong_pc_position > $preceding_strong_pc_position">
	            <xsl:variable name="following_pcs" select="$preceding_strong_pc/following-sibling::*[position() > 1 
	                and not(position() > $last_strong_pc_position)][@force='strong']"/>
	            <xsl:variable name="lists" select="ign:get_list($following_pcs, $last_strong_pc_position, 
	                $last_strong_pc, $preceding_strong_pc_position)"/>
	            <xsl:element name="rdf:Seq">
	                <xsl:call-template name="create_route">
	                    <xsl:with-param name="lists" select="$lists" />
	                </xsl:call-template>
	            </xsl:element>  
	        </xsl:when>
	        <xsl:otherwise>
	        	<xsl:variable name="following_pcs" select="$preceding_strong_pc/following-sibling::*[@force='strong']"/>
	            <xsl:variable name="lists" select="ign:get_list($following_pcs, ign:getPosition($following_pcs[last()]), 
	                $following_pcs[last()], $preceding_strong_pc_position)"/>
	            <xsl:element name="rdf:Seq">
	                <xsl:call-template name="create_route">
	                    <xsl:with-param name="lists" select="$lists" />
	                </xsl:call-template>
	            </xsl:element>  
	        </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    <xsl:template name="create_route">
    	<xsl:param name="lists" as="element()*" />
    	<xsl:for-each select="$lists">
                <xsl:element name="rdf:li">
                    <xsl:element name="rdf:Description">
                        <xsl:element name="rdf:type">
                            <xsl:attribute name="rdf:resource">                    
                                <xsl:text>iti:Route</xsl:text>
                            </xsl:attribute>
                        </xsl:element> 
                        <xsl:element name="iti:waypoints">
                            <xsl:element name="rdf:Description"> <!-- liste des waypoints -->
                                <xsl:copy-of select="."/>                                
                            </xsl:element>   
                        </xsl:element>   
                    </xsl:element>  
                </xsl:element>  
            </xsl:for-each>
    </xsl:template>
    
    <!-- Return the orientation's position if it's not the first one. Else return 0 -->
    <xsl:function name="ign:getPrecedingPCOrFirstPosition" as="xs:integer">
        <xsl:param name="preceding_strong_pc_position" as="xs:integer" />
        <xsl:param name="isFirstOrientation" as="xs:boolean" />
        
        <xsl:choose>
        	<xsl:when test="$isFirstOrientation"><xsl:sequence select="0"/></xsl:when>
        	<xsl:otherwise><xsl:sequence select="$preceding_strong_pc_position"/></xsl:otherwise>
        </xsl:choose>
    </xsl:function>
        
    <!-- affiche la liste de manière conforme au format RDF -->
    <xsl:function name="ign:get_list" as="xs:integer"><!-- element()* -->
        <xsl:param name="following_pcs" as="element()*" />
        <xsl:param name="last_strong_pc_position" as="xs:integer" />
        <xsl:param name="last_strong_pc" as="element()" />
        <xsl:param name="preceding_strong_pc_position" as="xs:integer" />
        <xsl:for-each select="$following_pcs">
            <!-- On récupère tout les pc strong entre les deux bornes -->
            <xsl:variable name="current_position" select="ign:getPosition(.)"/>
            <xsl:if test="$current_position >= $preceding_strong_pc_position and $last_strong_pc_position >= $current_position">
                <xsl:choose>
                    <xsl:when test="$current_position = $last_strong_pc_position">
                        <!-- C'est la dernière phrase -->
                        <!-- on appelle le template create_list en passant les éléments 
                            compris entre la pc avant last_strong_pc_position et last_strong_pc_position-->
                        <xsl:variable name="preceding_pc_position" select="ign:getPosition($last_strong_pc/preceding-sibling::*[ @force='strong'][1])"/>
                        <xsl:variable name="number_of_bags" select="count(//*[position() > 
                            $preceding_pc_position and not(position() > $last_strong_pc_position)][name() = 'bag'])" />
                        <xsl:if test="$number_of_bags > 0">
                            <xsl:variable name="sentence" select="//*[position() > 
                                        $preceding_pc_position and not(position() > $last_strong_pc_position)][name() = 'bag']" />
                            <xsl:copy-of select="count(//*[position() > 
                                        $preceding_pc_position and not(position() > $last_strong_pc_position)][name() = 'bag'])"/>
                        </xsl:if>
                    </xsl:when>
                    <xsl:otherwise>
                        <!-- Ce n'est pas la dernière phrase -->
                        <!-- on appelle le template create_list en passant les éléments 
                            compris entre la pc avant l'élément courant et l'élément courant-->
                        <xsl:variable name="preceding_pc_position" select="ign:getPosition(preceding-sibling::*[@force='strong'][1])"/>
                        <xsl:variable name="number_of_bags" select="count(//*[position() > 
                            $preceding_pc_position and not(position() > $current_position)][name() = 'bag'])" />
                        <xsl:if test="$number_of_bags > 0">
                            <xsl:variable name="sentence" select="//*[position() > 
                                    $preceding_pc_position and not(position() > $current_position)][name() = 'bag']" />
                            <xsl:copy-of select="count(//*[position() > 
                                    $preceding_pc_position and not(position() > $current_position)][name() = 'bag'])"/>
                        </xsl:if>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:if>
        </xsl:for-each>
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