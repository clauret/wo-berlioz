/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.util;

import java.io.IOException;

import com.topologi.diffx.xml.XMLWritable;
import com.topologi.diffx.xml.XMLWriter;

/**
 * A simple class to associate an error collected by a parser to a level or seriousness.
 *
 * <p>This class is designed to be used with an {@link org.xml.sax.ErrorHandler.ErrorHandler}
 * or an {@link javax.xml.transform.ErrorListener} so that errors can be collected in a simple list.
 *
 * @param <T> The type of error collected.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.8.3 - 1 July 2011
 * @since Berlioz 0.8.1
 */
public final class CollectedError<T extends Exception> implements XMLWritable {

  /**
   * The level of collected error.
   *
   * <p>Note: the ordinal of this enumeration constant (its position in its enum declaration)
   * is significant as it is used to compare levels.
   */
  public enum Level {

    /** Warning reported by the underlying process. */
    WARNING,

    /** For normal non-fatal errors reported by the underlying process. */
    ERROR,

    /** Error was considered fatal by the underlying process. */
    FATAL;

    @Override
    public String toString() {
      return this.name().toLowerCase();
    }
  };

  /**
   * The seriousness of the error.
   */
  private final Level _level;

  /**
   * The actual error (may be an exception, message as a string, etc...)
   */
  private final T _error;

  /**
   * Creates a new collected error.
   *
   * @param level The seriousness of the error.
   * @param error The error itself.
   *
   * @throws NullPointerException If either argument is <code>null</code>.
   */
  public CollectedError(Level level, T error) {
    if (level == null) throw new NullPointerException("level was not specified");
    if (error == null) throw new NullPointerException("error was not specified");
    this._level = level;
    this._error = error;
  }

  /**
   * The seriousness of the error.
   *
   * @return The captured error.
   */
  public Level level() {
    return this._level;
  }

  /**
   * The captured error.
   *
   * @return The captured error.
   */
  public T error() {
    return this._error;
  }

  /**
   * Returns the source locator as XML.
   *
   * <p>Does nothing if the locator is <code>null</code>.
   *
   * @param xml     the XML writer.
   *
   * @throws IOException If thrown by the XML writer.
   */
  @Override
  public void toXML(XMLWriter xml) throws IOException {
    xml.openElement("collected");
    xml.attribute("level", this._level.toString());
    Errors.toXML(this._error, xml, false);
    xml.closeElement();
  }

}
