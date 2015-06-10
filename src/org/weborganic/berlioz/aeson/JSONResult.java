/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.aeson;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URI;

import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamResult;

/**
 * A Result implementation automatically writing out JSON.
 *
 *
 * @see <a href="http://tools.ietf.org/html/rfc4627">The application/json Media Type for
 *  JavaScript Object Notation (JSON)</a>
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.9.32
 * @since Berlioz 0.9.32
 */
public class JSONResult extends SAXResult implements Result {

  /**
   * Zero-argument default constructor.
   *
   * <p>transformation results will go to <code>System.out</code>.
   */
  public JSONResult() {
    super(new JSONSerializer());
  }

  /**
   * Construct a JSONResult from a byte stream.
   *
   * @param out A valid OutputStream.
   */
  public JSONResult(OutputStream out) {
    super(new JSONSerializer(out));
  }

  /**
   * Construct a JSONResult from a character stream.
   *
   * <p>It is generally preferable to use a byte stream so that the encoding can controlled by the xsl:output
   * declaration; but can be convenient when using StringWriter
   *
   * @param writer A valid character stream.
   */
  public JSONResult(Writer writer) {
    super(new JSONSerializer(writer));
  }

  // Static helpers
  // ---------------------------------------------------------------------------------------------

  /**
   * Returns a new instance of the XSLT result if applicable.
   *
   * @param t      The XSLT transformer
   * @param result The result of transformation as a stream
   *
   * @return A new XSLT result if applicable.
   */
  public static Result newInstanceIfSupported(Transformer t, StreamResult result) {
    return supports(t)? newInstance(result) : result;
  }

  /**
   * Returns a new instance from the specified stream result.
   *
   * @param result a non-null stream result instance.
   *
   * @return a new <code>JSONResult</code> instance using the same properties as the stream result.
   *
   * @throws NullPointerException If the result is stream is <code>null</code>
   */
  public static JSONResult newInstance(StreamResult result) {
    // try to set the JSON result using the byte stream from the stream result
    OutputStream out = result.getOutputStream();
    String systemId = result.getSystemId();
    JSONResult json = null;
    if (out != null) {
      json = new JSONResult(out);
    } else {
      // try to set the JSON result using the character stream from the stream result
      Writer writer = result.getWriter();
      if (writer != null) {
        json = new JSONResult(writer);
      } else {
        if (systemId != null) {
          try {
            File f = new File(URI.create(systemId));
            FileOutputStream o = new FileOutputStream(f);
            json = new JSONResult(o);
          } catch (IOException ex) {
            // TODO: Handle this proper
            ex.printStackTrace();
          }
        } else {
          // Will output to System.out
          json = new JSONResult();
        }
      }
    }
    if (systemId != null) {
      json.setSystemId(systemId);
    }
    return json;
  }

  /**
   * Indicates whether the specified transformer based on its output properties.
   *
   * <p>the transformer is considered to support this Result type if it uses the "xml" method and
   * specifies the media type as "application/json".
   *
   * @param t the XSLT transformer implementation
   *
   * @return <code>true</code> if it matches the conditions above;
   *         <code>false</code> otherwise.
   */
  public static boolean supports(Transformer t) {
    String method = t.getOutputProperty("method");
    String media = t.getOutputProperty("media-type");
    return "xml".equals(method) && "application/json".equals(media);
  }

}
