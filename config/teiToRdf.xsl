<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
    xmlns:ign="http://example.com/namespace/" 
    xmlns:xs="http://www.w3.org/2001/XMLSchema" xpath-default-namespace="http://www.tei-c.org/ns/1.0" 
    xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" 
    xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#">
    
    <xsl:output method="xml" encoding="UTF-8" indent="yes" />
    
    <xsl:template match="/">
        <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#">
            <xsl:for-each select="TEI/bag/*[@xml:id]">
                <xsl:call-template name="create_resource">
                    <xsl:with-param name="toponym" select="." />
                </xsl:call-template>
            </xsl:for-each>
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
        <xsl:variable name="preceding_strong_pc" select="$orientation/preceding-sibling::*[@force='strong'][1]"/>
        <!-- borne inf -->
        <xsl:variable name="preceding_strong_pc_position" select="ign:getPosition($preceding_strong_pc)"/>
        <!-- borne sup -->
        <xsl:variable name="last_strong_pc_position" select="ign:getPosition($last_strong_pc)"/>
        <!--<xsl:element name="rdf:Seq">-->
            <xsl:variable name="following_pcs" select="$preceding_strong_pc/following-sibling::*[position() > 1 
                and not(position() > $last_strong_pc_position)][@force='strong']"/>
            <xsl:variable name="lists" select="ign:get_list($following_pcs, $last_strong_pc_position, $last_strong_pc)"/>
            <xsl:element name="rdf:Seq">
                <xsl:for-each select="$lists[name()='rdf:li']">
                    <xsl:copy-of select="."/>
                </xsl:for-each>
            </xsl:element>  
            <xsl:for-each select="$lists[not(name()='rdf:li')]">
                <xsl:copy-of select="."/>
            </xsl:for-each>
            
        <!--</xsl:element>-->
    </xsl:template>
    
    <!-- affiche la liste de manière conforme au format RDF -->
    <xsl:function name="ign:get_list" as="element()*">
        <xsl:param name="following_pcs" as="element()*" />
        <xsl:param name="last_strong_pc_position" as="xs:integer" />
        <xsl:param name="last_strong_pc" as="element()?" />
        <xsl:for-each select="$following_pcs">
            <!-- On récupère tout les pc strong entre les deux bornes -->
            <xsl:variable name="current_position" select="ign:getPosition(.)"/>
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
                        <xsl:copy-of select="ign:create_list($sentence, $number_of_bags, 1)"/>
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
                        <xsl:copy-of select="ign:create_list($sentence, $number_of_bags, 1)"/>
                    </xsl:if>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:for-each>
    </xsl:function>
    
    <!-- créer une liste à partir de la phrase passée en paramètre -->
    <xsl:function name="ign:create_list" as="element()+">
        <xsl:param name="sentence" as="element()+" />
        <xsl:param name="total" as="xs:integer" />
        <xsl:param name="index" as="xs:integer"/>
        
        <xsl:choose>
            <xsl:when test="$index = 1">
                <rdf:li>
                    <xsl:call-template name="element_of_list">
                        <xsl:with-param name="sentence" select="$sentence" />
                        <xsl:with-param name="index" select="$index" />
                        <xsl:with-param name="total" select="$total" />
                    </xsl:call-template>
                </rdf:li>
            </xsl:when>
            <xsl:otherwise>
                <xsl:call-template name="element_of_list">
                    <xsl:with-param name="sentence" select="$sentence" />
                    <xsl:with-param name="index" select="$index" />
                    <xsl:with-param name="total" select="$total" />
                </xsl:call-template>
            </xsl:otherwise>
        </xsl:choose>
        <xsl:if test="$total > $index">
            <xsl:copy-of select="ign:create_list($sentence, $total, $index + 1)"/>
        </xsl:if>
    </xsl:function>
    
    <xsl:template name="element_of_list">    
        <xsl:param name="sentence" as="element()+" />
        <xsl:param name="index" as="xs:integer" />
        <xsl:param name="total" as="xs:integer" />
        <xsl:element name="rdf:Description">
            <xsl:attribute name="rdf:nodeID">                    
                <xsl:text>bag</xsl:text><xsl:value-of select="$sentence[$index]/descendant::*[@xml:id][1]/@xml:id" />
            </xsl:attribute>
            <xsl:element name="rdf:first">
                <xsl:copy-of select="ign:create_bag($sentence[$index])" />
            </xsl:element>
            <xsl:choose>
                <xsl:when test="$index = $total">
                    <xsl:element name="rdf:rest">
                        <xsl:attribute name="rdf:resource">
                            <xsl:text>rdf:nil</xsl:text>
                        </xsl:attribute>
                    </xsl:element>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:element name="rdf:rest">
                        <xsl:attribute name="rdf:nodeID">
                            <xsl:text>bag</xsl:text><xsl:value-of select="$sentence[$index + 1]/child::*[@xml:id][1]/@xml:id" />
                        </xsl:attribute>
                    </xsl:element>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:element>
    </xsl:template>
    
    <xsl:function name="ign:create_bag" as="element()">
        <xsl:param name="bag" as="element()" />
        <xsl:element name="rdf:Bag">
            <xsl:for-each select="$bag/child::*[@xml:id]">
                <xsl:element name="rdf:li">
                    <xsl:attribute name="rdf:resource">
                        <xsl:text>ign:</xsl:text>
                        <xsl:value-of select="./@xml:id" />
                    </xsl:attribute>
                </xsl:element>
            </xsl:for-each>
        </xsl:element>
    </xsl:function>
    
    <xsl:template name="create_resource">
        <xsl:param name="toponym" as="element()" />
        <xsl:element name="rdf:Description">
            <xsl:attribute name="rdf:about">
                <xsl:text>ign:</xsl:text>
                <xsl:value-of select="./@xml:id" />
            </xsl:attribute>
            <xsl:element name="rdfs:label">
                <xsl:attribute name="xml:lang">
                    <xsl:text>fr</xsl:text>
                </xsl:attribute>
                <xsl:value-of select="./text()" />
            </xsl:element>
        </xsl:element>
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