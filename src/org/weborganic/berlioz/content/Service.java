/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.content;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weborganic.berlioz.Beta;
import org.weborganic.berlioz.content.ServiceStatusRule.SelectType;
import org.weborganic.berlioz.http.HttpMethod;

import com.topologi.diffx.xml.XMLWriter;

/**
 * A list of of content generators or content instructions.
 *
 * @author Christophe Lauret (Weborganic)
 *
 * @version Berlioz 0.9.3 - 9 December 2011
 * @since Berlioz 0.7
 */
public final class Service {

  /**
   * The ID of this service.
   */
  private final String _id;

  /**
   * The group this service is part of.
   */
  private final String _group;

  /**
   * Indicates whether this service can be cached.
   */
  private final boolean _cacheable;

  /**
   * The 'Cache-Control' header for this service.
   */
  private final String _cache;

  /**
   * How the status code this service is calculated.
   */
  private final ServiceStatusRule _rule;

  /**
   * The list of generators associated with this service.
   */
  private final List<ContentGenerator> _generators;

  /**
   * Maps parameter specifications to a given generator instance.
   */
  private final Map<ContentGenerator, List<Parameter>> _parameters;

  /**
   * Maps targets to a given generator instance.
   */
  private final Map<ContentGenerator, String> _targets;

  /**
   * Maps names to a given generator instance.
   */
  private final Map<ContentGenerator, String> _names;

  /**
   * Creates a new service.
   *
   * @param id         the ID of the service.
   * @param group      the group the service is part of.
   * @param cache      the cache control for this service.
   * @param rule       the status rule for the service status.
   * @param generators the list of generators.
   * @param parameters the parameters specifications for each generator.
   * @param names      the names of each generator (if any).
   * @param targets    the targets of each generator (if any).
   */
  private Service(String id, String group, String cache, ServiceStatusRule rule,
      List<ContentGenerator> generators,
      Map<ContentGenerator, List<Parameter>> parameters,
      Map<ContentGenerator, String> names,
      Map<ContentGenerator, String> targets) {
    this._id = id;
    this._group = group;
    this._cache = cache;
    this._rule = rule;
    this._generators = generators;
    this._parameters = parameters;
    this._cacheable = isCacheable(generators);
    this._names = names;
    this._targets = targets;
  }

  /**
   * Returns the ID of this service.
   *
   * @return the ID of this service.
   */
  public String id() {
    return this._id;
  }

  /**
   * Returns the group this service is part of.
   *
   * @return the group this service is part of.
   */
  public String group() {
    return this._group;
  }

  /**
   * Returns the value of the 'Cache-Control' for this service.
   *
   * @return the value of the 'Cache-Control' for this service.
   */
  public String cache() {
    return this._cache;
  }

  /**
   * Returns the status rule for this service.
   *
   * @return the status rule for this service.
   */
  public ServiceStatusRule rule() {
    return this._rule;
  }

  /**
   * Indicates whether this service is cacheable.
   *
   * <p>A service is cacheable only if all its generators are cacheable.
   *
   * @return <code>true</code> if this response is cacheable;
   *         <code>false</code> otherwise.
   */
  public boolean isCacheable() {
    return this._cacheable;
  }

  /**
   * Returns the list of generators for this service.
   *
   * @return the list of generators for this service.
   */
  public List<ContentGenerator> generators() {
    return this._generators;
  }

  /**
   * Returns the list of parameter specifications for the given generator.
   *
   * @param generator the content generator for which we need to parameters.
   * @return the list of parameter specifications for the given generator.
   */
  public List<Parameter> parameters(ContentGenerator generator) {
    List<Parameter> parameters = this._parameters.get(generator);
    if (parameters == null) return Collections.emptyList();
    return parameters;
  }

  /**
   * Returns the target of the given generator.
   *
   * @param generator the content generator for which we need the target.
   * @return the target if any (may be <code>null</code>).
   */
  public String target(ContentGenerator generator) {
    return this._targets.get(generator);
  }

  /**
   * Returns the name of the given generator.
   *
   * @param generator the content generator for which we need the name.
   * @return the name.
   */
  public String name(ContentGenerator generator) {
    String name = this._names.get(generator);
    return name != null? name : generator.getClass().getSimpleName();
  }

  /**
   * Indicates whether the specified generator affects the status of the service.
   * @param generator The generator.
   * @return <code>true</code> if the generator affects the status of the service;
   *         <code>false</code> otherwise.
   */
  public boolean affectStatus(ContentGenerator generator) {
    if (this._rule.appliesToAll()) return true;
    SelectType use = this._rule.use();
    switch (use) {
      case NAME:   return this._rule.appliesTo(this.name(generator));
      case TARGET: return this._rule.appliesTo(this.target(generator));
      default:     return false;
    }
  }

  @Override
  public String toString() {
    return "service:"+this._group+"/"+this._id;
  }

