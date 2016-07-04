<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
    xmlns:ign="http://example.com/namespace/" 
    xmlns:xs="http://www.w3.org/2001/XMLSchema" 
    xmlns:xsd="http://www.w3.org/2001/XMLSchema-datatypes#" 
    xpath-default-namespace="http://www.tei-c.org/ns/1.0" 
    xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" 
    xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"
    xmlns:dbo="http://dbpedia.org/ontology/"
    xmlns:iti="http://data.ign.fr/def/itineraires#"
    xmlns:rlsp="http://data.ign.fr/def/relationsspatiales#">
    
    <xsl:output method="xml" encoding="UTF-8" indent="yes" />
    
    <xsl:template match="/">
        <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#">
        	<xsl:variable name="rlspList" as="element()+" select="//*[name()='rlsp']"/>
        	<!-- <xsl:for-each select="/TEI/*[position() > 2 and not(position() > 4)]"><xsl:copy-of select="."/></xsl:for-each> -->
            <xsl:for-each select="TEI/p">
            	<xsl:variable name="currentParagraph" select="current()"/>
	            <xsl:for-each select="current()/child::*[(@type='orientation' or @subtype='orientation') 
	            and (preceding-sibling::*[1][(@type='PREPDET' or (@type='DET' and preceding-sibling::*[1][@type='PREP']) 
	            or @lemma='vers') and preceding-sibling::*[1][not(@type='N')] and preceding-sibling::*[2][not(@type='N')]])]">
	                <xsl:call-template name="create_seq">
	                    <xsl:with-param name="orientation" select="." />
	                    <xsl:with-param name="rlspList" select="$rlspList" />
	                    <xsl:with-param name="currentParagraph" select="$currentParagraph" />
	                </xsl:call-template>
	            </xsl:for-each> 
            </xsl:for-each> 
        </rdf:RDF>
    </xsl:template>
    
    <!-- Récupère les phrases attachées à cette orientation. Les bag formeront des list (une par phrase) et c'est listes formeront une séquence -->
    <xsl:template name="create_seq">
        <xsl:param name="orientation" as="element()" />
        <xsl:param name="rlspList" as="element()+" />
        <xsl:param name="currentParagraph" as="element()" />
        <!-- <xsl:element name="orientation"><xsl:copy-of select="ign:getPosition($orientation)" /></xsl:element> -->
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
        		<xsl:variable name="following_pcs" select="$firstPCPrecedingSibling/
        			following-sibling::*[not(position() > ($last_strong_pc_position - ign:getPosition($firstPCPrecedingSibling)))]
        			[@force='strong']"/>
	            <xsl:variable name="lists" select="ign:get_list($following_pcs, $last_strong_pc_position, 
	                $last_strong_pc, $preceding_strong_pc_position, $rlspList, $currentParagraph)"/>
	            <xsl:element name="rdf:Seq">
	                <xsl:for-each select="$lists[name()='rdf:li']">
	                    <xsl:copy-of select="."/>
	                </xsl:for-each>
	            </xsl:element>  
	            <xsl:for-each select="$lists[not(name()='rdf:li')]">
	                <xsl:copy-of select="."/>
	            </xsl:for-each>
        	</xsl:when>
	        <xsl:when test="$last_strong_pc_position > $preceding_strong_pc_position">
	            <xsl:variable name="following_pcs" as="element()*">
	                <xsl:choose>
	                	<xsl:when test="$preceding_strong_pc">
	                		<xsl:sequence select="$preceding_strong_pc/following-sibling::*[position() > 1 
	                and not(position() > ($last_strong_pc_position - $preceding_strong_pc_position))][@force='strong']"/>
	                	</xsl:when>
	                	<xsl:otherwise>
		                	<xsl:sequence select="$orientation/following-sibling::*[position() > 1 
		                and not(position() > ($last_strong_pc_position - $preceding_strong_pc_position))][@force='strong']"/>
	                </xsl:otherwise>
	                </xsl:choose>
                </xsl:variable>
	            <xsl:variable name="lists" select="ign:get_list($following_pcs, $last_strong_pc_position, 
	                $last_strong_pc, $preceding_strong_pc_position, $rlspList, $currentParagraph)"/>
	            <xsl:element name="rdf:Seq">
        			<!-- <xsl:copy-of select="$preceding_strong_pc"/> -->
	                <xsl:for-each select="$lists[name()='rdf:li']">
	                    <xsl:copy-of select="."/>
	                </xsl:for-each>
	            </xsl:element>  
	            <xsl:for-each select="$lists[not(name()='rdf:li')]">
	                <xsl:copy-of select="."/>
	            </xsl:for-each>
	        </xsl:when>
	        <xsl:when test="$preceding_strong_pc_position > $last_strong_pc_position">
	        	<xsl:variable name="following_pcs" select="$preceding_strong_pc/following-sibling::*[@force='strong']"/>
	            <xsl:variable name="lists" select="ign:get_list($following_pcs, ign:getPosition($following_pcs[last()]), 
	                $following_pcs[last()], $preceding_strong_pc_position, $rlspList, $currentParagraph)"/>
	            <xsl:element name="rdf:Seq">
	                <xsl:for-each select="$lists[name()='rdf:li']">
	                    <xsl:copy-of select="."/>
	                </xsl:for-each>
	            </xsl:element>  
	            <xsl:for-each select="$lists[not(name()='rdf:li')]">
	                <xsl:copy-of select="."/>
	            </xsl:for-each>
	        </xsl:when>
        </xsl:choose>
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
    <xsl:function name="ign:get_list" as="element()*">
        <xsl:param name="following_pcs" as="element()*" />
        <xsl:param name="last_strong_pc_position" as="xs:integer" />
        <xsl:param name="last_strong_pc" as="element()" />
        <xsl:param name="preceding_strong_pc_position" as="xs:integer" />
        <xsl:param name="rlspList" as="element()+" />
        <xsl:param name="currentParagraph" as="element()" />
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
                            <xsl:variable name="sentence" select="$currentParagraph/child::*[position() > 
                                        $preceding_pc_position and not(position() > $last_strong_pc_position)][name() = 'bag']" />
                            <xsl:copy-of select="ign:create_list($sentence, $number_of_bags, 1, $rlspList)"/>
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
                            <xsl:variable name="sentence" select="$currentParagraph/child::*[position() > 
                                    $preceding_pc_position and not(position() > $current_position)][name() = 'bag']" />
                            <xsl:copy-of select="ign:create_list($sentence, $number_of_bags, 1, $rlspList)"/>
                        </xsl:if>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:if>
        </xsl:for-each>
    </xsl:function>
    
    <!-- créer une liste à partir de la phrase passée en paramètre -->
    <xsl:function name="ign:create_list" as="element()+">
        <xsl:param name="sentence" as="element()+" />
        <xsl:param name="total" as="xs:integer" />
        <xsl:param name="index" as="xs:integer"/>
        <xsl:param name="rlspList" as="element()+"/>
        <!-- <xsl:element name="index"><xsl:value-of select="$index"/></xsl:element>
        <xsl:element name="total"><xsl:value-of select="$total"/></xsl:element> -->
        <xsl:choose>
            <xsl:when test="$index = 1">
                <rdf:li>
                    <xsl:call-template name="element_of_list">
                        <xsl:with-param name="sentence" select="$sentence" />
                        <xsl:with-param name="index" select="$index" />
                        <xsl:with-param name="total" select="$total" />
                        <xsl:with-param name="rlspList" select="$rlspList" />
                    </xsl:call-template>
                </rdf:li>
            </xsl:when>
            <xsl:otherwise>
                <xsl:call-template name="element_of_list">
                    <xsl:with-param name="sentence" select="$sentence" />
                    <xsl:with-param name="index" select="$index" />
                    <xsl:with-param name="total" select="$total" />
                    <xsl:with-param name="rlspList" select="$rlspList" />
                </xsl:call-template>
            </xsl:otherwise>
        </xsl:choose>
        <xsl:if test="$total > $index">
            <xsl:copy-of select="ign:create_list($sentence, $total, $index + 1, $rlspList)"/>
        </xsl:if>
    </xsl:function>
    
    <xsl:template name="element_of_list">    
        <xsl:param name="sentence" as="element()+" />
        <xsl:param name="index" as="xs:integer" />
        <xsl:param name="total" as="xs:integer" />
        <xsl:param name="rlspList" as="element()+" />
        <xsl:choose>
        	<xsl:when test="$index = 1">
        		<xsl:element name="rdf:Description">  
		            <xsl:element name="rdf:type">            
			            <xsl:attribute name="rdf:resource">                    
			            	<xsl:text>http://data.ign.fr/id/itineraire/route/</xsl:text>
			                <xsl:value-of select="ign:getPosition($sentence[$index])" />
			            </xsl:attribute>
		            </xsl:element>
		            <xsl:element name="iti:waypoints">
		            	<xsl:element name="rdf:Description">
				            <xsl:element name="rdf:first">
				                <xsl:copy-of select="ign:create_bag($sentence[$index], $rlspList)" />
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
		            </xsl:element>
		        </xsl:element>
        	</xsl:when>
        	<xsl:otherwise>
        		<xsl:element name="rdf:Description">       	
		           <xsl:attribute name="rdf:nodeID">                    
		               <xsl:text>bag</xsl:text><xsl:value-of select="$sentence[$index]/descendant::*[@xml:id][1]/@xml:id" />
		           </xsl:attribute>
		            <xsl:element name="rdf:first">
		                <xsl:copy-of select="ign:create_bag($sentence[$index], $rlspList)" />
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
        	</xsl:otherwise>
        </xsl:choose>
        
    </xsl:template>
    
    <xsl:function name="ign:create_bag" as="element()">
        <xsl:param name="bag" as="element()" />
        <xsl:param name="rlspList" as="element()+" />
        <xsl:element name="rdf:Bag">
            <xsl:for-each select="$bag/child::*[@xml:id]"> 
                <xsl:variable name="toponym" select="." />
                <xsl:variable name="date" as="element()?">
	                <xsl:if test="parent::*[@date]">
                			<xsl:choose>
                				<xsl:when test="parent::*[@date]/@date = 'motion_initial'">
			                		<xsl:element name="iti:departureDate">
			                			<xsl:attribute name="rdf:datatype"><xsl:text>xsd:date</xsl:text></xsl:attribute>
			                			<xsl:choose>
			                				<xsl:when test="parent::*[@when]"><xsl:value-of select="parent::*/@when"/></xsl:when>
			                				<xsl:otherwise><xsl:value-of select="parent::*/@from"/></xsl:otherwise>
			                			</xsl:choose>	                			
			                		</xsl:element>
		                		</xsl:when>
                				<xsl:otherwise>
			                		<xsl:element name="iti:arrivalDate">
			                			<xsl:attribute name="rdf:datatype"><xsl:text>xsd:date</xsl:text></xsl:attribute>
			                			<xsl:choose>
			                				<xsl:when test="parent::*[@when]"><xsl:value-of select="parent::*/@when"/></xsl:when>
			                				<xsl:otherwise><xsl:value-of select="parent::*/@from"/></xsl:otherwise>
			                			</xsl:choose>	                			
			                		</xsl:element>
                				</xsl:otherwise>
                			</xsl:choose>
					</xsl:if> 
				</xsl:variable>
           		<xsl:element name="rdf:li">
           			<xsl:variable name="waypointsOrlandmarks" as="element()+">
		            	<xsl:for-each select="ign:create_resource($toponym, $rlspList)">
		            			<xsl:choose>
		            				<xsl:when test="$bag[@landmark]">            					
					            		<xsl:element name="iti:Landmark">
					                	
						                	<xsl:element name="iti:spatialReference">
												<xsl:copy-of select="."/>
						                    </xsl:element>                		
						                	<xsl:if test="$date">
						                		<xsl:copy-of select="$date"/>
											</xsl:if> 
					                	</xsl:element> 
		            				</xsl:when>
		            				<xsl:otherwise>
					            		<xsl:element name="iti:Waypoint">
					                	
						                	<xsl:element name="iti:spatialReference">
												<xsl:copy-of select="."/>
						                    </xsl:element>                		
						                	<xsl:if test="$date">
						                		<xsl:copy-of select="$date"/>
											</xsl:if> 
					                	</xsl:element> 
		            				</xsl:otherwise>
		            			</xsl:choose>   
		            	</xsl:for-each> 
	            	</xsl:variable> 
	            	<xsl:choose>
	            		<xsl:when test="count($waypointsOrlandmarks) > 1">
	            			<rdf:Alt>
	            				<xsl:for-each select="$waypointsOrlandmarks">
	            					<rdf:li><xsl:copy-of select="."/></rdf:li>
	            				</xsl:for-each>
	            			</rdf:Alt>
	            		</xsl:when>
	            		<xsl:otherwise><xsl:copy-of select="$waypointsOrlandmarks"/></xsl:otherwise>
	            	</xsl:choose>           	
           		</xsl:element>  
            </xsl:for-each>
        </xsl:element>
    </xsl:function>
    
    <xsl:function name="ign:get_type" as="element()*">
    	<xsl:param name="typage" as="attribute()?" />
    	 <xsl:choose>
	        <xsl:when test="$typage">
	            <xsl:choose>
	                <xsl:when test="contains(lower-case($typage), 'mountainorvolcano')">
       					<xsl:element name="rdf:type">
	                         <xsl:attribute name="rdf:resource">
	                             <xsl:text>dbo:</xsl:text>
	                             <xsl:text>Mountain</xsl:text>
	                         </xsl:attribute>
                        </xsl:element>
            			<xsl:element name="rdf:type">
	                            <xsl:attribute name="rdf:resource">
	                                <xsl:text>dbo:</xsl:text>
	                                <xsl:text>Volcano</xsl:text>
	                            </xsl:attribute>
						</xsl:element>
	                </xsl:when>
	                <xsl:when test="contains(lower-case($typage), 'bodyofwater')">	                
            			<xsl:element name="rdf:type">
		                    <xsl:attribute name="rdf:resource">
		                        <xsl:text>dbo:</xsl:text>
		                        <xsl:text>BodyOfWater</xsl:text>
		                    </xsl:attribute>
	                    </xsl:element>
	                </xsl:when>
	                <xsl:when test="contains(lower-case($typage), 'settlement')">	                
            			<xsl:element name="rdf:type">
		                    <xsl:attribute name="rdf:resource">
		                        <xsl:text>dbo:</xsl:text>
		                        <xsl:text>Settlement</xsl:text>
		                    </xsl:attribute>     
	                    </xsl:element>                       
	                </xsl:when>
	                <xsl:when test="contains(lower-case($typage), 'territoryornaturalplace')">
            			<xsl:element name="rdf:type">
                            <xsl:attribute name="rdf:resource">
                                <xsl:text>dbo:</xsl:text>
                                <xsl:text>Territory</xsl:text>
                            </xsl:attribute>
						</xsl:element>
            			<xsl:element name="rdf:type">
                            <xsl:attribute name="rdf:resource">
                                <xsl:text>dbo:</xsl:text>
                                <xsl:text>NaturalPlace</xsl:text>
                            </xsl:attribute>
                        </xsl:element>                           
	                </xsl:when>
	                <xsl:otherwise>
            			<xsl:element name="rdf:type">
		                    <xsl:attribute name="rdf:resource">
		                        <xsl:text>dbo:</xsl:text>
		                        <xsl:text>Place</xsl:text>
		                    </xsl:attribute> 
	                    </xsl:element>
	                </xsl:otherwise>
	            </xsl:choose>
	        </xsl:when>
	        <xsl:otherwise>	        
            	<xsl:element name="rdf:type">
		            <xsl:attribute name="rdf:resource">
		                <xsl:text>dbo:</xsl:text>
		                <xsl:text>Place</xsl:text>
		            </xsl:attribute> 
	            </xsl:element>
	        </xsl:otherwise>
	    </xsl:choose>
    </xsl:function>
    
    <xsl:function name="ign:create_resource" as="element()*">
        <xsl:param name="toponym" as="element()" />
        <xsl:param name="rlspList" as="element()+" />
        
        <xsl:variable name="idTopo" select="$toponym/@xml:id" as="xs:integer"/>
        
        <xsl:variable name="labelTopo" as="element()">
        	<xsl:element name="rdfs:label">
	            <xsl:attribute name="xml:lang">
	                <xsl:text>fr</xsl:text>
	            </xsl:attribute>
	            <xsl:value-of select="$toponym/text()" />
	        </xsl:element>
        </xsl:variable>   

         <xsl:variable name="orientationTopo" as="element()*">
         	<xsl:for-each select="$rlspList[@root=$idTopo]">
	            <xsl:copy-of select="ign:getOrientationOntology('start', ./@orientation, ./@target)"/>
         	</xsl:for-each>
        </xsl:variable>
        
        <xsl:variable name="typeTopo" as="element()*" select="ign:get_type($toponym/@typage)"/>
        
        <xsl:for-each select="$typeTopo">
        	<xsl:element name="rdf:Description">  
       			<xsl:attribute name="rdf:about">
       				<xsl:text>http://data.ign.fr/id/propagation/Place/</xsl:text>
       				<xsl:choose>       					
       					<xsl:when test="count($typeTopo) > 1 and not(./@rdf:resource=$typeTopo[1]/@rdf:resource)"><xsl:value-of select="concat($idTopo, '_1')" /></xsl:when>
       					<xsl:otherwise><xsl:value-of select="$idTopo" /></xsl:otherwise>
       				</xsl:choose>
   				</xsl:attribute>     		
       			<xsl:copy-of select="."></xsl:copy-of> <!-- type -->
        		<xsl:element name="xml:id">
        			<xsl:attribute name="rdf:datatype"><xsl:text>xs:integer</xsl:text></xsl:attribute>
        			<xsl:value-of select="$idTopo" />
       			</xsl:element> 
        		<xsl:copy-of select="$labelTopo"></xsl:copy-of>
        		<xsl:if test="$orientationTopo">
        			<xsl:copy-of select="$orientationTopo"></xsl:copy-of>
        		</xsl:if>
        		<xsl:copy-of select="ign:createIncludes($toponym, current())" />
        	</xsl:element>
        </xsl:for-each>
        
    </xsl:function>
    
    <xsl:function name="ign:createIncludes" as="element()*">
    	<xsl:param name="toponym" as="element()"/>
    	<xsl:param name="typeTopo" as="element()"/>
    	<xsl:if test="count($toponym[parent::*[child::*[name()='include']]]) > 0">
	    	<!-- <test>
	    		<xsl:copy-of select="$toponym[parent::*[child::*[name()='include']]]"/>
	    	</test> -->
    		<xsl:variable name="ids" select="$toponym/parent::*[1]/child::*[name()='include']/@targetId" />
    		<xsl:for-each select="$ids">
    			<xsl:variable name="id" select="current()" as="xs:integer"/>
		    	<xsl:choose>
		    		<xsl:when test="$typeTopo[@rdf:resource='dbo:NaturalPlace' or @rdf:resource='dbo:BodyOfWater'
		    			or @rdf:resource='dbo:Mountain' or @rdf:resource='dbo:Volcano']">
		    			<xsl:element name="rlsp:near">
                            <xsl:attribute name="rdf:resource">
                            	<xsl:text>http://data.ign.fr/id/propagation/Place/</xsl:text>
                            	<xsl:copy-of select="$id"/>
                            </xsl:attribute>
                        </xsl:element>
	    			</xsl:when>
		    		<xsl:when test="$typeTopo[@rdf:resource='dbo:Territory']">
		    			<xsl:element name="rlsp:contains">
                            <xsl:attribute name="rdf:resource">
                            	<xsl:text>http://data.ign.fr/id/propagation/Place/</xsl:text>
                            	<xsl:copy-of select="$id"/>
                            </xsl:attribute>
                        </xsl:element>
		    		</xsl:when>
		    	</xsl:choose>
    		</xsl:for-each>
    	</xsl:if>
    </xsl:function>
    
    <!-- <xsl:function name="increment" as="xs:integer">
    	<xsl:param name="count" as="xs:integer"/>
    	<xsl:value-of select="$count + 1"/>
    </xsl:function> -->
    
    <!-- Return the end of the triple with the orientation 
    eg : <rlsp:northOf rdf:resource="3" />-->
    <xsl:function name="ign:getOrientationOntology" as="element()">
        <xsl:param name="position" as="xs:string" />
        <xsl:param name="orientation" as="attribute()" />
        <xsl:param name="second_node" as="attribute()" />
        <xsl:variable name="resourceURI" as="xs:string">
        	<xsl:text>http://data.ign.fr/id/propagation/Place/</xsl:text>
		</xsl:variable>
        <xsl:choose>
            <xsl:when test="lower-case($position) = 'start'">
                <xsl:choose>
                    <xsl:when test="contains(lower-case($orientation), 'nord')"> 
                        <xsl:choose>                       
                            <xsl:when test="contains(lower-case($orientation), 'ouest')">                            
                                <xsl:element name="rlsp:northWestOf">
                                    <xsl:attribute name="rdf:resource">
                                    	<xsl:copy-of select="$resourceURI"/>
                                        <xsl:value-of select="$second_node"/>
                                    </xsl:attribute>
                                </xsl:element>
                            </xsl:when>                       
                            <xsl:when test="contains(lower-case($orientation), 'est')">
                                <xsl:element name="rlsp:northEastOf">
                                    <xsl:attribute name="rdf:resource">
                                    	<xsl:copy-of select="$resourceURI"/>
                                        <xsl:value-of select="$second_node"/>
                                    </xsl:attribute>
                                </xsl:element>                           
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:element name="rlsp:northOf">
                                    <xsl:attribute name="rdf:resource">
                                    	<xsl:copy-of select="$resourceURI"/>
                                        <xsl:value-of select="$second_node"/>
                                    </xsl:attribute>
                                </xsl:element>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:when>
                    <xsl:when test="contains(lower-case($orientation), 'sud')"> 
                        <xsl:choose>                       
                            <xsl:when test="contains(lower-case($orientation), 'ouest')">
                                <xsl:element name="rlsp:southWestOf">
                                    <xsl:attribute name="rdf:resource">
                                    	<xsl:copy-of select="$resourceURI"/>
                                        <xsl:value-of select="$second_node"/>
                                    </xsl:attribute>
                                </xsl:element>
                            </xsl:when>                       
                            <xsl:when test="contains(lower-case($orientation), 'est')">
                                <xsl:element name="rlsp:southEastOf">
                                    <xsl:attribute name="rdf:resource">
                                    	<xsl:copy-of select="$resourceURI"/>
                                        <xsl:value-of select="$second_node"/>
                                    </xsl:attribute>
                                </xsl:element>                           
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:element name="rlsp:southOf">
                                    <xsl:attribute name="rdf:resource">
                                    	<xsl:copy-of select="$resourceURI"/>
                                        <xsl:value-of select="$second_node"/>
                                    </xsl:attribute>
                                </xsl:element>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:when>
                    <xsl:when test="contains(lower-case($orientation), 'ouest')">
                        <xsl:element name="rlsp:westOf">
                            <xsl:attribute name="rdf:resource">
                                    	<xsl:copy-of select="$resourceURI"/>
                                <xsl:value-of select="$second_node"/>
                            </xsl:attribute>
                        </xsl:element>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:element name="rlsp:eastOf">
                            <xsl:attribute name="rdf:resource">
                                    	<xsl:copy-of select="$resourceURI"/>
                                <xsl:value-of select="$second_node"/>
                            </xsl:attribute>
                        </xsl:element>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:when>
            <xsl:otherwise>
                <xsl:choose>
                    <xsl:when test="contains(lower-case($orientation), 'nord')">   
                        <xsl:choose>                     
                            <xsl:when test="contains(lower-case($orientation), 'ouest')">
                                <xsl:element name="rlsp:southEastOf">
                                    <xsl:attribute name="rdf:resource">
                                    	<xsl:copy-of select="$resourceURI"/>
                                        <xsl:value-of select="$second_node"/>
                                    </xsl:attribute>
                                </xsl:element>
                            </xsl:when>                       
                            <xsl:when test="contains(lower-case($orientation), 'est')">
                                <xsl:element name="rlsp:southWestOf">
                                    <xsl:attribute name="rdf:resource">
                                    	<xsl:copy-of select="$resourceURI"/>
                                        <xsl:value-of select="$second_node"/>
                                    </xsl:attribute>
                                </xsl:element>                           
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:element name="rlsp:southOf">
                                    <xsl:attribute name="rdf:resource">
                                    	<xsl:copy-of select="$resourceURI"/>
                                        <xsl:value-of select="$second_node"/>
                                    </xsl:attribute>
                                </xsl:element>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:when>
                    <xsl:when test="contains(lower-case($orientation), 'sud')">    
                        <xsl:choose>                    
                            <xsl:when test="contains(lower-case($orientation), 'ouest')">
                                <xsl:element name="rlsp:northEastOf">
                                    <xsl:attribute name="rdf:resource">
                                    	<xsl:copy-of select="$resourceURI"/>
                                        <xsl:value-of select="$second_node"/>
                                    </xsl:attribute>
                                </xsl:element>
                            </xsl:when>                       
                            <xsl:when test="contains(lower-case($orientation), 'est')">
                                <xsl:element name="rlsp:northWestOf">
                                    <xsl:attribute name="rdf:resource">
                                    	<xsl:copy-of select="$resourceURI"/>
                                        <xsl:value-of select="$second_node"/>
                                    </xsl:attribute>
                                </xsl:element>                           
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:element name="rlsp:northOf">
                                    <xsl:attribute name="rdf:resource">
                                    	<xsl:copy-of select="$resourceURI"/>
                                        <xsl:value-of select="$second_node"/>
                                    </xsl:attribute>
                                </xsl:element>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:when>
                    <xsl:when test="contains(lower-case($orientation), 'ouest')">
                        <xsl:element name="rlsp:eastOf">
                            <xsl:attribute name="rdf:resource">
                                    	<xsl:copy-of select="$resourceURI"/>
                                <xsl:value-of select="$second_node"/>
                            </xsl:attribute>
                        </xsl:element>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:element name="rlsp:westOf">
                            <xsl:attribute name="rdf:resource">
                                    	<xsl:copy-of select="$resourceURI"/>
                                <xsl:value-of select="$second_node"/>
                            </xsl:attribute>
                        </xsl:element>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:otherwise>
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