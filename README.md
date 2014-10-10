Elasticsearch Handlebars Lang Plugin
=======================

## Overview

This plugin add Handlebars language to Elasticsearch.

## Version

| Taste     | Elasticsearch |
|:---------:|:-------------:|
| master    | 1.4.X         |

### Issues/Questions

Please file an [issue](https://github.com/codelibs/elasticsearch-lang-handlebars/issues "issue").
(Japanese forum is [here](https://github.com/codelibs/codelibs-ja-forum "here").)

## Installation

### Install Handlebars Language Plugin

TBD

    $ $ES_HOME/bin/plugin --install org.codelibs/elasticsearch-lang-handlebars/1.4.0

## References

This plugin supports an executable script language(search script is not supported).

### Using on Script-based Search Template

Using [Script-based Search Template](https://github.com/codelibs/elasticsearch-sstmpl "Script-based Search Template") Plugin, you can search by Handlebars template.

    GET /_search/template
    {
        "lang": "handlebars",
        "template": "{\"query\": {\"match\": {\"title\": \"{{query_string}}\"}}}",
        "params": {
            "query_string": "search for these words"
        }
    }

### How to Register Helpers

To register your helper, put .js file to $ES_HOME/config/helpers.

    $ echo "Handlebars.registerHelper('foobar', function (context) { return \"foobar\";})" > $ES_HOME/config/helpers/foobar.js

