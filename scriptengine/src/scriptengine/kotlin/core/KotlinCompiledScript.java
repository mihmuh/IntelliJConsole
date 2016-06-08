package scriptengine.kotlin.core;

import scriptengine.kotlin.KotlinScriptEngine;
import scriptengine.kotlin.util.ReflUtils;

import javax.script.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;

public class KotlinCompiledScript extends CompiledScript {

  private final KotlinScriptEngine engine;
  private final String script;
  private final File file;

  public KotlinCompiledScript(KotlinScriptEngine engine, String script, File file) {
    this.engine = engine;
    this.script = script;
    this.file = file;
  }

  public static KotlinCompiledScript createCompiledScript(KotlinScriptEngine engine, String script) throws IOException {
    File console_tempdir = Files.createTempDirectory("console_tempdir").toFile();
    File scriptFile = new File(console_tempdir.getAbsolutePath() + "/script.kt");
    return new KotlinCompiledScript(engine, script, scriptFile);
  }

  @Override
  public Object eval(ScriptContext context) throws ScriptException {
    Class<?> aClass;
    try {
      FileWriter out = new FileWriter(file);
      out.write(script);
      out.flush();
      out.close();
    } catch (IOException e) {
      throw new ScriptException(e);
    }
    aClass = new KotlinCompiler().compileScript(file);
    Bindings bnd = context.getBindings(ScriptContext.ENGINE_SCOPE);
    bnd.put(ScriptEngine.FILENAME, file.getAbsolutePath());
    Method executeMethod;
    executeMethod = ReflUtils.findMethod(aClass, KotlinScriptEngine.EXECUTE_METHOD_NAME);
    if (executeMethod != null) {
      try {
        return executeMethod.invoke(null);
      } catch (IllegalAccessException | InvocationTargetException e) {
        throw new ScriptException(e);
      }
    } else {
      throw new ScriptException("Generated class does not have method `" + KotlinScriptEngine.EXECUTE_METHOD_NAME + "`");
    }

  }

  @Override
  public ScriptEngine getEngine() {
    return engine;
  }
}