  /**
   * Serialises the specified service as XML.
   *
   * @param xml     the XML writer
   * @param method  the HTTP method the service is mapped to.
   * @param urls    the URI patterns this service matches
   *
   * @throws IOException if thrown by the XML writer.
   */
  @Beta
  public void toXML(XMLWriter xml, HttpMethod method, List<String> urls) throws IOException {
    toXML(xml, method, urls, null);
  }

  /**
   * Serialises the specified service as XML.
   *
   * @param xml          the XML writer
   * @param method       the HTTP method the service is mapped to.
   * @param urls         the URI patterns this service matches
   * @param cacheControl the cache control directives.
   *
   * @throws IOException if thrown by the XML writer.
   */
  @Beta
  public void toXML(XMLWriter xml, HttpMethod method, List<String> urls, String cacheControl) throws IOException {
    xml.openElement("service", true);
    xml.attribute("id", this._id);
    if (this._group != null)
      xml.attribute("group", this._group);
    if (method != null)
      xml.attribute("method", method.toString().toLowerCase());

    // Caching information
    xml.attribute("cacheable", Boolean.toString(this._cacheable));
    if (this._cacheable && (cacheControl != null || this._cache != null)) {
      xml.attribute("cache-control", this._cache != null? this._cache : cacheControl);
    }

    // How the response code is calculated
    xml.openElement("response-code", true);
    xml.attribute("use", this._rule.use().toString().toLowerCase());
    xml.attribute("rule", this._rule.rule().toString().toLowerCase());
    xml.closeElement();

    // URI patterns
    if (urls != null) {
      for (String url : urls) {
        xml.openElement("url", true);
        xml.attribute("pattern", url);
        xml.closeElement();
      }
    }

    // Generators
    for (ContentGenerator generator : this._generators) {
      List<Parameter> parameters = this.parameters(generator);
      xml.openElement("generator", !parameters.isEmpty());
      xml.attribute("class", generator.getClass().getName());
      xml.attribute("name", this.name(generator));
      xml.attribute("target", this.target(generator));
      xml.attribute("cacheable", Boolean.toString(generator instanceof Cacheable));
      xml.attribute("affect-status", Boolean.toString(this.affectStatus(generator)));
      for (Parameter p : parameters) {
        xml.openElement("parameter", false);
        xml.attribute("name", p.name());
        xml.attribute("value", p.value());
        xml.closeElement();
      }
      xml.closeElement();
    }

    xml.closeElement();
  }

  /**
   * Indicates whether the list of generators are all cacheable.
   *
   * @param generators the list of generators to evaluate.
   * @return <code>true</code> if all generators implement the {@link Cacheable} interface;
   *         <code>false</code> otherwise.
   */
  static boolean isCacheable(List<ContentGenerator> generators) {
    for (ContentGenerator g : generators) {
      if (!(g instanceof Cacheable)) return false;
    }
    return true;
  }

  /**
   * A builder for services to ensure that <code>Service</code> instances are immutable.
   *
   * <p>The same builder can be used for builder multiple services.
   *
   * @author Christophe Lauret (Weborganic)
   * @version 8 July 2010
   */
  static final class Builder {

    /**
     * The ID of the service to build.
     */
    private String _id;

    /**
     * The group the service to build belongs to.
     */
    private String _group;

    /**
     * The value of the 'Cache-Control' header for this service.
     */
    private String _cache;

    /**
     * Maps targets to a given generator instance.
     */
    private ServiceStatusRule _rule;

    /**
     * The list of generators associated with this service.
     */
    private final List<ContentGenerator> _generators = new ArrayList<ContentGenerator>();

    /**
     * Maps parameter specifications to a given generator instance.
     */
    private final Map<ContentGenerator, List<Parameter>> _parameters = new HashMap<ContentGenerator, List<Parameter>>();

    /**
     * Maps names to a given generator instance.
     */
    private final Map<ContentGenerator, String> _names = new HashMap<ContentGenerator, String>();

    /**
     * Maps targets to a given generator instance.
     */
    private final Map<ContentGenerator, String> _targets = new HashMap<ContentGenerator, String>();

    /**
     * Creates a new builder.
     */
    public Builder() {
    }

    /**
     * Returns the ID of the service to build.
     *
     * @return the ID of the service to build.
     */
    public String id() {
      return this._id;
    }

    /**
     * Sets the ID of the service to build.
     *
     * @param id the ID of the service to build.
     * @return this builder for easy chaining.
     */
    public Builder id(String id) {
      this._id = id;
      return this;
    }

    /**
     * Sets the group of the service to build.
     *
     * @param group the group of the service to build.
     * @return this builder for easy chaining.
     */
    public Builder group(String group) {
      this._group = group;
      return this;
    }

    /**
     * Sets the cache control for this service.
     *
     * @param cache the 'Cache-Control' value of the service to build.
     * @return this builder for easy chaining.
     */
    public Builder cache(String cache) {
      this._cache = cache;
      return this;
    }

    /**
     * Sets the status rule of the service to build.
     *
     * @param rule the status rule of the service to build.
     * @return this builder for easy chaining.
     */
    public Builder rule(ServiceStatusRule rule) {
      this._rule = rule;
      return this;
    }

