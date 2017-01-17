/*
 * Copyright (C) 2008 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.hippo.ehviewer.network.publicsuffix;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Parser for a map of reversed domain names stored as a serialized radix tree.
 */
// https://github.com/google/guava/blob/v21.0/guava/src/com/google/thirdparty/publicsuffix/TrieParser.java
final class TrieParser {

  /**
   * Parses a serialized trie representation of a map of reversed public suffixes into an immutable
   * map of public suffixes.
   */
  static Map<String, PublicSuffixType> parseTrie(CharSequence encoded, int size) {
    Map<String, PublicSuffixType> map = new HashMap<>(size);
    int encodedLen = encoded.length();
    int idx = 0;
    while (idx < encodedLen) {
      idx +=
          doParseTrieToBuilder(
              new LinkedList<CharSequence>(), encoded.subSequence(idx, encodedLen), map);
    }
    return map;
  }

  /**
   * Parses a trie node and returns the number of characters consumed.
   *
   * @param stack The prefixes that precede the characters represented by this node. Each entry of
   *     the stack is in reverse order.
   * @param encoded The serialized trie.
   * @param map A map to which all entries will be added.
   * @return The number of characters consumed from {@code encoded}.
   */
  private static int doParseTrieToBuilder(
      List<CharSequence> stack,
      CharSequence encoded,
      Map<String, PublicSuffixType> map) {

    int encodedLen = encoded.length();
    int idx = 0;
    char c = '\0';

    // Read all of the characters for this node.
    for (; idx < encodedLen; idx++) {
      c = encoded.charAt(idx);
      if (c == '&' || c == '?' || c == '!' || c == ':' || c == ',') {
        break;
      }
    }

    stack.add(0, reverse(encoded.subSequence(0, idx)));

    if (c == '!' || c == '?' || c == ':' || c == ',') {
      // '!' represents an interior node that represents an ICANN entry in the map.
      // '?' represents a leaf node, which represents an ICANN entry in map.
      // ':' represents an interior node that represents a private entry in the map
      // ',' represents a leaf node, which represents a private entry in the map.
      String domain = join(stack);
      if (domain.length() > 0) {
        map.put(domain, PublicSuffixType.fromCode(c));
      }
    }
    idx++;

    if (c != '?' && c != ',') {
      while (idx < encodedLen) {
        // Read all the children
        idx += doParseTrieToBuilder(stack, encoded.subSequence(idx, encodedLen), map);
        if (encoded.charAt(idx) == '?' || encoded.charAt(idx) == ',') {
          // An extra '?' or ',' after a child node indicates the end of all children of this node.
          idx++;
          break;
        }
      }
    }
    stack.remove(0);
    return idx;
  }

  private static CharSequence reverse(CharSequence s) {
    return new StringBuilder(s).reverse();
  }

  private static String join(List<CharSequence> list) {
    StringBuilder sb = new StringBuilder();
    for (CharSequence s: list) {
      sb.append(s);
    }
    return sb.toString();
  }
}
