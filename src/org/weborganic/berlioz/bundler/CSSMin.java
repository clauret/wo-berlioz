/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
/*
 * CSSMin Copyright License Agreement (BSD License)
 *
 * Copyright (c) 2011, Barry van Oudtshoorn
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * * Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 *
 * * Neither the name of Barryvan nor the names of its
 *   contributors may be used to endorse or promote products
 *   derived from this software without specific prior
 *   written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.weborganic.berlioz.bundler;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CSSMin takes in well-formed, human-readable CSS and reduces its size substantially.
 *
 * <p>It removes unnecessary whitespace and comments, and orders the contents of CSS
 * selectors alphabetically to enhance GZIP compression.
 *
 * <p>Originally by Barry van Oudtshoorn and released under BSD licence, with bug
 * reports, fixes, and contributions by
 * <ul>
 *   <li>Kevin de Groote</li>
 *   <li>Pedro Pinheiro</li>
 *   <li>Asier Lostal</li>
 * </ul>
 * Portions of the code is based on the YUI CssCompressor code, by Julien Lecomte.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.9.32
 * @since Berlioz 0.9.32
 */
public final class CSSMin {

  /**
   * Logger to know what is going on in the class.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(CSSMin.class);

  /** Utility class. */
  private CSSMin() {
  }

  /**
   * Process a file from a filename.
   *
   * @param filename The file name of the CSS file to process.
   * @param out Where to send the result
   */
  public static void minimize(String filename, OutputStream out) {
    try {
      minimize(new InputStreamReader(new FileInputStream(filename), Charset.forName("utf8")), out);
    } catch (FileNotFoundException ex) {
      LOGGER.debug("Unable to find file", ex);
    }
  }

  /**
   * Process input from a reader.
   *
   * @param input Where to read the CSS from
   * @param out   Where to send the result
   */
  public static void minimize(Reader input, OutputStream out) {
    minimize(input, new PrintWriter(new OutputStreamWriter(out, Charset.forName("utf8"))));
  }

  /**
   * Minify CSS from a reader to a printstream.
   *
   * @param input Where to read the CSS from
   * @param min   Where to write the result to
   */
  public static void minimize(Reader input, PrintWriter min) {
    try {
      StringBuilder buffer = toBuffer(input);
      String comment = stripComments(buffer);
      LOGGER.debug("Parsing and processing selectors.");

      // Reset for selector
      List<Rule> rules = new ArrayList<Rule>();
      int line = 0;
      int n = 0; // Current position in stream
      int j = 0; // Number of open braces
      char c;    // Character being read
      for (int i = 0; i < buffer.length(); i++) {
        c = buffer.charAt(i);
        if (j < 0) throw new ParsingException("Unbalanced braces!", -1, -1);
        if (c == '{') {
          j++;
        } else if (c == '}') {
          j--;
          if (j == 0) {
            try {
              rules.add(new Rule(buffer.substring(n, i + 1)));
            } catch (ParsingException ex) {
              LOGGER.warn(ex.getMessage()+" L:"+line);
            }
            n = i + 1;
          }
        } else if (c == '\n') {
          line++;
        }
      }

      // Let's write it out
      int countRules = 0;
      min.println(comment);
      for (Rule rule : rules) {
        if (countRules % 10 == 0 || rule.subrules != null) {
          min.println();
        }
        min.print(rule.toString());
        countRules++;
      }
      min.println();
      min.close();

      LOGGER.debug("Process completed successfully.");

    } catch (Exception ex) {
      ex.printStackTrace(System.err);
      LOGGER.error(ex.getMessage());
    }

  }

  /**
   * Loads the content of the styles as CSS trimming empty lines (to preserve line count)
   *
   * @param input the CSS code to read.
   * @return the buffer.
   *
   * @throws IOException Should an error occur while reading the file.
   */
   private static StringBuilder toBuffer(Reader input) throws IOException {
     BufferedReader br = new BufferedReader(input);
     StringBuilder buffer = new StringBuilder();
     String s;
     while ((s = br.readLine()) != null) {
       if (s.trim().length() > 0) {
         buffer.append(s);
       }
       buffer.append('\n');
     }
     return buffer;
   }

