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
              and preceding-sibling::*[2][@xml:id]))]">
              <!-- Si c'est un toponyme non précédé de toponymes on ouvre le bag -->
                <xsl:choose>
                    <xsl:when test="@xml:id and preceding-sibling::w[1][(@lemma = 'la' or @lemma = 'le' or text() = 'du') 
                    and (preceding-sibling::w[1][@lemma = 'sur' or @subtype = 'motion_median' or @subtype = 'motion_final' 
                    or preceding-sibling::w[1][@lemma = 'vallée']])]">
                        <xsl:text disable-output-escaping="yes"><![CDATA[<bag type='natural_place'>]]></xsl:text>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:text disable-output-escaping="yes"><![CDATA[<bag>]]></xsl:text>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:if>
            <name>
              <xsl:attribute name="xml:id">
                <xsl:value-of select="@xml:id" />
              </xsl:attribute>
              <xsl:value-of select="string-join(descendant::*, ' ')" />
            </name>
            <xsl:if test="following-sibling::*[1][not((@lemma='et' or text()=',') and (following-sibling::*[1][@xml:id]
                or (following-sibling::*[1][@lemma='à' or @lemma='de'] and following-sibling::*[2][@xml:id])))]">
              <!-- Si c'est un toponyme non suivi de toponymes on ferme le bag --> 
              <xsl:text disable-output-escaping="yes"><![CDATA[</bag>]]></xsl:text>
            </xsl:if>
          </xsl:if>
          <xsl:if test="not(@xml:id)">
            <!-- Ce n'est pas un toponyme, on garde le noeud tel quel -->
            <xsl:copy-of select="."/>
          </xsl:if>        
        </xsl:for-each>
      </TEI>
    </xsl:template>
    
</xsl:stylesheet>