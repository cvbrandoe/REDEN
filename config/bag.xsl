<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xpath-default-namespace="http://www.tei-c.org/ns/1.0" >
    <xsl:output method="xml" encoding="UTF-8" indent="yes" />
    <xsl:template match="/">
      <TEI xmlns="http://www.tei-c.org/ns/1.0">
        <xsl:for-each select="TEI/*">        
          <xsl:if test="@xml:id">
            <!-- C'est un toponyme -->
            <xsl:if test="preceding-sibling::*[1]
              [not(((@lemma='et' or text()=',') and preceding-sibling::*[1][@xml:id])
              or ((@lemma='à' or @lemma='de') and preceding-sibling::*[1][(@lemma='et' or text()=',')] 
              and preceding-sibling::*[2][@xml:id or ((@subtype='orientation' or @type='orientation') and 
              preceding-sibling::*[1][@xml:id])]))]">
				<xsl:text disable-output-escaping="yes"><![CDATA[<bag>]]></xsl:text>
            </xsl:if>
            <name>
              <xsl:attribute name="xml:id">
                <xsl:value-of select="@xml:id" />
              </xsl:attribute>
              <xsl:if test="@typage"><xsl:attribute name="typage"><xsl:value-of select="@typage"/></xsl:attribute></xsl:if>
              <xsl:value-of select="string-join(descendant::*, ' ')" />
            </name>
            <xsl:if test="following-sibling::*[1][not(((@lemma='et' or text()=',') and (following-sibling::*[1][@xml:id]
                or (following-sibling::*[1][@lemma='à' or @lemma='de'] and following-sibling::*[2][@xml:id])))
                or ((@subtype='orientation' or @type='orientation') and following-sibling::*[1][(@lemma='et' or text()=',') 
                and (following-sibling::*[1][@xml:id]
                or (following-sibling::*[1][@lemma='à' or @lemma='de'] and following-sibling::*[2][@xml:id]))]))]">
              <!-- Si c'est un toponyme non suivi de toponymes on ferme le bag --> 
              <xsl:text disable-output-escaping="yes"><![CDATA[</bag>]]></xsl:text>
            </xsl:if>
          </xsl:if>
          <xsl:if test="not(@xml:id)">
            <!-- Ce n'est pas un toponyme, on garde le noeud tel quel -->
            <xsl:choose>
            	<xsl:when test="(@type='orientation' or @subtype='orientation') and child::*[1][@lemma='occidental' or 
            	@lemma='oriental' or @lemma='méridional' or @lemma='septentrional']">
            		<xsl:copy-of select="child::*"/>
            	</xsl:when>
            	<xsl:otherwise>
	            	<xsl:copy-of select="."/>
	            </xsl:otherwise>
            </xsl:choose>
          </xsl:if>        
        </xsl:for-each>
      </TEI>
    </xsl:template>
    
</xsl:stylesheet>