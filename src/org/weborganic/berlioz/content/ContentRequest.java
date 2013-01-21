/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.content;

import java.util.Date;
import java.util.Enumeration;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;

/**
 * Provides a generic and uniform mechanism for the content generator to access parameters
 * and attributes from a request.
 *
 * All of the methods will return a <code>NullPointerException</code> if the specified
 * parameter name, attribute name or object name is <code>null</code>.
 *
 * @author Tu Tak Tran
 * @author Christophe Lauret
 *
 * @version Berlioz 0.9.13 - 21 January 2013
 * @since Berlioz 0.6
 */
public interface ContentRequest {

  /**
   * Returns the path information of this request.
   *
   * @deprecated Use {@link PathInfo} instead
   *
   * @return The path information of this request.
   */
  @Deprecated
  String getPathInfo();

  /**
   * Returns the dynamic path of the Berlioz request.
   *
   * <p>The Berlioz path corresponds to:
   * <ul>
   *   <li>the <code>pathInfo</code> when the Berlioz Servlet is mapped using a prefix servlet
   *   (for example <code>/html/*</code>);</li>
   *   <li>the <code>servletPath</code> when the Berlioz Servlet is mapped using a suffix servlet
   *   (for example <code>*.html</code>);</li>
   * </ul>
   *
   * <p>Use this method in preference to the {@link #getPathInfo()} which only works if Berlioz is
   * mapped to prefixes.
   *
   * @return The path information of this request.
   */
  String getBerliozPath();

  /**
   * Returns the specified parameter value or <code>null</code>.
   *
   * <p>This method guarantees that the returned value is not equal to an empty string.
   *
   * @param name The name of the requested parameter.
   *
   * @return A <code>String</code> or <code>null</code>.
   */
  String getParameter(String name);

  /**
   * Returns the specified parameter value or the specified default if <code>null</code>.
   *
   * <p>This method guarantees that a value is returned.
   *
   * @param name The name of the requested parameter.
   * @param def  A default value if the value is <code>null</code> or empty string.
   *
   * @return A value of the parameter or the default value if missing.
   */
  String getParameter(String name, String def);

  /**
   * Returns the specified parameter value.
   *
   * <p>This method guarantees that a value is returned.
   *
   * @param name The name of the requested parameter.
   * @param def  A default value if the value is <code>null</code> or empty string.
   *
   * @return A value of the parameter or the default value if missing or could not be parsed.
   */
  int getIntParameter(String name, int def);

  /**
   * Returns an array of String objects containing all of the values the given request parameter
   * has, or <code>null</code> if the parameter does not exist.
   *
   * <p>If the parameter has a single value, the array has a length of 1.
   *
   * @param name A String containing the name of the parameter whose value is requested
   *
   * @return An array of String objects containing the parameter's values
   */
  String[] getParameterValues(String name);

  /**
   * Returns an <code>Enumeration</code> of <code>String</code> objects containing the names of
   * the parameters contained in this request.
   *
   * <p>If the request has no parameters, the method returns an empty Enumeration.
   *
   * @return An <code>Enumeration</code> of the names of each parameters as <code>String</code>s;
   *          or an empty <code>Enumeration</code> if the request has no parameters.
   */
  Enumeration<String> getParameterNames();

  /**
   * Returns the specified attribute object or <code>null</code>.
   *
   * @param name The name of the attribute.
   *
   * @return the specified attribute object or <code>null</code>.
   */
  Object getAttribute(String name);

  /**
   * Sets the specified attribute object or <code>null</code>.
   *
   * @param name The name of the attribute.
   * @param o    The object for this attribute.
   */
  void setAttribute(String name, Object o);

  /**
   * Returns a <code>Date</code> instance from the specified parameter.
   *
   * <p><b>Important note</b>: incompatible change, since Berlioz 0.8, dates are parsed as ISO 8601.
   *
   * @param name The name of the parameter.
   *
   * @return A <code>Date</code> instance or <code>null</code> if not specified.
   */
  Date getDateParameter(String name);

  /**
   * Returns an array containing all of the Cookie objects the client sent with this request.
   *
   * This method returns <code>null</code> if no cookies were sent.
   *
   * @return An array of all the Cookies included with this request,
   *         or <code>null</code> if the request has no cookies
   */
  Cookie[] getCookies();

  /**
   * Returns the session of the wrapped HTTP servlet request.
   *
   * @return The session of the HTTP servlet request.
   */
  HttpSession getSession();

  /**
   * Returns the environment of the request.
   *
   * @return The environment of the request.
   */
  Environment getEnvironment();

  /**
   * Returns information about the location of the request.
   *
   * <p>This includes information about the request URI.
   *
   * @return information about the location of the request.
   */
  Location getLocation();

  /**
   * Sets the status of this request.
   *
   * @param code The status code to use.
   *
   * @throws NullPointerException if the status is <code>null</code>.
   * @throws IllegalArgumentException if the status is a redirect status.
   */
  void setStatus(ContentStatus code);

  /**
   * Sets the status of this request for redirection.
   *
   * @param code The status code to use (required).
   * @param url  The URL to redirect to.
   *
   * @throws NullPointerException if the URL is <code>null</code>.
   * @throws IllegalArgumentException if the status is not a redirect status.
   */
  void setRedirect(String url, ContentStatus code);

}
