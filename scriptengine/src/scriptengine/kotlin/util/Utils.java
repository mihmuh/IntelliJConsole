/*
 * Copyright 2000-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package scriptengine.kotlin.util;

import javax.script.ScriptException;
import java.io.IOException;
import java.io.Reader;

/**
 * Created by Mihail.Buryakov on 6/8/2016.
 */
public class Utils {
  public static String readFully(Reader reader) throws ScriptException {
    char[] arr = new char[8192];
    StringBuilder buf = new StringBuilder();

    int numChars;
    try {
      while ((numChars = reader.read(arr, 0, arr.length)) > 0) {
        buf.append(arr, 0, numChars);
      }
    } catch (IOException e) {
      throw new ScriptException(e);
    }

    return buf.toString();
  }
}
