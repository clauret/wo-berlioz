package org.weborganic.berlioz.util;

import org.weborganic.berlioz.BerliozException;
import org.weborganic.berlioz.ErrorID;

/**
 * A Berlioz exception to includes a list of collected errors.
 * 
 * @author Christophe Lauret
 * @version 30 June 2011
 */
public final class CompoundBerliozException extends BerliozException {

  /** 
   * As per requirement for the Serializable interface.
   */
  private static final long serialVersionUID = 6536134594918706098L;

  /**
   * The error collector.
   */
  private final ErrorCollector<? extends Exception> _collector;

  /**
   * Creates a new compound exception. 
   *
   * @param message   An explanatory message.
   * @param collector The error collector.
   */
  public CompoundBerliozException(String message, ErrorCollector<? extends Exception> collector) {
    super(message);
    this._collector = collector;
  }

  /**
   * Creates a new compound exception. 
   *
   * @param message   An explanatory message.
   * @param ex        The original exception causing this exception to be raised.
   * @param collector The error collector.
   */
  public CompoundBerliozException(String message, Exception ex, ErrorCollector<? extends Exception> collector) {
    super(message, ex);
    this._collector = collector;
  }

  /**
   * Creates a new compound exception. 
   *
   * @param message   An explanatory message.
   * @param id        An error ID to help with error handling and diagnostic.
   * @param collector The error collector.
   */
  public CompoundBerliozException(String message, ErrorID id, ErrorCollector<? extends Exception> collector) {
    super(message, id);
    this._collector = collector;
  }

  /**
   * Creates a new compound exception. 
   *
   * @param message   An explanatory message.
   * @param ex        The original exception causing this exception to be raised.
   * @param id        An error ID to help with error handling and diagnostic.
   * @param collector The error collector.
   */
  public CompoundBerliozException(String message, Exception ex, ErrorID id, ErrorCollector<? extends Exception> collector) {
    super(message, ex, id);
    this._collector = collector;
  }

  /**
   * The error collector included in this exception.
   * 
   * @return The error collector included in this exception.
   */
  public ErrorCollector<? extends Exception> getCollector() {
    return this._collector;
  }
}