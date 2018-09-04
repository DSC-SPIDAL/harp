/*
 * Copyright 2013-2017 Indiana University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.iu.examples;

import org.apache.commons.cli.CommandLine;

public final class Utils {
  private Utils() {
  }


  public static String getStringValue(String name, String def, CommandLine c) {
    if (c.hasOption(name)) {
      return c.getOptionValue(name);
    } else {
      return def;
    }
  }

  public static int getIntValue(String name, int def, CommandLine c) {
    if (c.hasOption(name)) {
      return Integer.parseInt(c.getOptionValue(name));
    } else {
      return def;
    }
  }
}
