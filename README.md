Elasticsearch Handlebars Lang Plugin
=======================

## Overview

This plugin add Handlebars language to Elasticsearch.

## Version

| Version   | Elasticsearch |
|:---------:|:-------------:|
| master    | 2.4.X         |
| 2.4.0     | 2.4.0         |
| 2.3.0     | 2.3.1         |
| 2.2.0     | 2.2.2         |
| 1.4.1     | 1.4.0         |

### Issues/Questions

Please file an [issue](https://github.com/codelibs/elasticsearch-lang-handlebars/issues "issue").
(Japanese forum is [here](https://github.com/codelibs/codelibs-ja-forum "here").)

## Installation

### Install Handlebars Language Plugin

    $ $ES_HOME/bin/plugin install org.codelibs/elasticsearch-lang-handlebars/2.4.0

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

