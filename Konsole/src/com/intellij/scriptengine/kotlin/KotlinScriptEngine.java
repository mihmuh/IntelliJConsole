package com.intellij.scriptengine.kotlin;

import com.intellij.scriptengine.kotlin.core.KotlinCompiledScript;
import com.intellij.scriptengine.kotlin.util.Utils;

import javax.script.*;
import java.io.IOException;
import java.io.Reader;

public class KotlinScriptEngine extends AbstractScriptEngine implements ScriptEngine, Compilable {

  private final KotlinScriptEngineFactory factory;

  public KotlinScriptEngine(KotlinScriptEngineFactory factory) {
    this.factory = factory;
  }

  @Override
  public CompiledScript compile(String script) throws ScriptException {
    try {
      return KotlinCompiledScript.createCompiledScript(this, script);
    } catch (IOException e) {
      throw new ScriptException(e);
    }
  }

  public static String evaluateScript(String text) throws ScriptException {
    /*def enginePlugin = PluginManagerCore.getPlugins().find {it -> it.getName().contains("Scripting") }
    def scriptEngine = new ScriptEngineManager(enginePlugin.getPluginClassLoader()).getEngineByExtension("kt")
*/
    ScriptEngine scriptEngine = new ScriptEngineManager().getEngineByExtension("kt");
    return scriptEngine.eval(text).toString();
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