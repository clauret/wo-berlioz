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
package org.pageseeder.berlioz.servlet;

import java.util.Map;
import java.util.Objects;

import org.eclipse.jdt.annotation.Nullable;
import org.pageseeder.berlioz.content.ContentGenerator;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.berlioz.content.ContentStatus;
import org.pageseeder.berlioz.content.Service;

/**
 * Wraps a {@link javax.servlet.ServletRequest} instance and provide methods to access the parameters and attributes in
 * a consistent manner.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.11.2
 * @since Berlioz 0.9
 */
public final class HttpContentRequest extends HttpRequestWrapper implements ContentRequest {

  /**
   * Generator for which the request is designed for.
   */
  private final ContentGenerator generator;

  /**
   * Service the generator is part of.
   */
  private final Service service;

  /**
   * Indicates in which order it is going to be invoked starting with 0.
   */
  private final int order;

  /**
   * The status for this request.
   */
  private ContentStatus status = ContentStatus.OK;

  /**
   * The URL to redirect to.
   */
  private @Nullable String redirectTo = null;

  /**
   * Profiling information.
   */
  private long profile = 0;

  // sole constructor -------------------------------------------------------------------------------

  /**
   * Creates a new wrapper around the specified HTTP servlet request.
   *
   * @param core       The common HTTP info.
   * @param parameters The parameters to use.
   * @param generator  The generator for which this request is for.
   * @param service    The service this request is part of.
   * @param order      The order in which this request is called.
   *
   * @throws IllegalArgumentException If the request is <code>null</code>.
   */
  protected HttpContentRequest(CoreHttpRequest core, Map<String, String> parameters,
      ContentGenerator generator, Service service, int order) {
    super(core, parameters);
    this.generator = Objects.requireNonNull(generator, "The generator is required");
    this.service = Objects.requireNonNull(service, "The service is required");
    this.order = order;
  }

  /**
   * Sets the status of this request.
   *
   * @param status the status of this request.
   *
   * @throws NullPointerException if the status is <code>null</code>.
   * @throws IllegalArgumentException if the status is a redirect status.
   */
  @Override
  public void setStatus(ContentStatus status) {
    Objects.requireNonNull(status, "Cannot set status to null");
    if (ContentStatus.isRedirect(status))
      throw new IllegalArgumentException("Unable to use redirect status code:"+status);
    this.status = status;
  }

  /**
   * Sets the status of this request.
   *
   * {@inheritDoc}
   *
   * @throws NullPointerException if the URL is <code>null</code>.
   * @throws IllegalArgumentException if the status is not a redirect status.
   */
  @Override
  public void setRedirect(String url, @Nullable ContentStatus status) {
    Objects.requireNonNull(url, "Cannot set URL to null");
    if (status != null) {
      if (!ContentStatus.isRedirect(status)) throw new IllegalArgumentException("Invalid redirect status:"+status);
      this.status = status;
    } else {
      this.status = ContentStatus.TEMPORARY_REDIRECT;
    }
    this.redirectTo = url;
  }

  /**
   * Returns the status of this request.
   *
   * @return the status of this request.
   */
  public ContentStatus getStatus() {
    return this.status;
  }

  /**
   * Returns the generator for which this request is used for.
   *
   * @return the generator for which this request is used for.
   */
  public ContentGenerator generator() {
    return this.generator;
  }

  /**
   * Service the generator is part of.
   *
   * @return the service the generator is part of.
   */
  public Service getService() {
    return this.service;
  }

  /**
   * Returns the URL to redirect to.
   *
   * @return the URL to redirect to (may be <code>null</code>).
   */
  public @Nullable String getRedirectURL() {
    return this.redirectTo;
  }

  /**
   * @return the order in which it will be invoked.
   */
  protected int order() {
    return this.order;
  }

  /**
   * @param profile Nano time for the etag
   */
  protected void setProfileEtag(long profile) {
    this.profile = profile;
  }

  /**
   * @return Nano time for the etag
   */
  protected long getProfileEtag() {
    return this.profile;
  }
}
