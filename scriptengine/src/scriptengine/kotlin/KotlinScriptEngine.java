package scriptengine.kotlin;

import scriptengine.kotlin.core.KotlinCompiledScript;
import scriptengine.kotlin.core.KotlinCompiler;
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
  private final KotlinCompiler compiler;

  public KotlinScriptEngine(KotlinScriptEngineFactory factory) {
    this.factory = factory;
    this.compiler = new KotlinCompiler();
  }

  @Override
  public CompiledScript compile(String script) throws ScriptException {
    try {
      return new KotlinCompiledScript(this, script, File.createTempFile("kc_cmp", ".kts"));
    } catch (IOException e) {
      throw new ScriptException(e);
    }
  }

  @Override
  public Object eval(String script, ScriptContext context) throws ScriptException {
    try {
      File file = File.createTempFile("kc_evl", ".kts");
      KotlinCompiledScript scr = new KotlinCompiledScript(this, script, file);
      Bindings ctx = context.getBindings(ScriptContext.ENGINE_SCOPE);
      return scr.eval(ctx);
    } catch (IOException e) {
      throw new ScriptException(e);
    }
  }

  public Object evalClass(Class<?> clazz, Bindings ctx) throws ScriptException {
    try {
      String[] args = (String[]) ctx.get(ScriptEngine.ARGV);
      // Fix arguments if null
      if (args == null) {
        args = new String[0];
      }
      // Get constructor and invoke that
      Constructor<?> constr = clazz.getConstructor(String[].class, Map.class);
      // Create new instance
      Object[] invArgs = new Object[]{args, ctx};
      Object obj = constr.newInstance(invArgs);
      // Invoke main method if given (non-script)
      Method mainMth;
      if ((mainMth = ReflUtils.findMethod(clazz, "main", String[].class)) != null) {
        mainMth.invoke(obj, new Object[]{args});
      }
      // Return it!
      return obj;
    } catch (Exception e) {
      throw new ScriptException(e);
    }
  }

  public Class<?> compileScript(File file) {
    return compiler.compileScript(file);
  }

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