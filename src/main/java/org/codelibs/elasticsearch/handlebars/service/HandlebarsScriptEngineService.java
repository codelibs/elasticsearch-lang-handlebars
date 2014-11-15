package org.codelibs.elasticsearch.handlebars.service;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.Map;

import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.io.UTF8StreamWriter;
import org.elasticsearch.common.io.stream.BytesStreamOutput;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.script.CompiledScript;
import org.elasticsearch.script.ExecutableScript;
import org.elasticsearch.script.ScriptEngineService;
import org.elasticsearch.script.SearchScript;
import org.elasticsearch.search.lookup.SearchLookup;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.HandlebarsException;
import com.github.jknack.handlebars.Template;

public class HandlebarsScriptEngineService extends AbstractComponent implements
        ScriptEngineService {

    private Handlebars handlebars;

    /** Thread local UTF8StreamWriter to store template execution results in, thread local to save object creation.*/
    private static ThreadLocal<SoftReference<UTF8StreamWriter>> utf8StreamWriter = new ThreadLocal<>();

    /** If exists, reset and return, otherwise create, reset and return a writer.*/
    private static UTF8StreamWriter utf8StreamWriter() {
        final SoftReference<UTF8StreamWriter> ref = utf8StreamWriter.get();
        UTF8StreamWriter writer = ref == null ? null : ref.get();
        if (writer == null) {
            writer = new UTF8StreamWriter(1024 * 4);
            utf8StreamWriter.set(new SoftReference<>(writer));
        }
        writer.reset();
        return writer;
    }

    @Inject
    public HandlebarsScriptEngineService(final Settings settings) {
        super(settings);

        handlebars = new Handlebars();

        final File baseDir = new File(settings.get(
                "script.handlebars.template.basedir", settings.get("path.conf")
                        + "/helpers"));
        final String suffix = settings.get("script.handlebars.template.suffix",
                ".js");

        if (baseDir.exists()) {
            final File[] listFiles = baseDir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(final File dir, final String name) {
                    return name.endsWith(suffix);
                }
            });
            for (final File file : listFiles) {
                try {
                    handlebars.registerHelpers(file);
                } catch (final Exception e) {
                    throw new HandlebarsException("Failed to register: "
                            + file.getAbsolutePath(), e);
                }
            }
        }

    }

    @Override
    public String[] types() {
        return new String[] { "handlebars" };
    }

    @Override
    public String[] extensions() {
        return new String[] { "handlebars", "hbs" };
    }

    @Override
    public boolean sandboxed() {
        return true;
    }

    @Override
    public Object compile(final String script) {
        try {
            return handlebars.compileInline(script);
        } catch (final IOException e) {
            throw new HandlebarsException("Failed to compile: " + script, e);
        }
    }

    @Override
    public ExecutableScript executable(final Object template,
            final Map<String, Object> vars) {
        return new HandlebarsExecutableScript((Template) template, vars);
    }

    @Override
    public SearchScript search(final Object template,
            final SearchLookup lookup, final Map<String, Object> vars) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object execute(final Object template, final Map<String, Object> vars) {
        final BytesStreamOutput result = new BytesStreamOutput();
        final UTF8StreamWriter writer = utf8StreamWriter().setOutput(result);
        try {
            ((Template) template).apply(vars, writer);
            writer.flush();
        } catch (final IOException e) {
            logger.error(
                    "Could not execute query template (failed to flush writer): ",
                    e);
        } finally {
            try {
                writer.close();
            } catch (final IOException e) {
                logger.error(
                        "Could not execute query template (failed to close writer): ",
                        e);
            }
        }
        return result.bytes();
    }

    @Override
    public Object unwrap(final Object value) {
        return value;
    }

    @Override
    public void scriptRemoved(CompiledScript script) {
        // Nothing to do here
    }

    @Override
    public void close() {
        // Nothing to do here
    }

    private class HandlebarsExecutableScript implements ExecutableScript {
        /** Compiled template object. */
        private Template template;

        /** Parameters to fill above object with. */
        private Map<String, Object> vars;

        /**
         * @param mustache the compiled template object
         * @param vars the parameters to fill above object with
         **/
        public HandlebarsExecutableScript(final Template template,
                final Map<String, Object> vars) {
            this.template = template;
            if (vars == null) {
                this.vars = Collections.emptyMap();
            } else {
                this.vars = vars;
            }
        }

        @Override
        public void setNextVar(final String name, final Object value) {
            vars.put(name, value);
        }

        @Override
        public Object run() {
            final BytesStreamOutput result = new BytesStreamOutput();
            final UTF8StreamWriter writer = utf8StreamWriter()
                    .setOutput(result);
            try {
                template.apply(vars, writer);
                writer.flush();
            } catch (final IOException e) {
                logger.error(
                        "Could not execute query template (failed to flush writer): ",
                        e);
            } finally {
                try {
                    writer.close();
                } catch (final IOException e) {
                    logger.error(
                            "Could not execute query template (failed to close writer): ",
                            e);
                }
            }
            return result.bytes();
        }

        @Override
        public Object unwrap(final Object value) {
            return value;
        }
    }
}