   /**
    * Strips comments from the buffer.
    *
    * @param buffer the buffer to strip the comments from.
    * @return the updated buffer.
    *
    * @throws ParsingException Should an error occur while reading the file.
    */
  private static String stripComments(StringBuilder buffer) throws ParsingException {
    int n = 0;
    int k = 0;
    boolean keep = false;
    StringBuilder comments = new StringBuilder();
    // Find the start of the comment
    while ((n = buffer.indexOf("/*", n)) != -1) {
      if (buffer.charAt(n + 2) == '*') { // Retain special comments
        keep = true;
      }
      k = buffer.indexOf("*/", n + 2);
      if (k == -1) throw new ParsingException("Unterminated comment. Aborting.", -1, -1);
      int s = 0;
      for (int i = n; i < k; i++) {
        if (buffer.charAt(i) == '\n') {
          s++;
        }
      }
      if (keep) {
        comments.append(buffer.substring(n, k+2));
      }
      buffer.delete(n, k + 2);
      for (int i = 0; i < s; i++) {
        buffer.insert(n, '\n');
      }
      keep = false;
    }
    return cleanComment(comments.toString());
  }

  /**
   * Clean the comment by removing unnecessary white space.
   *
   * @param comment the comment string to clean.
   *
   * @return the clean comment.
   */
  private static String cleanComment(String comment) {
    StringBuilder clean = new StringBuilder(comment);
    int n = 0;
    while ((n = clean.indexOf("\n * ", n)) != -1) {
      clean.delete(n, n+3);
    }
    while ((n = clean.indexOf("\n", n)) != -1) {
      clean.delete(n, n+1);
    }
    return clean.toString();
  }

  /**
   * A CSS rule.
   *
   * For example, "div { border: solid 1px red; color: blue; }"
   *
   * @author Christophe Lauret
   * @version 18 June 2012
   */
  private static class Rule {

    /** The selector */
    private String _selector;

    /** Properties inside the selector. */
    private Property[] _properties = null;

    /** Properties inside the selector. */
    private List<Rule> subrules = null;

    /**
     * Creates a new Selector using the supplied strings.
     *
     * @param rule The entire rule starting with the selector
     *
     * @throws ParsingException If the selector is incomplete and cannot be parsed.
     */
    public Rule(String rule) throws ParsingException {
      String[] parts = rule.split("\\{");
      if (parts.length < 2) // TODO detect line and column
      throw new ParsingException("Warning: Incomplete selector: " + rule, -1, -1);

      // Always starts with the selector
      this._selector = parts[0].toString().trim();

      // Simplify combinators
      this._selector = this._selector.replaceAll("\\s?(\\+|~|,|=|~=|\\^=|\\$=|\\*=|\\|=|>)\\s?", "$1");

      // We're dealing with a nested property, eg @-webkit-keyframes or @media
      if (parts.length > 2) {
        this.subrules = new ArrayList<Rule>();
        parts = rule.split("\\{|\\}");
        for (int i = 1; i < parts.length; i += 2) {
          // sub selector
          parts[i] = parts[i].trim();
          if (parts[i].length() > 0 && (i+1) < parts.length) {
            // properties of sub selector
            parts[i + 1] = parts[i + 1].trim();
            if (parts[i + 1].length() > 0) {
              this.subrules.add(new Rule(parts[i] + "{" + parts[i + 1] + "}"));
            }
          }
        }
      } else {
        String contents = parts[parts.length - 1].trim();
        if (contents.charAt(contents.length() - 1) != '}') throw new ParsingException("Unterminated selector: " +rule, -1, -1);
        // No need to include empty selectors
        if (contents.length() > 1) {
          contents = contents.substring(0, contents.length() - 1);
          this._properties = parseProperties(contents);
          // We do not sort the properties as it may affect result
//          Arrays.sort(this._properties);
        }
      }
    }

    /**
     * Prints out this selector and its contents nicely, with the contents sorted alphabetically.
     *
     * @return A string representing this selector, minified.
     */
    @Override
    public String toString() {
      return append(new StringBuilder()).toString();
    }

