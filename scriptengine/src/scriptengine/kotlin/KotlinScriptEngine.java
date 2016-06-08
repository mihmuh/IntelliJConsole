package scriptengine.kotlin;

import scriptengine.kotlin.core.KotlinCompiledScript;
import scriptengine.kotlin.util.ReflUtils;
import scriptengine.kotlin.util.Utils;

import javax.script.*;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;

public class KotlinScriptEngine extends AbstractScriptEngine implements ScriptEngine, Compilable {

  private final KotlinScriptEngineFactory factory;

  public KotlinScriptEngine(KotlinScriptEngineFactory factory) {
    this.factory = factory;
  }

  @Override
  public CompiledScript compile(String script) throws ScriptException {
    try {
      return new KotlinCompiledScript(this, script, File.createTempFile("kc_cmp", ".kt"));
    } catch (IOException e) {
      throw new ScriptException(e);
    }
  }

  @Override
  public Object eval(String script, ScriptContext context) throws ScriptException {
    CompiledScript scr = compile(script);
    Bindings ctx = context.getBindings(ScriptContext.ENGINE_SCOPE);
    return scr.eval(ctx);
  }

  public static final String EXECUTE_METHOD_NAME = "execute";

  @Override
  public Bindings createBindings() {
    return new SimpleBindings();
  }

  @Override
  public ScriptEngineFactory getFactory() {
    return factory;
  }

  @Override
  public CompiledScript compile(Reader script) throws ScriptException {
    return compile(Utils.readFully(script));
  }

  @Override
  public Object eval(Reader script, ScriptContext context) throws ScriptException {
    return eval(Utils.readFully(script), context);
  }
}