    /**
     * Adds a parameter to the last content generator entered.
     *
     * @param p The parameter to add to the latest generator added.
     * @return this builder for easy chaining.
     */
    public Builder parameter(Parameter p) {
      if (this._generators.size() > 0) {
        ContentGenerator generator = this._generators.get(this._generators.size() - 1);
        List<Parameter> parameters = this._parameters.get(generator);
        if (parameters == null) {
          parameters = new ArrayList<Parameter>();
          this._parameters.put(generator, parameters);
        }
        parameters.add(p);
      }
      return this;
    }

    /**
     * Adds a content generator to this service.
     *
     * @param g the content generator to add to this service.
     * @return this builder for easy chaining.
     */
    public Builder add(ContentGenerator g) {
      this._generators.add(g);
      return this;
    }

    /**
     * Sets the target of the latest content generator added.
     *
     * @param target the target for the latest content generator.
     * @return this builder for easy chaining.
     */
    public Builder target(String target) {
      if (this._generators.size() > 0 && target != null) {
        ContentGenerator generator = this._generators.get(this._generators.size() - 1);
        this._targets.put(generator, target);
      }
      return this;
    }

    /**
     * Sets the name of the latest content generator added.
     *
     * @param name the name for the latest content generator.
     * @return this builder for easy chaining.
     */
    public Builder name(String name) {
      if (this._generators.size() > 0 && name != null) {
        ContentGenerator generator = this._generators.get(this._generators.size() - 1);
        this._names.put(generator, name);
      }
      return this;
    }

    /**
     * Builds the service from the attributes in this builder.
     *
     * <p>Note: use the <code>reset</code> method to reset the class attributes.
     *
     * @return a new service instance.
     */
    public Service build() {
      // warn when attempting to use cache control with uncacheable service
      if (this._cache != null && !isCacheable(this._generators)) {
        Logger logger = LoggerFactory.getLogger(Builder.class);
        logger.warn("Building non-cacheable service {} - cache control ignored.", this._id);
      }
      return new Service(this._id, this._group, this._cache, this._rule,
          immutable(this._generators),
          immutable(this._parameters),
          immutable3(this._names),
          immutable3(this._targets));
    }

    /**
     * Resets the all the class attributes (except group).
     */
    public void reset() {
      this._id = null;
      this._cache = null;
      this._generators.clear();
      this._parameters.clear();
      this._names.clear();
      this._targets.clear();
    }

    /**
     * Returns a new identical immutable list.
     *
     * @param original the list maintained by the builder.
     * @return a new identical immutable list.
     */
    private static List<ContentGenerator> immutable(List<ContentGenerator> original) {
      if (original.isEmpty())
        return Collections.emptyList();
      else if (original.size() == 1) return Collections.singletonList(original.get(0));
      else
        return Collections.unmodifiableList(new ArrayList<ContentGenerator>(original));
    }

    /**
     * Returns a new identical immutable map.
     *
     * @param original the map maintained by the builder.
     * @return a new identical immutable map.
     */
    private static Map<ContentGenerator, List<Parameter>> immutable(Map<ContentGenerator, List<Parameter>> original) {
      if (original.isEmpty())
        return Collections.emptyMap();
      else if (original.size() == 1) {
        Entry<ContentGenerator, List<Parameter>> entry = original.entrySet().iterator().next();
        return Collections.singletonMap(entry.getKey(), immutable2(entry.getValue()));
      } else {
        Map<ContentGenerator, List<Parameter>> map = new HashMap<ContentGenerator, List<Parameter>>();
        for (Entry<ContentGenerator, List<Parameter>> entry : original.entrySet()) {
          map.put(entry.getKey(), immutable2(entry.getValue()));
        }
        return Collections.unmodifiableMap(map);
      }
    }

    /**
     * Returns a new identical immutable list.
     *
     * @param original the list maintained by the builder.
     * @return a new identical immutable list.
     */
    private static List<Parameter> immutable2(List<Parameter> original) {
      if (original.isEmpty())
        return Collections.emptyList();
      else if (original.size() == 1) return Collections.singletonList(original.get(0));
      else
        return Collections.unmodifiableList(new ArrayList<Parameter>(original));
    }

    /**
     * Returns a new identical immutable map.
     *
     * @param original the map maintained by the builder.
     * @return a new identical immutable map.
     */
    private static Map<ContentGenerator, String> immutable3(Map<ContentGenerator, String> original) {
      if (original.isEmpty())
        return Collections.emptyMap();
      else if (original.size() == 1) {
        Entry<ContentGenerator, String> entry = original.entrySet().iterator().next();
        return Collections.singletonMap(entry.getKey(), entry.getValue());
      } else {
        Map<ContentGenerator, String> map = new HashMap<ContentGenerator, String>();
        for (Entry<ContentGenerator, String> entry : original.entrySet()) {
          map.put(entry.getKey(), entry.getValue());
        }
        return Collections.unmodifiableMap(map);
      }
    }

  }

}
