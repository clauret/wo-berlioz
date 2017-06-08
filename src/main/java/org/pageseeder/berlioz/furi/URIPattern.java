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
package org.pageseeder.berlioz.furi;

import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.Nullable;


/**
 * A URI Pattern for matching URI following the same regular structure.
 *
 * <p>
 * Instances of this class implement the PageSeeder URL pattern as defined by the "PageSeeder URI
 * Templates" document.
 *
 * <p>
 * A PageSeeder URI Pattern follows the URI syntax defined for URI templates but must only contain
 * matchable tokens.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.11.2
 * @since Berlioz 0.9.32
 */
public final class URIPattern extends URITemplate implements Matchable {

  /**
   * The regular expression pattern for matching URIs to this URI Pattern.
   */
  private Pattern _pattern;

  /**
   * The score for this pattern, the length of the literal text.
   */
  private int _score = -1;

  /**
   * Creates a new URI Pattern instance from the specified URI template string.
   *
   * @param template The string following the URI template syntax.
   *
   * @throws IllegalArgumentException If the string provided does not follow the proper syntax.
   */
  public URIPattern(String template) {
    super(template);
    if (!isMatchable(this))
      throw new IllegalArgumentException("Cannot create a URL pattern containing non-matchable tokens.");
    this._pattern = computePattern(tokens());
  }

  /**
   * Creates a new URI Pattern instance from an existing URI Template.
   *
   * @param template The URI template to generate the pattern from.
   *
   * @throws IllegalArgumentException If the string provided does not follow the proper syntax.
   */
  public URIPattern(URITemplate template) {
    super(template);
    if (!isMatchable(template))
      throw new IllegalArgumentException("Cannot create a URL pattern from template containing non-matchable tokens.");
    this._pattern = computePattern(tokens());
  }

  /**
   * Indicates whether the given URI template can be used to construct a new URI Pattern instance.
   *
   * <p>
   * A template is matchable only if all its components are matchable tokens, that is the token
   * implements the {@link Matchable} interface.
   *
   * @param template The template to test.
   *
   * @return <code>true</code> if the template is matchable; <code>false</code> otherwise.
   */
  public static boolean isMatchable(URITemplate template) {
    // return false if any non-matchable token is found.
    for (Token t : template.tokens()) {
      if (!(t instanceof Matchable))
        return false;
    }
    // all tokens are matchable at this point.
    return true;
  }

  /**
   * Indicates whether this URI Pattern matches the specified URL.
   *
   * @param uri The URI to test.
   *
   * @return <code>true</code> if this URI Pattern matches this
   */
  @Override
  public boolean match(String uri) {
    return this._pattern.matcher(uri).matches();
  }

  /**
   * Returns the regular expression pattern corresponding to this URI pattern.
   *
   * @return The regular expression pattern corresponding to this URI pattern.
   */
  @Override
  public Pattern pattern() {
    return this._pattern;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    return super.equals(o);
  }

  @Override
  public int hashCode() {
    // use the original regex pattern string because the Pattern object's hashCode
    return 7 * this._pattern.pattern().hashCode() + 3 * toString().hashCode() + 31;
  }

  /**
   * Returns the score for this URI pattern.
   *
   * The score corresponds to the length of literal content.
   *
   * @return the score for this URI pattern.
   */
  protected int score() {
    if (this._score < 0) {
      this._score = computeScore(tokens());
    }
    return this._score;
  }

  // private helpers ----------------------------------------------------------

  /**
   * Compute the Regular Expression pattern for this URI Pattern.
   *
   * Important note: the regular expression contain the same number of capturing groups as the
   * number of token to facilitate the resolve process.
   *
   * @param tokens The list of tokens to compute the pattern.
   *
   * @return The regex Pattern instance corresponding to this URI pattern.
   */
  private Pattern computePattern(List<Token> tokens) {
    StringBuilder p = new StringBuilder();
    for (Token t : tokens) {
      Matchable mt = (Matchable) t;
      // wrap each token in a capturing group to facilitate the resolve process.
      p.append('(');
      p.append(mt.pattern());
      p.append(')');
    }
    return Pattern.compile(p.toString());
  }

  /**
   * Compute the score from the specified tokens.
   *
   * The score is the sum of the length of each literal token.
   *
   * @param tokens The list of tokens to compute the score.
   *
   * @return The score from the specified tokens.
   */
  private int computeScore(List<Token> tokens) {
    int score = 0;
    for (Token t : tokens) {
      if (t instanceof TokenLiteral) {
        score += t.expression().length();
      }
    }
    return score;
  }

}