    /**
     * Prints out this selector and its contents nicely, with the contents sorted alphabetically.
     *
     * @param min The
     * @return the string bufferer
     */
    public StringBuilder append(StringBuilder min) {
      min.append(this._selector).append('{');
      if (this.subrules != null) {
        for (Rule s : this.subrules) {
          min.append(s.toString());
        }
      }
      if (this._properties != null) {
        for (Property p : this._properties) {
          p.append(min);
        }
      }
      if (min.charAt(min.length() - 1) == ';') {
        min.deleteCharAt(min.length() - 1);
      }
      min.append('}');
      return min;
    }

    /**
     * Parses out the properties of a selector's body.
     *
     * @param contents The body; for example, "border: solid 1px red; color: blue;"
     * @return An array of properties parsed from this selector.
     */
    private Property[] parseProperties(String contents) {
      List<String> parts = new ArrayList<String>();
      boolean inquotes = false;
      boolean inbrackets = false;
      int j = 0;
      String substr;
      for (int i = 0; i < contents.length(); i++) {
        if (inquotes) { // If we're inside a string
          inquotes = contents.charAt(i) != '"';
        } else if (inbrackets) {
          inbrackets = contents.charAt(i) != ')';
        } else if (contents.charAt(i) == '"') {
          inquotes = true;
        } else if (contents.charAt(i) == '(') {
          inbrackets = true;
        } else if (contents.charAt(i) == ';') {
          substr = contents.substring(j, i);
          if (!("".equals(substr.trim()) || (substr == null))) {
            parts.add(substr);
          }
          j = i + 1;
        }
      }
      substr = contents.substring(j, contents.length());
      if (!("".equals(substr.trim()) || (substr == null))) {
        parts.add(substr);
      }
      Property[] results = new Property[parts.size()];

      for (int i = 0; i < parts.size(); i++) {
        try {
          results[i] = new Property(parts.get(i));
        } catch (Exception e) {
          System.out.println(e.getMessage());
          results[i] = null;
        }
      }

      return results;
    }

  }

  /**
   * A CSS property.
   *
   * <p>For example:
   * <ul>
   *   <li><code>"border: solid 1px red;"</code></li>
   *   <li><code>"-moz-box-shadow: 3px 3px 3px rgba(255, 255, 0, 0.5);"</code></li>
   * </ul>
   */
  private static class Property implements Comparable<Property> {

    /**
     * Name of the property
     */
    private String _property;

    /**
     * The various parts of the property.
     */
    private Part[] _parts;

    /**
     * Creates a new Property using the supplied strings.
     *
     * Parses out the values of the property selector.
     *
     * @param property The property;
     * @throws ParsingException If the property is incomplete and cannot be parsed.
     */
    public Property(String property) throws ParsingException {
      // Parse the property.
      List<String> parts = new ArrayList<String>();
      boolean inquotes = false;   // If we're inside a string
      boolean inbrackets = false; // If we're inside brackets
      int j = 0;
      String substr;
      for (int i = 0; i < property.length(); i++) {
        if (inquotes) {
          inquotes = (property.charAt(i) != '"');
        } else if (inbrackets) {
          inbrackets = (property.charAt(i) != ')');
        } else if (property.charAt(i) == '"') {
          inquotes = true;
        } else if (property.charAt(i) == '(') {
          inbrackets = true;
        } else if (property.charAt(i) == ':') {
          substr = property.substring(j, i);
          if (!("".equals(substr.trim()) || (substr == null))) {
            parts.add(substr);
          }
          j = i + 1;
        }
      }
      substr = property.substring(j, property.length());
      if (!("".equals(substr.trim()) || (substr == null))) {
        parts.add(substr);
      }
      if (parts.size() < 2) throw new ParsingException("Warning: Incomplete property: "+property, -1, -1);
      try {
        this._property = parts.get(0).trim().toLowerCase();
        this._parts = parseValues(simplifyColours(parts.get(1).trim().replaceAll(", ", ",")));
      } catch (PatternSyntaxException ex) {
        // Invalid regular expression used.
      }
    }

    /**
     * Prints out this property nicely.
     *
     * @return A string representing this property, minified.
     */
    @Override
    public String toString() {
      return append(new StringBuilder()).toString();
    }

