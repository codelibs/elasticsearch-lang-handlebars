package org.codelibs.elasticsearch.handlebars;

import org.codelibs.elasticsearch.handlebars.service.HandlebarsScriptEngineService;
import org.elasticsearch.plugins.AbstractPlugin;
import org.elasticsearch.script.ScriptModule;

public class HandlebarsPlugin extends AbstractPlugin {
    @Override
    public String name() {
        return "HandlebarsPlugin";
    }

    @Override
    public String description() {
        return "This plugin provides Handlebars language as a script.";
    }

    public void onModule(final ScriptModule module) {
        module.addScriptEngine(HandlebarsScriptEngineService.class);
    }

}
