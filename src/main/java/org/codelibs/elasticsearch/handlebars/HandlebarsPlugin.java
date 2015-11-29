package org.codelibs.elasticsearch.handlebars;

import org.codelibs.elasticsearch.handlebars.service.HandlebarsScriptEngineService;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.script.ScriptModule;

public class HandlebarsPlugin extends Plugin {
    @Override
    public String name() {
        return "lang-handlebars";
    }

    @Override
    public String description() {
        return "This plugin provides Handlebars language as a script.";
    }

    public void onModule(final ScriptModule module) {
        module.addScriptEngine(HandlebarsScriptEngineService.class);
    }

}