    /**
     * Prints out this property nicely.
     * @param min the minified string to append to.
     * @return A string representing this property, minified.
     */
    public StringBuilder append(StringBuilder min) {
      min.append(this._property).append(":");
      for (Part p : this._parts) {
        min.append(p.toString()).append(",");
      }
      min.deleteCharAt(min.length() - 1); // Delete the trailing comma.
      min.append(";");
      return min;
    }

    /**
     * Compare this property with another.
     *
     * <p>We can't just use <code>String.compareTo()</code>, because we need to sort properties that have hack
     * prefixes last -- eg, *display should come after display.
     *
     * {@inheritDoc}
     */
    @Override
    public int compareTo(Property other) {
      String thisProp = this._property;
      String thatProp = other._property;

      if (thisProp.charAt(0) == '-') {
        thisProp = thisProp.substring(1);
        thisProp = thisProp.substring(thisProp.indexOf('-') + 1);
      } else if (thisProp.charAt(0) < 'a') {
        thisProp = thisProp.substring(1);
      }

      if (thatProp.charAt(0) == '-') {
        thatProp = thatProp.substring(1);
        thatProp = thatProp.substring(thatProp.indexOf('-') + 1);
      } else if (thatProp.charAt(0) < 'a') {
        thatProp = thatProp.substring(1);
      }

      return thisProp.compareTo(thatProp);
    }

    /**
     * Parse the values out of a property.
     *
     * @param contents The property to parse
     * @return An array of Parts
     */
    private Part[] parseValues(String contents) {
      // Make sure we do not split data URIs
      String[] parts = contents.split("(?<!base64),");
      Part[] results = new Part[parts.length];
      for (int i = 0; i < parts.length; i++) {
        try {
          results[i] = new Part(parts[i], this._property);
        } catch (Exception ex) {
          LOGGER.warn(ex.getMessage());
          results[i] = null;
        }
      }
      return results;
    }

    private String simplifyColours(String contents) {
      // This replacement, although it results in a smaller uncompressed file,
      // actually makes the gzipped file bigger -- people tend to use rgba(0,0,0,0.x)
      // quite a lot, which means that rgba(0,0,0,0) has its first eight or so characters
      // compressed really efficiently; much more so than "transparent".
      //contents = contents.replaceAll("rgba\\(0,0,0,0\\)", "transparent");

      return simplifyRGBColours(contents);
    }

    /**
     * Convert rgb(51,102,153) to #336699 (this code largely based on YUI code)
     *
     * @param contents The color to replace
     * @return the simplified color.
     */
    private String simplifyRGBColours(String contents) {
      StringBuffer newContents = new StringBuffer();
      StringBuffer hexColour;
      String[] rgbColours;
      int colourValue;

      Pattern pattern = Pattern.compile("rgb\\s*\\(\\s*([0-9,\\s]+)\\s*\\)");
      Matcher matcher = pattern.matcher(contents);

      while (matcher.find()) {
        hexColour = new StringBuffer("#");
        rgbColours = matcher.group(1).split(",");
        for (String rgbColour : rgbColours) {
          colourValue = Integer.parseInt(rgbColour);
          if (colourValue < 16) {
            hexColour.append("0");
          }
          hexColour.append(Integer.toHexString(colourValue));
        }
        matcher.appendReplacement(newContents, hexColour.toString());
      }
      matcher.appendTail(newContents);

      return newContents.toString();
    }
  }

  /**
   * A property part.
   */
  private static class Part {

    /**
     * The property this part belongs to.
     */
    private final String _property;

    /**
     * The contents of the property.
     */
    private String _contents;

    /**
     * Create a new property part by parsing the given string.
     *
     * @param contents The string to parse.
     * @param property The name of the property is part belongs to.
     */
    public Part(String contents, String property) {
      // Many of these regular expressions are adapted from those used in the YUI CSS Compressor.
      // For simpler regexes.
      this._contents = " " + contents;
      this._property = property;
      simplify();
    }

