/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.xml;

import org.slf4j.Logger;
import org.weborganic.berlioz.util.CollectedError.Level;
import org.weborganic.berlioz.util.ErrorCollector;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

/**
 * A SAX error handler will collect all the exceptions reported by the SAX parser.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.8.5 - 15 August 2011
 * @since Berlioz 0.8
 */
public final class SAXErrorCollector extends ErrorCollector<SAXParseException> implements ErrorHandler {

  /**
   * The logger to use to report errors
   */
  private final Logger _logger;

  /**
   * Creates a new Berlioz error handler.
   *
   * @param logger A logger to report errors when the SAX methods are called.
   */
  public SAXErrorCollector(Logger logger) {
    this._logger = logger;
  }

  /**
   * @see org.xml.sax.ErrorHandler#fatalError(org.xml.sax.SAXParseException)
   *
   * @param exception A SAX parse fatal reported by the SAX parser.
   *
   * @throws SAXParseException If thrown by the underlying {@link ErrorCollector}.
   */
  @Override
  public void fatalError(SAXParseException exception) throws SAXParseException {
    this._logger.error("{} (line: {})", exception.getMessage(), exception.getLineNumber());
    collect(Level.FATAL, exception);
  }

  /**
   * @see org.xml.sax.ErrorHandler#error(org.xml.sax.SAXParseException)
   *
   * @param exception A SAX parse error reported by the SAX parser.
   *
   * @throws SAXParseException If thrown by the underlying {@link ErrorCollector}.
   */
  @Override
  public void error(SAXParseException exception) throws SAXParseException {
    this._logger.error("{} (line: {})", exception.getMessage(), exception.getLineNumber());
    collect(Level.ERROR, exception);
  }

  /**
   * @see org.xml.sax.ErrorHandler#warning(org.xml.sax.SAXParseException)
   *
   * @param exception A SAX parse warning reported by the SAX parser.
   *
   * @throws SAXParseException If thrown by the underlying {@link ErrorCollector}.
   */
  @Override
  public void warning(SAXParseException exception) throws SAXParseException {
    this._logger.warn("{} (line: {})", exception.getMessage(), exception.getLineNumber());
    collect(Level.WARNING, exception);
  }

}
