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
            <xsl:for-each select="TEI/p[descendant-or-self::*[@xml:id]]">
            	<xsl:variable name="currentParagraph" select="current()"/>
            	<xsl:variable name="orientations" select="$currentParagraph/child::*[(@type='orientation' or @subtype='orientation') 
    	and preceding-sibling::*[position() > 0 and not(position() > 2) 
    		and descendant-or-self::*[@lemma='vers' or @lemma='à' or @lemma='par']]]" as="element()*"/>
    			<xsl:copy-of select="ign:generateSequences($orientations, 1, $currentParagraph, $rlspList)"/>
            </xsl:for-each> 
        </rdf:RDF>
    </xsl:template>
    
    <xsl:function name="ign:generateSequences" as="element()+">
    	<xsl:param name="orientations" as="element()*"/>
    	<xsl:param name="index" as="xs:integer" />
    	<xsl:param name="p" as="element()" />
        <xsl:param name="rlspList" as="element()+" />
    	<!-- <xsl:param name="precedingResults" as="element()*"/> -->
    	<!-- <xsl:if test="$precedingResults and count($precedingResults) > 0">
    		<xsl:copy-of select="$precedingResults"/>
    	</xsl:if> -->
    	<xsl:variable name="orientation" as="element()?">
    		<xsl:if test="$orientations">
    			<xsl:sequence select="$orientations[$index]"/>
    		</xsl:if>
    	</xsl:variable>
    	<xsl:variable name="left" as="xs:integer" select="ign:getLeftBoundary($orientation)"/>
    	<xsl:variable name="right" as="xs:integer">
    		<xsl:choose>
    			<xsl:when test="count($orientations) > $index">
    				<xsl:sequence select="ign:getLeftBoundary($orientations[$index + 1]) - 1"/>
    			</xsl:when>
    			<xsl:when test="$orientation">
    				<xsl:sequence select="ign:getPosition($orientation/following-sibling::*[text()='.'][last()])"/>
    			</xsl:when>
    			<!-- Il n'y a pas d'orientation dans le paragraphe -->
    			<xsl:otherwise>
    				<xsl:sequence select="ign:getPosition($p/child::*[last()])"/>
    			</xsl:otherwise>
    		</xsl:choose>
    	</xsl:variable>
		<xsl:variable name="nbOfBags" as="xs:integer" select="count($p/child::*[position() > $left and not(position() > $right)][name()='bag'])"/>
    	<xsl:element name="rdf:Seq">
    		<ori><xsl:copy-of select="$orientation/child::*[@type='N' or @type='NPr']/text()"/></ori>
    		<left><xsl:copy-of select="$left"/></left>
    		<right><xsl:copy-of select="$right"/></right>
    		<nbBag><xsl:copy-of select="$nbOfBags"/></nbBag>
    		<xsl:copy-of select="ign:listFromSentences($p/child::*[position() > $left and not(position() > $right)][name()='bag'], 1, $rlspList)"/>
    	</xsl:element>
    	<xsl:if test="count($orientations) > $index">
    		<xsl:sequence select="ign:generateSequences($orientations, $index + 1, $p, $rlspList)" />
    	</xsl:if>
    </xsl:function>
    
    <xsl:function name="ign:listFromSentences" as="element()*">
    	<xsl:param name="bags" as="element()*"/>
    	<xsl:param name="index" as="xs:integer"/>
        <xsl:param name="rlspList" as="element()+" />
    	<xsl:variable name="bag" as="element()?" select="$bags[$index]"/>
    	<xsl:if test="$bag">
	    	<xsl:variable name="isFirst" as="xs:boolean" select="ign:isFirstOfTheSentence($bag)"/>
	    	<xsl:variable name="isLast" as="xs:boolean">
	    		<xsl:sequence select="ign:getPosition($bag/following-sibling::bag[1]) >= 
	    		ign:getPosition($bag/following-sibling::*[text()='.'][1])"/>
	    	</xsl:variable>
	    	<xsl:choose>
	    		<xsl:when test="$isFirst and $isLast">
	    			<xsl:element name="rdf:li">
	    				<xsl:element name="rdf:Description">  
				            <xsl:element name="rdf:type">            
					            <xsl:attribute name="rdf:resource">                    
					            	<xsl:text>http://data.ign.fr/id/itineraire/route/</xsl:text>
					                <xsl:value-of select="ign:getPosition($bag)" />
					            </xsl:attribute>
				            </xsl:element>				            
			    			<xsl:element name="iti:waypoints">
				            	<xsl:element name="rdf:Description">
						            <xsl:element name="rdf:first">
			    						<xsl:copy-of select="ign:create_bag($bag, $rlspList)"/>
						            </xsl:element>
				                    <xsl:element name="rdf:rest">
				                        <xsl:attribute name="rdf:resource">
				                            <xsl:text>rdf:nil</xsl:text>
				                        </xsl:attribute>
				                    </xsl:element>
				            	</xsl:element>
				            </xsl:element>
			            </xsl:element>
	    			</xsl:element>
	    		</xsl:when>
	    		<xsl:when test="$isFirst">
	    			<xsl:element name="rdf:li">
	    				<xsl:element name="rdf:Description">  
				            <xsl:element name="rdf:type">            
					            <xsl:attribute name="rdf:resource">                    
					            	<xsl:text>http://data.ign.fr/id/itineraire/route/</xsl:text>
					                <xsl:value-of select="ign:getPosition($bag)" />
					            </xsl:attribute>
				            </xsl:element>				            
			    			<xsl:element name="iti:waypoints">
				            	<xsl:element name="rdf:Description">
						            <xsl:element name="rdf:first">
			    						<xsl:copy-of select="ign:create_bag($bag, $rlspList)"/>
						            </xsl:element>
				                    <xsl:element name="rdf:rest">
				                        <xsl:element name="rdf:resource">
				                            <xsl:copy-of select="ign:listFromSentences($bags, $index + 1, $rlspList)"/>
				                        </xsl:element>
				                    </xsl:element>
				            	</xsl:element>
				            </xsl:element>
			            </xsl:element>
	    			</xsl:element>
	    		</xsl:when>
	    		<xsl:when test="$isLast">
	    			<xsl:element name="rdf:Description">
			            <xsl:element name="rdf:first">
	   						<xsl:copy-of select="ign:create_bag($bag, $rlspList)"/>
			            </xsl:element>
	                    <xsl:element name="rdf:rest">
	                        <xsl:attribute name="rdf:resource">
	                            <xsl:text>rdf:nil</xsl:text>
	                        </xsl:attribute>
	                    </xsl:element>
	            	</xsl:element>
	    		</xsl:when>
	    		<!-- In the middle -->
	    		<xsl:otherwise>	    			
	    			<xsl:element name="rdf:Description">
			            <xsl:element name="rdf:first">
	   				<xsl:copy-of select="ign:create_bag($bag, $rlspList)"/>
			            </xsl:element>
	                    <xsl:element name="rdf:rest">
	                        <xsl:element name="rdf:resource">
	   							<xsl:copy-of select="ign:listFromSentences($bags, $index + 1, $rlspList)"/>
	                        </xsl:element>
	                    </xsl:element>
	            	</xsl:element>
				</xsl:otherwise>
	    	</xsl:choose>
	    	<!-- Il faut vérifier s'il n'y a pas d'autres toponymes dans les phrases d'après -->	
	    	<xsl:variable name="indexFirstOtherFirstBag" as="xs:integer" select="ign:getIndexFollowingFirstBag($bags, $index)"/>
	    	<xsl:if test="$isFirst and $indexFirstOtherFirstBag > 0">
				<xsl:copy-of select="ign:listFromSentences($bags, $indexFirstOtherFirstBag, $rlspList)"/>
	    	</xsl:if>
    	</xsl:if>
    </xsl:function>
    
    <xsl:function name="ign:getIndexFollowingFirstBag" as="xs:integer">
    	<xsl:param name="bags" as="element()*"/>
    	<xsl:param name="index" as="xs:integer"/>
    	<xsl:variable name="bagsInFirstPosition" as="element()*">
    		<xsl:for-each select="$bags">
    			<xsl:if test="ign:isFirstOfTheSentence(current())">
    				<xsl:sequence select="current()"/>
    			</xsl:if>
    		</xsl:for-each>
    	</xsl:variable>
    	<xsl:variable name="indexesOfFirstsBags" as="xs:integer*">
    		<xsl:for-each select="$bagsInFirstPosition">
    			<xsl:sequence select="index-of($bags, current())"/>
    		</xsl:for-each>
    	</xsl:variable>
    	<xsl:variable name="result" as="xs:integer?">
	    	<xsl:for-each select="$indexesOfFirstsBags">
	    		<xsl:if test="current() = $index and (index-of($indexesOfFirstsBags, current())[1] + 1) > count($indexesOfFirstsBags)">
	    			<xsl:sequence select="$indexesOfFirstsBags[index-of($indexesOfFirstsBags, current())[1] + 1]"/>
	    		</xsl:if>
	    	</xsl:for-each>
    	</xsl:variable>
    	<xsl:choose>
    		<xsl:when test="$result"><xsl:sequence select="$result"/></xsl:when>
    		<xsl:otherwise><xsl:sequence select="-1"/></xsl:otherwise>
    	</xsl:choose>
    </xsl:function>
    <!-- <xsl:function name="ign:getIndexFollowingFirstBag" as="xs:integer">
    	<xsl:param name="bags" as="element()*"/>
    	<xsl:param name="index" as="xs:integer"/>
    	<xsl:choose>
    		<xsl:when test="$index > count($bags)">
    			<xsl:sequence select="-1"/>
   			</xsl:when>
    		<xsl:when test="ign:isFirstOfTheSentence($bags[$index])">
    			<xsl:sequence select="$index"/>
    		</xsl:when>
    		<xsl:otherwise>
    			<xsl:sequence select="ign:getIndexFollowingFirstBag($bags, $index + 1)"/>
    		</xsl:otherwise>
    	</xsl:choose>
    </xsl:function> -->
    
    <xsl:function as="xs:boolean" name="ign:isFirstOfTheSentence">
    	<xsl:param name="bag" as="element()"/>
    	<xsl:sequence select="ign:getPosition($bag/preceding-sibling::*[text()='.'][1]) >= 
	    		ign:getPosition($bag/preceding-sibling::bag[1])"/>
    </xsl:function>
    
    <xsl:function name="ign:getLeftBoundary" as="xs:integer">
    	<xsl:param name="orientation" as="element()?"/>
    	<xsl:choose>
	    		<xsl:when test="$orientation and $orientation[preceding-sibling::*[(@type='orientation' or @subtype='orientation') 
	    	and preceding-sibling::*[position() > 0 and not(position() > 2) 
	    		and descendant-or-self::*[@lemma='vers' or @lemma='à' or @lemma='par']]]]">
		    		<xsl:choose>
		    			<xsl:when test="ign:getPosition($orientation/preceding-sibling::*[(@type='orientation' or @subtype='orientation') 
	    	and preceding-sibling::*[position() > 0 and not(position() > 2) 
	    		and descendant-or-self::*[@lemma='vers' or @lemma='à' or @lemma='par']]][1]/following-sibling::*[text()='.'][1]) = 
	    		ign:getPosition($orientation/following-sibling::*[text()='.'][1])">
	    					<xsl:sequence select="ign:getPosition($orientation)" />
		    			</xsl:when>
		    			<xsl:otherwise>
	    					<xsl:sequence select="ign:getPosition($orientation/preceding-sibling::*[text()='.'][1])" />
		    			</xsl:otherwise>
		    		</xsl:choose>
	    		</xsl:when>
	    		<xsl:otherwise>
	    			<xsl:sequence select="0"/>
	    		</xsl:otherwise>
	    	</xsl:choose>
    </xsl:function>
                
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