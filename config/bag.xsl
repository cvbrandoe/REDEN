<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:ign="http://example.com/namespace/" 
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
	xpath-default-namespace="http://www.tei-c.org/ns/1.0">
	<xsl:output method="xml" encoding="UTF-8" indent="yes" />
	<xsl:template match="/">
		<TEI xmlns="http://www.tei-c.org/ns/1.0">
			<xsl:for-each select="TEI/*">
				<xsl:if test="@xml:id">
					<!-- it's a toponym -->
					<xsl:if
						test="preceding-sibling::*[1]
              [not(
              	(
              		(@lemma='et' or text()=',') 
              		and 
              		preceding-sibling::*[1][@xml:id]
           		)
              	or 
              	(
              		descendant-or-self::*[@lemma='de'] 
              		and 
              		preceding-sibling::*[1][(@lemma='et' or text()=',')] 
	              	and 
	              	preceding-sibling::*[2][(@xml:id and preceding-sibling::*[1][descendant-or-self::*[@lemma='de']]) or ((@subtype='orientation' or @type='orientation') 
	              		and 
	              		preceding-sibling::*[1][@xml:id])]
              	)
              	or 
              	(
              		descendant-or-self::*[@lemma='à'] 
              		and 
              		preceding-sibling::*[1][(@lemma='et' or text()=',')] 
	              	and 
	              	preceding-sibling::*[2][(@xml:id and preceding-sibling::*[1][descendant-or-self::*[@lemma='à']]) or ((@subtype='orientation' or @type='orientation') 
	              		and 
	              		preceding-sibling::*[1][@xml:id])]
              	)
              )]">
              		<!-- <xsl:text disable-output-escaping="yes"><![CDATA[<bag>]]></xsl:text> -->
						<xsl:variable name="precedingPas" select="current()/preceding-sibling::*[position() > 0 and not(position() > 6)]
							[descendant-or-self::*[@lemma='pas']][1]" as="element()?"/>
						<xsl:variable name="followingPas" select="current()/following-sibling::*[position() > 0 and not(position() > 6)]
							[descendant-or-self::*[@lemma='pas']][1]" as="element()?"/>
						<xsl:variable name="precedingDot" select="current()/preceding-sibling::*[text()='.'][1]" as="element()?"/>
						<xsl:variable name="followingDot" select="current()/following-sibling::*[text()='.'][1]" as="element()"/>
						<xsl:choose>
							<xsl:when test="($precedingPas and ign:getPosition($precedingPas) > ign:getPosition($precedingDot))
							or ($followingPas and ign:getPosition($followingDot) > ign:getPosition($followingPas))">
							<xsl:text disable-output-escaping="yes"><![CDATA[<bag landmark='true'>]]></xsl:text>
							</xsl:when>
							<xsl:otherwise><xsl:text disable-output-escaping="yes"><![CDATA[<bag>]]></xsl:text></xsl:otherwise>
						</xsl:choose>
					</xsl:if>
					<name>
						<xsl:attribute name="xml:id">
                <xsl:value-of select="@xml:id" />
              </xsl:attribute>
						<xsl:if test="@typage">
							<xsl:attribute name="typage"><xsl:value-of
								select="@typage" /></xsl:attribute>
						</xsl:if>
						<xsl:value-of select="string-join(descendant::*, ' ')" />
					</name>
					<xsl:if
						test="
            following-sibling::*[1][not(
	            	(preceding-sibling::*[2][descendant-or-self::*[@lemma='à']] and (@lemma='et' or text()=',') and (following-sibling::*[1][@xml:id]
	                or (following-sibling::*[1][descendant-or-self::*[@lemma='à']] and following-sibling::*[2][@xml:id])))
                or 
	            	(preceding-sibling::*[2][not(descendant-or-self::*[@lemma='à' or @lemma='de'])] and (@lemma='et' or text()=',') and (following-sibling::*[1][@xml:id]))
                or 
	            	(preceding-sibling::*[2][descendant-or-self::*[@lemma='de']] and (@lemma='et' or text()=',') and (following-sibling::*[1][@xml:id]
	                or (following-sibling::*[1][descendant-or-self::*[@lemma='de']] and following-sibling::*[2][@xml:id])))
                or 
	                ((@subtype='orientation' or @type='orientation') and following-sibling::*[1][(@lemma='et' or text()=',') 
	                and (following-sibling::*[1][@xml:id]
	                or (following-sibling::*[1][@lemma='à' or @lemma='de'] and following-sibling::*[2][@xml:id]))])
            )]">
						<!-- Si c'est un toponyme non suivi de toponymes on ferme le bag -->
						<xsl:text disable-output-escaping="yes"><![CDATA[</bag>]]></xsl:text>
					</xsl:if>
				</xsl:if>
				<xsl:if test="not(@xml:id)">
					<!-- it's not a toponym, we keep the node as it is -->
					<xsl:choose>
						<xsl:when
							test="(@type='orientation' or @subtype='orientation') and child::*[1][@lemma='occidental' or 
            	@lemma='oriental' or @lemma='méridional' or @lemma='septentrional']">
							<xsl:copy-of select="child::*" />
						</xsl:when>
						<xsl:otherwise>
							<xsl:copy-of select="." />
						</xsl:otherwise>
					</xsl:choose>
							<!-- <xsl:copy-of select="." /> -->
				</xsl:if>
			</xsl:for-each>
		</TEI>
	</xsl:template>
	
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