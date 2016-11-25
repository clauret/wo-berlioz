<?xml version="1.0" encoding="UTF-8"?>
<!--
  Fallback template invoked by Berlioz for JSON output returning an
  empty JSON object.

  The XML generated by this template should map to a simple JSON tree model
  following the Aeson format.

  @see https://pageseeder.org/aeson.html
-->
<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:json="http://weborganic.org/JSON"
                exclude-result-prefixes="#all">

<!-- JSON output properties -->
<xsl:output method="xml" media-type="application/json" encoding="utf-8"/>

<!-- Default -->
<xsl:template match="/">
<json:object/>
</xsl:template>

</xsl:stylesheet>
