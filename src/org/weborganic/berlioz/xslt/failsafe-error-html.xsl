<?xml version="1.0" encoding="UTF-8"?>
<!--
  Fail-safe stylesheet to display transform errors

  @author Christophe Lauret
  @version 1 July 2011
-->
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<!--
  General Output properties.
  (Ensure that the doctype does not triggers Quirks Mode)
-->
<xsl:output method="html" encoding="utf-8" indent="yes" undeclare-prefixes="no" media-type="text/html" />

<!-- The ID of the error -->
<xsl:variable name="id" select="/*/@id"/>

<!-- Main template called in all cases. -->
<xsl:template match="/">
<!-- Display the HTML Doctype -->
<xsl:text disable-output-escaping="yes"><![CDATA[<!doctype html>
]]></xsl:text>
<html>
<head>
  <title><xsl:value-of select="*/title"/></title>
  <style type="text/css">
body {font-family: Frutiger, "Frutiger Linotype", Univers, Calibri, "Gill Sans", "Gill Sans MT", "Myriad Pro", Myriad, "Liberation Sans",  Tahoma, Geneva, "Helvetica Neue", Helvetica, Arial, sans-serif; background: #f7f7f7; border-top: 80px solid #13476a; padding: 0; margin: 0}

h1   {margin-top: 0; font-weight: 100; color: white}

.informational {border-color: #666;}
.server-error  {border-color: #c01;}
.client-error  {border-color: #e60;}
.redirection   {border-color: #146;}
.successful    {border-color: #292;}

h2   {border-bottom: 2px solid #09f; color: #09f; font-size: 3ex}
h3   {border-bottom: 1px solid #06a; color: #06a; font-size: 2.5ex}
h4   {font-size: 2ex}

code {font-family: Consolas, "Lucida Console", "Lucida Sans Typewriter", "Courier New", monospace; font-size: 80%; line-height: 150%}
pre  {font-family: Consolas, "Lucida Console", "Lucida Sans Typewriter", "Courier New", monospace; font-size: 70%; line-height: 150%; color: #666; }

.container       {width: 980px; margin: -56px auto; background: rgba(255,255,255,0.3); padding: 5px 10px; box-shadow: 0 0 15px 3px rgba(0,0,0,.2); border: 5px solid rgba(0,0,0,.1);}
.message         {font-weight: bold}
.footer          {border-top: 2px solid #999; height: 20px; color: #666; font-size: 80%;}
.location        {font-family: Consolas, "Lucida Console", "Lucida Sans Typewriter", "Courier New", monospace; font-size: 80%; line-height: 150%}
#datetime        {float: left;}
#berlioz-version {float: right;}

li     {list-style-type: none; display: block; clear: both; font-size: 12px; font-family: Consolas, "Lucida Console", "Lucida Sans Typewriter", "Courier New", monospace; font-size: 80%; margin-bottom: 2px}
.line  {float:left; margin-right: 4px; color: #999;width: 60px}
.col   {float:left; margin-right: 4px; color: #999;width: 80px}
.level {float:left; margin-right: 4px; color: #999;width: 70px; text-align: center; font-weight: bold; border-radius: 5px; padding: 2px}
.warning > .level  {color: orange;}
.error   > .level  {color: red;}
.fatal   > .level  {color: white; background: #C01;}

.help {border: 1px solid #ffe; background: #ffe; border-radius: 3px; box-shadow: 0 0 4px #ec9; font-style: italic;}
.help p {margin: 4px}
  </style>
</head>
<body class="{name(*)}">
  <xsl:apply-templates select="*"/>
</body>
</html>
</xsl:template>

<!-- Default template for errors -->
<xsl:template match="continue|successful|redirection|client-error|server-error">
  <div class="container">
    <h1><xsl:value-of select="@http-code"/> - <xsl:value-of select="title"/></h1>
    <xsl:if test="not(message = exception/message)">
      <p class="message"><xsl:value-of select="message"/></p>
    </xsl:if>
    <xsl:apply-templates select="." mode="help"/>
    <xsl:apply-templates select="exception|error"/>
    <xsl:apply-templates select="collected-errors"/>
    <div class="footer">
      <div id="datetime"><xsl:value-of select="format-dateTime(@datetime, '[MNn] [D], [Y] at [H01]:[m01]:[s01] [z]')"/></div>
      <div id="berlioz-version">Berlioz <xsl:value-of select="berlioz/@version"/></div>
    </div>
    <div hidden="hidden" style="display:none">
      <xsl:copy-of select="."/>
    </div>
  </div>
</xsl:template>

<!-- Other errors -->
<xsl:template match="error[@http-code=404]">
  <div class="container client-error">
    <h1><xsl:value-of select="message"/></h1>
    <p class="message">Sorry but I could not find anything at <code><xsl:value-of select="@request-uri"/></code></p>
    <xsl:copy-of select="."/>
  </div>
</xsl:template>

<!-- Common templates ======================================================================== -->

<!-- Exception -->
<xsl:template match="exception">
  <div class="exception">
    <h2><xsl:value-of select="message"/></h2>
    <xsl:if test="not(following-sibling::collected-errors)">
      <xsl:apply-templates select="location"/>
    </xsl:if>
    <xsl:apply-templates select="stack-trace"/>
    <xsl:apply-templates select="cause[not(message = current()/message)]"/>
  </div>
</xsl:template>

<!-- Java Error -->
<xsl:template match="error[@class]">
  <div class="error">
    <h2><xsl:value-of select="message"/></h2>
    <xsl:if test="not(following-sibling::collected-errors)">
      <xsl:apply-templates select="location"/>
    </xsl:if>
    <xsl:apply-templates select="stack-trace"/>
    <xsl:apply-templates select="cause[not(message = current()/message)]"/>
  </div>
</xsl:template>

<!-- Cause of an exception -->
<xsl:template match="cause">
  <div class="cause">
    <h4><i>Caused by: </i> <xsl:value-of select="message"/></h4>
    <xsl:if test="not(parent::exception/following-sibling::collected-errors)">
      <xsl:apply-templates select="location"/>
    </xsl:if>
    <xsl:apply-templates select="stack-trace|cause"/>
  </div>
</xsl:template>

<!-- Stack Trace -->
<xsl:template match="stack-trace">
  <pre class="stacktrace">
  <!-- No need to display the stack trace if we know the error -->
  <xsl:if test="not($id = 'berlioz-unexpected' or starts-with($id, 'berlioz-generator'))">
    <xsl:attribute name="hidden">hidden</xsl:attribute>
    <xsl:attribute name="style">display:none</xsl:attribute>
  </xsl:if>
  <xsl:value-of select="text()"/></pre>
</xsl:template>

<!-- Location -->
<xsl:template match="location">
  <p class="location">File: <xsl:value-of select="@system-id"/>, Line: <xsl:value-of select="@line"/>, Column: <xsl:value-of select="@column"/></p>
</xsl:template>

<!-- Collected errors -->
<xsl:template match="collected-errors">
<xsl:for-each-group select="collected" group-by="location/@system-id">
  <h4><xsl:value-of select="location/@system-id"/></h4>
  <ul class="collected">
    <xsl:for-each select="current-group()">
      <li class="{@level}">
        <span class="level">[<xsl:value-of select="@level"/>]</span>
        <span class="line">Line: <xsl:value-of select="location/@line"/></span>
        <span class="col">Column: <xsl:value-of select="location/@column"/></span>
        <span class="info">
          <xsl:value-of select="message"/>
          <xsl:if test="cause and not(message = cause/message)">: <xsl:value-of select="cause/message"/></xsl:if>
        </span>
      </li>
    </xsl:for-each>
  </ul>
</xsl:for-each-group>
<!-- If there are error without a location -->
<xsl:apply-templates select="collected[not(location)]"/>
</xsl:template>

<xsl:template match="collected[not(location)]">
  <div class="exception">
    <h3><xsl:value-of select="message"/></h3>
    <xsl:apply-templates select="stack-trace" />
    <xsl:apply-templates select="cause[not(message = current()/message)]"/>
  </div>
</xsl:template>

<!-- Help for Specifid Error IDs ============================================================== -->

<!-- No help: ignore -->
<xsl:template match="*" mode="help" />

<!-- Help: Services configuration could not be found  -->
<xsl:template match="server-error[@id='berlioz-services-not-found']" mode="help">
<div class="help">
  <p>Berlioz was unable to find the <b>service configuration</b>.</p>
  <p>To fix this problem, creates a file called '<b>services.xml</b>' and put it in your <code>/WEB-INF/config/</code> folder.</p>
</div>
</xsl:template>

<!-- Help: Services configuration is not well formed  -->
<xsl:template match="server-error[@id='berlioz-services-malformed']" mode="help">
<div class="help">
  <p>Berlioz was unable to parse the <b>service configuration</b>.</p>
  <p>To fix this problem, you need to fix the XML errors in the '<b>/WEB-INF<xsl:value-of select="(//location)[1]/@system-id"/></b>' file.</p>
</div>
</xsl:template>

<!-- Help: Services configuration is invalid  -->
<xsl:template match="server-error[@id='berlioz-services-invalid']" mode="help">
<div class="help">
  <p>Berlioz was unable to load the service configuration because of the errors listed below.</p>
  <p>To fix this problem, you need to modify the '<b>/WEB-INF<xsl:value-of select="(//location)[1]/@system-id"/></b>' file.</p>
</div>
</xsl:template>

<!-- Help: Transform file could not be found -->
<xsl:template match="server-error[@id='berlioz-transform-not-found']" mode="help">
<div class="help">
  <p>Berlioz was unable to find the <b>XSLT style sheet</b>.</p>
  <p>To fix this problem, simply create the style file describe below in your <code>/WEB-INF/</code> folder.</p>
</div>
</xsl:template>

<!-- Help: Transform file could not be found -->
<xsl:template match="server-error[@id='berlioz-transform-malformed-source-xml']" mode="help">
<div class="help">
  <p>Berlioz could not transform the <b>source XML</b> because it is not well-formed.</p>
  <p>To fix this problem, simply ensure that the XML returned by your generator is well formed.</p>
</div>
</xsl:template>

</xsl:stylesheet>