    /**
     * Simplifies the part.
     */
    private void simplify() {
      // !important doesn't need to be spaced
      this._contents = this._contents.replaceAll(" !important", "!important");

      // Replace 0in, 0cm, etc. with just 0
      this._contents = this._contents.replaceAll("(\\s)(0)(px|em|%|in|cm|mm|pc|pt|ex)", "$1$2");

      // Now we can trim
      this._contents = this._contents.trim();

      // Simplify multiple zeroes
      if (this._contents.equals("0 0 0 0")) {
        this._contents = "0";
      }
      if (this._contents.equals("0 0 0")) {
        this._contents = "0";
      }
      if (this._contents.equals("0 0")) {
        this._contents = "0";
      }

      // Simplify multiple-parameter properties
      simplifyParameters();

      // Simplify font weights
      simplifyFontWeights();

      // Strip unnecessary quotes from url() and single-word parts, and make as much lowercase as possible.
      simplifyQuotesAndCaps();

      // Simplify colours
      simplifyColourNames();
      simplifyHexColours();
    }

    /**
     * Simplifies multiple-parameter properties.
     */
    private void simplifyParameters() {
      StringBuilder newContents = new StringBuilder();

      String[] params = this._contents.split(" ");
      if (params.length == 4) {
        // We can drop off the fourth item if the second and fourth items match
        // ie turn 3px 0 3px 0 into 3px 0 3px
        if (params[1].equalsIgnoreCase(params[3])) {
          params = Arrays.copyOf(params, 3);
        }
      }
      if (params.length == 3) {
        // We can drop off the third item if the first and third items match
        // ie turn 3px 0 3px into 3px 0
        if (params[0].equalsIgnoreCase(params[2])) {
          params = Arrays.copyOf(params, 2);
        }
      }
      if (params.length == 2) {
        // We can drop off the second item if the first and second items match
        // ie turn 3px 3px into 3px
        if (params[0].equalsIgnoreCase(params[1])) {
          params = Arrays.copyOf(params, 1);
        }
      }

      for (String param : params) {
        newContents.append(param + " ");
      }
      newContents.deleteCharAt(newContents.length() - 1); // Delete the trailing space

      this._contents = newContents.toString();
    }

    /**
     * Simplifies font weights.
     */
    private void simplifyFontWeights() {
      // only applies to font-weights
      if (!this._property.equals("font-weight")) return;

      String lcContents = this._contents.toLowerCase();

      for (int i = 0; i < Constants.FONT_WEIGHT_NAMES.length; i++) {
        if (lcContents.equals(Constants.FONT_WEIGHT_NAMES[i])) {
          this._contents = Constants.FONT_WEIGHT_VALUES[i];
          break;
        }
      }
    }

    /**
     * Simplifies quotes and caps.
     */
    private void simplifyQuotesAndCaps() {
      // Strip quotes from URLs
      if ((this._contents.length() > 4) && ("url(".equalsIgnoreCase(this._contents.substring(0, 4)))) {
        this._contents = this._contents.replaceAll("(?i)url\\(('|\")?(.*?)\\1\\)", "url($2)");
      } else {
        String[] words = this._contents.split("\\s");
        if (words.length == 1) {
          this._contents = this._contents.toLowerCase();
          this._contents = this._contents.replaceAll("('|\")?(.*?)\1", "$2");
        }
      }
    }

    /**
     * Simplifies color names.
     */
    private void simplifyColourNames() {
      String lcContents = this._contents.toLowerCase();

      for (int i = 0; i < Constants.HTML_COLOR_NAMES.length; i++) {
        if (lcContents.equals(Constants.HTML_COLOR_NAMES[i])) {
          if (Constants.HTML_COLOR_VALUES[i].length() < Constants.HTML_COLOR_NAMES[i].length()) {
            this._contents = Constants.HTML_COLOR_VALUES[i];
          }
          break;
        } else if (lcContents.equals(Constants.HTML_COLOR_VALUES[i])) {
          if (Constants.HTML_COLOR_NAMES[i].length() < Constants.HTML_COLOR_VALUES[i].length()) {
            this._contents = Constants.HTML_COLOR_NAMES[i];
          }
        }
      }
    }

