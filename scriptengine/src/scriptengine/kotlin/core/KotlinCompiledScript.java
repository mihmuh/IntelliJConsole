package scriptengine.kotlin.core;

import scriptengine.kotlin.KotlinScriptEngine;

import javax.script.*;
import java.io.File;
import java.io.FileWriter;

public class KotlinCompiledScript extends CompiledScript {

  private final KotlinScriptEngine engine;
  private final String script;
  private final File file;

  private Class<?> cachedClazz;

  public KotlinCompiledScript(KotlinScriptEngine engine, String script, File file) {
    this.engine = engine;
    this.script = script;
    this.file = file;
  }

  @Override
  public Object eval(ScriptContext context) throws ScriptException {
    // Only compile if necessary (should be only the first time!)
    if (cachedClazz == null) {
      try {
        FileWriter out = new FileWriter(file);
        out.write(script);
        out.flush();
        out.close();
        cachedClazz = engine.compileScript(file);
      } catch (Exception e) {
        throw new ScriptException(e);
      }
    }
    // Evaluate it
    Bindings bnd = context.getBindings(ScriptContext.ENGINE_SCOPE);
    bnd.put(ScriptEngine.FILENAME, file.getAbsolutePath());
    return engine.evalClass(cachedClazz, bnd);
  }

  @Override
  public ScriptEngine getEngine() {
    return engine;
  }
}