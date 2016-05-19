<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xpath-default-namespace="http://www.tei-c.org/ns/1.0" >
    <xsl:output method="xml" encoding="UTF-8" indent="yes" />
	  <!-- NOTES 
      Pour l'instant j'utilise les balises "<result><toponym/><orientation/><toponym/></results>" pour afficher les résultats.
      De plus, je me base sur le fichier peurSudOuestMinimized.xml qui n'est pas poluée par les balises "phr", "s" and co.
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
    
    <xsl:template match="bag">
        <xsl:choose>
            <!-- T1 # PREPDET|(PREP DET) O de T2 => T1 au O de T2 -->
            <xsl:when test="following-sibling::*[position() >= 1 and not(position() > 4)][
            (@type='PREPDET' and following-sibling::*[1][@type='orientation' or @subtype='orientation'] and 
            following-sibling::*[position() >= 2 and not(position() > 6)][name()='bag' and 
            (preceding-sibling::*[1][@lemma='de'] or preceding-sibling::*[1][@type='DET' and preceding-sibling::*[1][@type='PREP'
            or child::*[@type='PREP']]])]) or 
            (@type='PREP' and following-sibling::*[1][@type='DET']
            and following-sibling::*[2][@type='orientation' or @subtype='orientation'] and 
            following-sibling::*[position() >= 3 and not(position() > 6)][name()='bag' and 
            (preceding-sibling::*[1][@lemma='de'] or preceding-sibling::*[1][@type='DET' and preceding-sibling::*[1][@type='PREP'
            or child::*[@type='PREP']]])])]">
                <xsl:element name="bag">
                    <xsl:attribute name="position">start</xsl:attribute>
                    <xsl:attribute name="node"><xsl:value-of select="following-sibling::bag[1]/descendant::*[@xml:id][1]/@xml:id" /></xsl:attribute>
                    <xsl:attribute name="orientation"><xsl:value-of select="following-sibling::*[@type='orientation' or @subtype='orientation'][1]/descendant::*[
                    lower-case(text())='nord' or lower-case(text())='sud' or lower-case(text())='est' or lower-case(text())='ouest'][1]" /></xsl:attribute>                    
                    <xsl:apply-templates select="@*|node()"/>
                </xsl:element>
            </xsl:when>
            <!-- # T1 # motion_final de T2 # PREPDET|(PREP DET) O => T2 au O de T1 -->
            <xsl:when test="following-sibling::*[position() >= 2 and not(position() > 4)][
            @subtype='motion_final' and following-sibling::*[1][@lemma='de'] and 
            following-sibling::*[position() >= 2 and not(position() > 3)][name()='bag'] and 
            following-sibling::*[position() >= 3 and not(position() > 6)][@type='orientation' or @subtype='orientation']]">
                <xsl:element name="bag">
                    <xsl:attribute name="position">end</xsl:attribute>
                    <xsl:attribute name="node"><xsl:value-of select="following-sibling::bag[1]/descendant::*[@xml:id][1]/@xml:id" /></xsl:attribute>
                    <xsl:attribute name="orientation"><xsl:value-of select="following-sibling::*[@type='orientation' or @subtype='orientation'][1]/descendant::*[
                    lower-case(text())='nord' or lower-case(text())='sud' or lower-case(text())='est' or lower-case(text())='ouest'][1]" /></xsl:attribute>                    
                    <xsl:apply-templates select="@*|node()"/>
                </xsl:element>
            </xsl:when>
            <!-- de T1 # vers DET O # motion_final # T2  => T2 au O de T1 -->
            <xsl:when test="following-sibling::*[position() >= 1 and not(position() > 8)][
                @lemma='vers' and following-sibling::*[1][(@type='orientation' or @subtype='orientation') and 
                following-sibling::*[position() >= 2 and not(position() > 4)][@subtype='motion_final' and
                    following-sibling::*[position() >= 1 and not(position() > 6)][name()='bag']]]]">
                <xsl:element name="bag">
                    <xsl:attribute name="position">end</xsl:attribute>
                    <xsl:attribute name="node"><xsl:value-of select="following-sibling::bag[1]/descendant::*[@xml:id][1]/@xml:id" /></xsl:attribute>
                    <xsl:attribute name="orientation"><xsl:value-of select="following-sibling::*[@type='orientation' or @subtype='orientation'][1]/descendant::*[
                    lower-case(text())='nord' or lower-case(text())='sud' or lower-case(text())='est' or lower-case(text())='ouest'][1]" /></xsl:attribute>                    
                    <xsl:apply-templates select="@*|node()"/>
                </xsl:element>
            </xsl:when>
            <!-- vers DET O # motion_initial de T1 # T2  => T2 au O de T1 -->
            <xsl:when test="preceding-sibling::*[1][@lemma='de' and preceding-sibling::*[1][@subtype='motion_initial'] and
                preceding-sibling::*[position() >= 2 and not(position() > 6)][(@type='orientation' or @subtype='orientation') and 
                    preceding-sibling::*[1][@lemma='vers']] and
                    following-sibling::*[position() >= 2 and not(position() > 10)][name()='bag']]">
                <xsl:element name="bag">
                    <xsl:attribute name="position">end</xsl:attribute>
                    <xsl:attribute name="node"><xsl:value-of select="following-sibling::bag[1]/descendant::*[@xml:id][1]/@xml:id" /></xsl:attribute>
                    <xsl:attribute name="orientation"><xsl:value-of select="following-sibling::*[@type='orientation' or @subtype='orientation'][1]/descendant::*[
                    lower-case(text())='nord' or lower-case(text())='sud' or lower-case(text())='est' or lower-case(text())='ouest'][1]" /></xsl:attribute>                    
                    <xsl:apply-templates select="@*|node()"/>
                </xsl:element>
            </xsl:when>  
            <!-- PREPDET|(PREP DET) O PREP T1 # PREP T2 => T2 au O de T1 -->
            <xsl:when test="(preceding-sibling::*[1][@type='PREP' and preceding-sibling::*[1][(@type='orientation' or @subtype='orientation')]]
                or preceding-sibling::*[1][@type='DET' and preceding-sibling::*[1][@type='PREP'] and 
                preceding-sibling::*[2][(@type='orientation' or @subtype='orientation')]])
                and following-sibling::*[position() >= 3 and not(position() > 12)][name()='bag']">
                <xsl:element name="bag">
                    <xsl:attribute name="position">end</xsl:attribute>
                    <xsl:attribute name="node"><xsl:value-of select="following-sibling::bag[position() >= 3 and not(position() > 12)]/descendant::*[@xml:id][1]/@xml:id" /></xsl:attribute>
                    <xsl:attribute name="orientation"><xsl:value-of select="following-sibling::*[@type='orientation' or @subtype='orientation'][1]/descendant::*[
                    lower-case(text())='nord' or lower-case(text())='sud' or lower-case(text())='est' or lower-case(text())='ouest'][1]" /></xsl:attribute>                    
                    <xsl:apply-templates select="@*|node()"/>
                </xsl:element>
            </xsl:when>
            <!-- T1 # PREPDET|(PREP DET) O à|vers T2 => T2 au O de T1 -->
            <xsl:when test="following-sibling::*[position() >= 1 and not(position() > 4)][
            (
                @type='PREPDET' 
                and 
                following-sibling::*[1][@type='orientation' or @subtype='orientation'] 
                and 
                following-sibling::*[position() >= 2 and not(position() > 8)]
                [
                name()='bag' 
                and 
                (
                    preceding-sibling::*[1][@lemma='à' or @lemma='vers'] 
                    or 
                    preceding-sibling::*[1][@type='DET' and preceding-sibling::*[1][@type='PREP']]
                )
                ]
            ) 
            or 
            (
                @type='PREP' 
                and 
                following-sibling::*[1][@type='DET']
                and 
                following-sibling::*[2][@type='orientation' or @subtype='orientation'] 
                and 
                following-sibling::*[position() >= 3 and not(position() > 8)]
                [
                name()='bag' 
                and 
                (
                    preceding-sibling::*[1][@lemma='à' or @lemma='vers'] 
                    or 
                    preceding-sibling::*[1][@type='DET' and preceding-sibling::*[1][@type='PREP']]
                )
                ]
            )
            ]">
                <xsl:element name="bag">
                    <xsl:attribute name="position">start</xsl:attribute>
                    <xsl:attribute name="node"><xsl:value-of select="following-sibling::bag[1]/descendant::*[@xml:id][1]/@xml:id" /></xsl:attribute>
                    <xsl:attribute name="orientation"><xsl:value-of select="following-sibling::*[@type='orientation' or @subtype='orientation'][1]/descendant::*[
                    lower-case(text())='nord' or lower-case(text())='sud' or lower-case(text())='est' or lower-case(text())='ouest'][1]" /></xsl:attribute>                    
                    <xsl:apply-templates select="@*|node()"/>
                </xsl:element>
            </xsl:when>
            <xsl:otherwise>
                <xsl:copy>
                    <xsl:apply-templates select="@*|node()"/>
                </xsl:copy>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

</xsl:stylesheet>