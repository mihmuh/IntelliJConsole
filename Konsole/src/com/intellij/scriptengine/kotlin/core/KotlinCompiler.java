package com.intellij.scriptengine.kotlin.core;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import kotlin.Unit;
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys;
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments;
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation;
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity;
import org.jetbrains.kotlin.cli.common.messages.MessageCollector;
import org.jetbrains.kotlin.cli.common.modules.ModuleBuilder;
import org.jetbrains.kotlin.cli.common.modules.ModuleXmlParser;
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler;
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinToJVMBytecodeCompiler;
import org.jetbrains.kotlin.cli.jvm.config.JVMConfigurationKeys;
import org.jetbrains.kotlin.codegen.GeneratedClassLoader;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.codegen.state.GenerationStateEventCallback;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.utils.KotlinPaths;
import org.jetbrains.kotlin.utils.PathUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.intellij.scriptengine.kotlin.util.ReflUtils;

import javax.script.ScriptException;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import static org.jetbrains.kotlin.cli.jvm.config.JvmContentRootsKt.addJvmClasspathRoots;
import static org.jetbrains.kotlin.config.ContentRootsKt.addKotlinSourceRoot;

public class KotlinCompiler implements MessageCollector, Disposable {

    private static final Logger log = LoggerFactory.getLogger("kc");

    private final KotlinPaths paths;

    public KotlinCompiler() {
        this.paths = PathUtil.getKotlinPathsForCompiler();
    }

    public Class<?> compileScript(File file) throws ScriptException {
        ArrayList<ModuleBuilder> modules = new ArrayList<ModuleBuilder>();
        ModuleBuilder module = new ModuleBuilder("dynamic", file.getParent(), ModuleXmlParser.TYPE_PRODUCTION);
        module.addSourceFiles("script.kt");
        modules.add(module);
        CompilerConfiguration configuration = createCompilerConfig(file);
        KotlinCoreEnvironment environment = KotlinCoreEnvironment.createForProduction(Disposer.newDisposable(), configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES);
        GenerationState state = KotlinToJVMBytecodeCompiler.INSTANCE.analyzeAndGenerate(environment, new GenerationStateEventCallback() {
            @Override
            public Unit invoke(GenerationState generationState) {
                return Unit.INSTANCE;
            }
        });
        KotlinToJVMBytecodeCompiler.INSTANCE.compileModules(environment, configuration, modules, file.getParentFile(), null, new ArrayList<>(), false);
        GeneratedClassLoader generatedClassLoader;
        if (state == null) {
            throw new ScriptException(errors.get(0));
        }
        try {
            generatedClassLoader = new GeneratedClassLoader(state.getFactory(), new URLClassLoader(new URL[]{new URL("file://" + file.getParentFile().getAbsolutePath())}, null));
            return generatedClassLoader.loadClass("ScriptKt");
        } catch (MalformedURLException | ClassNotFoundException e) {
            throw new ScriptException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private CompilerConfiguration addCurrentClassPath(CompilerConfiguration config) {
        K2JVMCompilerArguments cmpArgs = new K2JVMCompilerArguments();
        cmpArgs.classpath = System.getProperty("java.class.path");
        cmpArgs.noStdlib = true;
        addJvmClasspathRoots(config, (List<File>) ReflUtils.invoke(K2JVMCompiler.Companion.class, null,
                "access$getClasspath", K2JVMCompiler.Companion, paths, cmpArgs));
        return config;
    }

    private CompilerConfiguration createCompilerConfig(File file) {
        CompilerConfiguration config = new CompilerConfiguration();
        config.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, this);
        // Put arguments as field
        //MemoryScriptDefinition memDef = new MemoryScriptDefinition();
        //memDef.inject(StandardScriptDefinition.INSTANCE);
        // Bundle injection
        //List<ScriptParameter> scriptParams = new LinkedList<ScriptParameter>();
        //KotlinType type = DefaultBuiltIns.getInstance().getMutableMap().getDefaultType();
        //Name ctxName = Name.identifier("ctx");
        //scriptParams.add(new ScriptParameter(ctxName, type));
        //memDef.inject(scriptParams);
        // Set definitions
        //List<KotlinScriptDefinition> scriptDefs = new LinkedList<KotlinScriptDefinition>();
        //scriptDefs.add(memDef);
        // Finish configuration
        config.put(JVMConfigurationKeys.MODULE_NAME, "dynamic");
        //config.put(CommonConfigurationKeys.SCRIPT_DEFINITIONS_KEY, scriptDefs);
        addJvmClasspathRoots(config, PathUtil.getJdkClassesRoots());
        addKotlinSourceRoot(config, file.getParentFile().getAbsolutePath());
        return config;
    }

    private List<String> errors = new ArrayList<>();

    @Override
    public void report(CompilerMessageSeverity severity, String message, CompilerMessageLocation location) {
        switch (severity) {
            case ERROR:
            case EXCEPTION:
                errors.add(message + " " + location);
                log.error(message + " " + location);
                break;
            case INFO:
                log.info(message + " " + location);
                break;
            case WARNING:
                log.warn(message + " " + location);
                break;
            default:
                log.debug(message + " " + location);
                break;
        }
    }

    @Override
    public void dispose() {
        log.info("Disposed.");
    }
}