    /**
     * Simplifies color names.
     */
    private void simplifyHexColours() {
      StringBuffer newContents = new StringBuffer();

      Pattern pattern = Pattern.compile("#([0-9a-fA-F])([0-9a-fA-F])([0-9a-fA-F])([0-9a-fA-F])([0-9a-fA-F])([0-9a-fA-F])");
      Matcher matcher = pattern.matcher(this._contents);

      while (matcher.find()) {
        if (matcher.group(1).equalsIgnoreCase(matcher.group(2)) && matcher.group(3).equalsIgnoreCase(matcher.group(4)) && matcher.group(5).equalsIgnoreCase(matcher.group(6))) {
          matcher.appendReplacement(newContents, "#" + matcher.group(1).toLowerCase() + matcher.group(3).toLowerCase() + matcher.group(5).toLowerCase());
        } else {
          matcher.appendReplacement(newContents, matcher.group().toLowerCase());
        }
      }
      matcher.appendTail(newContents);

      this._contents = newContents.toString();
    }

    /**
     * Returns itself.
     * @return this part's string representation.
     */
    @Override
    public String toString() {
      return this._contents;
    }
  }


  /**
   * Main entry point for CSSMin from the command-line.
   *
   * <b>Usage:</b> CSSMin <i>[Input file]</i>, <i>[Output file]</i>
   *
   * @param args The command-line arguments
   */
  public static void main(String[] args) {
    if (args.length < 1) {
      System.out.println("Usage: ");
      System.out.println("CSSMin [Input file] [Output file]");
      System.out.println("If no output file is specified, stdout will be used.");
      return;
    }

    PrintStream out;

    if (args.length > 1) {
      try {
        out = new PrintStream(args[1]);
      } catch (IOException ex) {
        System.err.println("Error outputting to " + args[1] + "; redirecting to stdout");
        out = System.out;
      }
    } else {
      out = System.out;
    }
    minimize(args[0], out);
  }

}

/**
 * Constants for replacement.
 *
 * @author Christophe Lauret
 * @version 21 November 2012
 */
final class Constants {

  /**
   * Color name - index must match color codes below.
   */
  static final String[] HTML_COLOR_NAMES = {
    "aliceblue",
    "antiquewhite",
    "aqua",
    "aquamarine",
    "azure",
    "beige",
    "bisque",
    "black",
    "blanchedalmond",
    "blue",
    "blueviolet",
    "brown",
    "burlywood",
    "cadetblue",
    "chartreuse",
    "chocolate",
    "coral",
    "cornflowerblue",
    "cornsilk",
    "crimson",
    "cyan",
    "darkblue",
    "darkcyan",
    "darkgoldenrod",
    "darkgray",
    "darkgreen",
    "darkkhaki",
    "darkmagenta",
    "darkolivegreen",
    "darkorange",
    "darkorchid",
    "darkred",
    "darksalmon",
    "darkseagreen",
    "darkslateblue",
    "darkslategray",
    "darkturquoise",
    "darkviolet",
    "deeppink",
    "deepskyblue",
    "dimgray",
    "dodgerblue",
    "firebrick",
    "floralwhite",
    "forestgreen",
    "fuchsia",
    "gainsboro",
    "ghostwhite",
    "gold",
    "goldenrod",
    "gray",
    "green",
    "greenyellow",
    "honeydew",
    "hotpink",
    "indianred ",
    "indigo ",
    "ivory",
    "khaki",
    "lavender",
    "lavenderblush",
    "lawngreen",
    "lemonchiffon",
    "lightblue",
    "lightcoral",
    "lightcyan",
    "lightgoldenrodyellow",
    "lightgrey",
    "lightgreen",
    "lightpink",
    "lightsalmon",
    "lightseagreen",
    "lightskyblue",
    "lightslategray",
    "lightsteelblue",
    "lightyellow",
    "lime",
    "limegreen",
    "linen",
    "magenta",
    "maroon",
    "mediumaquamarine",
    "mediumblue",
    "mediumorchid",
    "mediumpurple",
    "mediumseagreen",
    "mediumslateblue",
    "mediumspringgreen",
    "mediumturquoise",
    "mediumvioletred",
    "midnightblue",
    "mintcream",
    "mistyrose",
    "moccasin",
    "navajowhite",
    "navy",
    "oldlace",
    "olive",
    "olivedrab",
    "orange",
    "orangered",
    "orchid",
    "palegoldenrod",
    "palegreen",
    "paleturquoise",
    "palevioletred",
    "papayawhip",
    "peachpuff",
    "peru",
    "pink",
    "plum",
    "powderblue",
    "purple",
    "red",
    "rosybrown",
    "royalblue",
    "saddlebrown",
    "salmon",
    "sandybrown",
    "seagreen",
    "seashell",
    "sienna",
    "silver",
    "skyblue",
    "slateblue",
    "slategray",
    "snow",
    "springgreen",
    "steelblue",
    "tan",
    "teal",
    "thistle",
    "tomato",
    "turquoise",
    "violet",
    "wheat",
    "white",
    "whitesmoke",
    "yellow",
    "yellowgreen"
  };

