/*
 * Copyright 2015 Allette Systems (Australia)
 * http://www.allette.com.au
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.pageseeder.berlioz.content;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.SAXParser;

import org.pageseeder.berlioz.BerliozErrorID;
import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.berlioz.BerliozOption;
import org.pageseeder.berlioz.GlobalSettings;
import org.pageseeder.berlioz.util.CollectedError.Level;
import org.pageseeder.berlioz.util.CompoundBerliozException;
import org.pageseeder.berlioz.xml.BerliozEntityResolver;
import org.pageseeder.berlioz.xml.SAXErrorCollector;
import org.pageseeder.berlioz.xml.XMLUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * A utility class to provide access to the content of generators.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.9.26 - 16 December 2013
 * @since Berlioz 0.6
 */
public final class ContentManager {

  /**
   * Displays debug information.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(ContentManager.class);

  /**
   * Maps content generators URL patterns to their content generator instance.
   */
  private static final ServiceRegistry SERVICES = new ServiceRegistry();

  /**
   * The file filter to
   */
  private static final FilenameFilter FILE_FILTER = new FilenameFilter() {

    @Override
    public boolean accept(File dir, String name) {
      return name.startsWith("services!") && name.endsWith(".xml");
    }
  };

  /**
   * Indicates whether the boolean value was loaded.
   */
  private static volatile boolean loaded = false;

  /**
   * Prevents creation of instances.
   */
  private ContentManager() {
    // no public constructors for utility classes
  }

  /**
   * Returns the default service registry (mapped to "services.xml").
   * @return the default service registry (mapped to "services.xml").
   */
  public static ServiceRegistry getDefaultRegistry() {
    return SERVICES;
  }

  /**
   * Update the patterns based on the current generators.
   *
   * @throws BerliozException Should something unexpected happen.
   *
   * @since Berlioz 0.8.2
   */
  public static synchronized void loadIfRequired() throws BerliozException {
    if (!loaded) {
      load();
      loaded = true;
    }
  }

  /**
   * Loads the content access file from all services files.
   *
   * @throws BerliozException Should something unexpected happen.
   */
  public static synchronized void load() throws BerliozException {
    File repository = GlobalSettings.getRepository();
    File config = new File(repository, "config");
    File xml = new File(config, "services.xml");
    load(xml);
    // look for other files
    for (File sub : config.listFiles(FILE_FILTER)) {
      load(sub);
    }
  }

  /**
   * @return the list of services files.
   */
  public static List<File> getServiceFiles() {
    File repository = GlobalSettings.getRepository();
    File config = new File(repository, "config");
    File xml = new File(config, "services.xml");
    List<File> files = new ArrayList<File>();
    files.add(xml);
    for (File sub : config.listFiles(FILE_FILTER)) {
      files.add(sub);
    }
    return files;
  }

  /**
   * Loads the content access file.
   *
   * @param xml    The XML file to load.
   *
   * @throws BerliozException Should something unexpected happen.
   */
  public static synchronized void load(File xml) throws BerliozException {
    if (xml == null)
      throw new NullPointerException("The service configuration file is null! That's it I give up.");
    // OK Let's start
    SAXParser parser = XMLUtils.getParser(true);
    SAXErrorCollector collector = new SAXErrorCollector(LOGGER);
    if (GlobalSettings.has(BerliozOption.XML_PARSE_STRICT)) {
      collector.setErrorFlag(Level.WARNING);
    }
    BerliozErrorID id = null;
    // Load the services
    try {
      XMLReader reader = parser.getXMLReader();
      HandlingDispatcher dispatcher = new HandlingDispatcher(reader, SERVICES);
      reader.setContentHandler(dispatcher);
      reader.setEntityResolver(BerliozEntityResolver.getInstance());
      reader.setErrorHandler(collector);
      LOGGER.info("Parsing "+xml.toURI().toString());
      reader.parse(new InputSource(xml.toURI().toString()));
      // if the error threshold was reached, throw an error!
      if (collector.hasError()) {
        id = BerliozErrorID.SERVICES_INVALID;
        throw new SAXException(collector.getErrors().size()+" error(s) reported by the XML parser.");
      }
    } catch (SAXException ex) {
      if (id == null) {
        id = BerliozErrorID.SERVICES_MALFORMED;
      }
      LOGGER.error("An SAX error occurred while reading XML service configuration: {}", ex.getMessage());
      throw new CompoundBerliozException("Unable to parse services configuration file.", ex, id, collector);
    } catch (IOException ex) {
      LOGGER.error("An I/O error occurred while reading XML service configuration: {}", ex.getMessage());
      throw new BerliozException("Unable to read services configuration file.", ex, BerliozErrorID.SERVICES_NOT_FOUND);
    }
    SERVICES.touch();
  }

  /**
   * Update the patterns based on the current generators.
   */
  public static synchronized void clear() {
    LOGGER.info("Clearing content manager");
    SERVICES.clear();
    loaded = false;
  }

  // Inner class to determine which handler to use --------------------------------------------------

  /**
   * A content handler to determine which handler implementation should be used to parse the
   * web access configuration.
   */
  private static final class HandlingDispatcher extends DefaultHandler implements ContentHandler {

    /**
     * Registry for the services to load.
     */
    private final ServiceRegistry _registry;

    /**
     * The reader in use.
     */
    private final XMLReader _reader;

    /**
     * The document locator for use when reporting the location of errors and warnings.
     */
    private Locator _locator;

    /**
     * Create a new version sniffer for the specified XML reader.
     *
     * @param reader   The XML Reader in use.
     * @param registry The service registry.
     */
    public HandlingDispatcher(XMLReader reader, ServiceRegistry registry) {
      this._reader = reader;
      this._registry = registry;
    }

    @Override
    public void setDocumentLocator(Locator locator) {
      this._locator = locator;
    }

    /**
     * Once the first element is matched, the reader is assigned the appropriate handler.
     *
     * {@inheritDoc}
     *
     * @throws SAXException if the the file being parsed is not a service configuration.
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
      // Identify the handler to use
      ContentHandler handler = getHandler(localName, atts);
      handler.setDocumentLocator(this._locator);
      // re-trigger events on handler to ensure proper initialisation
      handler.startDocument();
      handler.startElement(uri, localName, qName, atts);
      this._reader.setContentHandler(handler);
    }

    /**
     * Returns the content handler to use based on the element and its attributes
     *
     * @param name The name of the element (local)
     * @param atts The attributes attached to the element.
     *
     * @return The corresponding handler
     *
     * @throws SAXException if the the file being parsed is not a service configuration.
     */
    private ContentHandler getHandler(String name, Attributes atts) throws SAXException {
      SAXErrorCollector collector = (SAXErrorCollector)this._reader.getErrorHandler();
      // Service configuration
      if ("service-config".equals(name)) {
        String version = atts.getValue("version");

        // Version 1.0
        if ("1.0".equals(version)) {
          LOGGER.info("Service configuration 1.0 detected");
          return new ServicesHandler10(this._registry, collector);

        // Unknown version (assume 1.0)
        } else {
          LOGGER.info("Service configuration version unavailable, assuming 1.0");
          return new ServicesHandler10(this._registry, collector);
        }

      } else if ("services".equals(name)) {

        LOGGER.info("Services group using 1.0");
        return new ServicesHandler10(this._registry, collector);

      // Definitely not supported
      } else {
        LOGGER.error("Unable to determine Berlioz configuration");
        SAXParseException fatal = new SAXParseException("Not a valid Berlioz service configuration!", this._locator);
        collector.fatalError(fatal);
        // Just in case it wasn't thrown
        throw fatal;
      }
    }

  }

}