  /**
   * Color hex codes - index must match color names.
   */
  static final String[] HTML_COLOR_VALUES = {
    "#f0f8ff",
    "#faebd7",
    "#00ffff",
    "#7fffd4",
    "#f0ffff",
    "#f5f5dc",
    "#ffe4c4",
    "#000",
    "#ffebcd",
    "#00f",
    "#8a2be2",
    "#a52a2a",
    "#deb887",
    "#5f9ea0",
    "#7fff00",
    "#d2691e",
    "#ff7f50",
    "#6495ed",
    "#fff8dc",
    "#dc143c",
    "#0ff",
    "#00008b",
    "#008b8b",
    "#b8860b",
    "#a9a9a9",
    "#006400",
    "#bdb76b",
    "#8b008b",
    "#556b2f",
    "#ff8c00",
    "#9932cc",
    "#8b0000",
    "#e9967a",
    "#8fbc8f",
    "#483d8b",
    "#2f4f4f",
    "#00ced1",
    "#9400d3",
    "#ff1493",
    "#00bfff",
    "#696969",
    "#1e90ff",
    "#b22222",
    "#fffaf0",
    "#228b22",
    "#f0f",
    "#dcdcdc",
    "#f8f8ff",
    "#ffd700",
    "#daa520",
    "#808080",
    "#008000",
    "#adff2f",
    "#f0fff0",
    "#ff69b4",
    "#cd5c5c",
    "#4b0082",
    "#fffff0",
    "#f0e68c",
    "#e6e6fa",
    "#fff0f5",
    "#7cfc00",
    "#fffacd",
    "#add8e6",
    "#f08080",
    "#e0ffff",
    "#fafad2",
    "#d3d3d3",
    "#90ee90",
    "#ffb6c1",
    "#ffa07a",
    "#20b2aa",
    "#87cefa",
    "#789",
    "#b0c4de",
    "#ffffe0",
    "#0f0",
    "#32cd32",
    "#faf0e6",
    "#f0f",
    "#800000",
    "#66cdaa",
    "#0000cd",
    "#ba55d3",
    "#9370d8",
    "#3cb371",
    "#7b68ee",
    "#00fa9a",
    "#48d1cc",
    "#c71585",
    "#191970",
    "#f5fffa",
    "#ffe4e1",
    "#ffe4b5",
    "#ffdead",
    "#000080",
    "#fdf5e6",
    "#808000",
    "#6b8e23",
    "#ffa500",
    "#ff4500",
    "#da70d6",
    "#eee8aa",
    "#98fb98",
    "#afeeee",
    "#d87093",
    "#ffefd5",
    "#ffdab9",
    "#cd853f",
    "#ffc0cb",
    "#dda0dd",
    "#b0e0e6",
    "#800080",
    "#f00",
    "#bc8f8f",
    "#4169e1",
    "#8b4513",
    "#fa8072",
    "#f4a460",
    "#2e8b57",
    "#fff5ee",
    "#a0522d",
    "#c0c0c0",
    "#87ceeb",
    "#6a5acd",
    "#708090",
    "#fffafa",
    "#00ff7f",
    "#4682b4",
    "#d2b48c",
    "#008080",
    "#d8bfd8",
    "#ff6347",
    "#40e0d0",
    "#ee82ee",
    "#f5deb3",
    "#fff",
    "#f5f5f5",
    "#ff0",
    "#9acd32"
  };

  /**
   * Font weight names - index must match font weight values below.
   */
  static final String[] FONT_WEIGHT_NAMES = {
    "normal",
    "bold",
    "bolder",
    "lighter"
  };

  /**
   * Font weight value - index must match font weight names below.
   */
  static final String[] FONT_WEIGHT_VALUES = {
    "400",
    "700",
    "900",
    "100"
  };
}
