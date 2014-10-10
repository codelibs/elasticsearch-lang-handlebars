package org.codelibs.elasticsearch.handlebars;

import static org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner.newConfigs;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner;
import org.codelibs.elasticsearch.runner.net.Curl;
import org.codelibs.elasticsearch.runner.net.CurlResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.ImmutableSettings.Builder;
import org.elasticsearch.node.Node;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.io.Files;

public class HandlebarsPluginTest {
    ElasticsearchClusterRunner runner;

    private File esHomeDir;

    @Before
    public void setUp() throws Exception {
        esHomeDir = File.createTempFile("eshome", "");
        esHomeDir.delete();

        final File scriptDir = new File(esHomeDir, "config/scripts");
        scriptDir.mkdirs();
        Files.write(
                "{\"query\":{\"match\":{\"{{my_field}}\":\"{{my_value}}\"}},\"size\":\"{{my_size}}\"}"
                        .getBytes(), new File(scriptDir, "search_query_2.hbs"));
        final File helperDir = new File(esHomeDir, "config/helpers");
        helperDir.mkdirs();
        Files.write(
                "Handlebars.registerHelper('valueHelper', function (context) { return \"1\";})"
                        .getBytes(), new File(helperDir, "valueHelper.js"));

        runner = new ElasticsearchClusterRunner();
        runner.onBuild(new ElasticsearchClusterRunner.Builder() {
            @Override
            public void build(final int number, final Builder settingsBuilder) {
                settingsBuilder.put("http.cors.enabled", true);
            }
        }).build(
                newConfigs().numOfNode(1).ramIndexStore()
                        .basePath(esHomeDir.getAbsolutePath()));
        runner.ensureGreen();
    }

    @After
    public void tearDown() throws Exception {
        runner.close();
        esHomeDir.delete();
    }

    @Test
    public void test_search() throws Exception {

        assertThat(1, is(runner.getNodeSize()));

        final Node node = runner.node();

        final String index = "sample";
        final String type = "data";
        runner.createIndex(index, ImmutableSettings.builder().build());

        for (int i = 1; i <= 1000; i++) {
            final IndexResponse indexResponse = runner.insert(index, type,
                    String.valueOf(i), "{\"id\":\"" + i + "\",\"msg\":\"test "
                            + i + "\",\"counter\":" + i + ",\"category\":" + i
                            % 10 + "}");
            assertTrue(indexResponse.isCreated());
        }

        try (CurlResponse curlResponse = Curl
                .post(node, "/_scripts/handlebars/search_query_1")
                .body("{\"script\":\"{\\\"query\\\":{\\\"match\\\":{\\\"{{my_field}}\\\":\\\"{{my_value}}\\\"}},\\\"size\\\":\\\"{{my_size}}\\\"}\"}")
                .execute()) {
            assertThat(201, is(curlResponse.getHttpStatusCode()));
        }

        String query;
        /*
                query = "{\"query\":{\"match_all\":{}}}";
                try (CurlResponse curlResponse = Curl
                        .post(node, "/" + index + "/" + type + "/_search").body(query)
                        .execute()) {
                    Map<String, Object> contentMap = curlResponse.getContentAsMap();
                    Map<String, Object> hitsMap = (Map<String, Object>) contentMap
                            .get("hits");
                    assertThat(1000, is(hitsMap.get("total")));
                    assertThat(
                            10,
                            is(((List<Map<String, Object>>) hitsMap.get("hits")).size()));
                }

                query = "{\"template\":{\"query\":{\"match\":{\"{{my_field}}\":\"{{my_value}}\"}},\"size\":\"{{my_size}}\"},"
                        + "\"params\":{\"my_field\":\"category\",\"my_value\":\"1\",\"my_size\":\"50\"}}";
                try (CurlResponse curlResponse = Curl
                        .post(node, "/" + index + "/" + type + "/_search/template")
                        .body(query).execute()) {
                    Map<String, Object> contentMap = curlResponse.getContentAsMap();
                    Map<String, Object> hitsMap = (Map<String, Object>) contentMap
                            .get("hits");
                    assertThat(100, is(hitsMap.get("total")));
                    assertThat(
                            50,
                            is(((List<Map<String, Object>>) hitsMap.get("hits")).size()));
                }

                query = "{\"lang\":\"mustache\",\"template\":{\"query\":{\"match\":{\"{{my_field}}\":\"{{my_value}}\"}},\"size\":\"{{my_size}}\"},"
                        + "\"params\":{\"my_field\":\"category\",\"my_value\":\"1\",\"my_size\":\"50\"}}";
                try (CurlResponse curlResponse = Curl
                        .post(node, "/" + index + "/" + type + "/_search/template")
                        .body(query).execute()) {
                    Map<String, Object> contentMap = curlResponse.getContentAsMap();
                    Map<String, Object> hitsMap = (Map<String, Object>) contentMap
                            .get("hits");
                    assertThat(100, is(hitsMap.get("total")));
                    assertThat(
                            50,
                            is(((List<Map<String, Object>>) hitsMap.get("hits")).size()));
                }

                query = "{\"lang\":\"handlebars\",\"template\":\"{\\\"query\\\":{\\\"match\\\":{\\\"{{my_field}}\\\":\\\"{{my_value}}\\\"}},\\\"size\\\":\\\"{{my_size}}\\\"}\","
                        + "\"params\":{\"my_field\":\"category\",\"my_value\":\"1\",\"my_size\":\"50\"}}";
                try (CurlResponse curlResponse = Curl
                        .post(node, "/" + index + "/" + type + "/_search/template")
                        .body(query).execute()) {
                    Map<String, Object> contentMap = curlResponse.getContentAsMap();
                    Map<String, Object> hitsMap = (Map<String, Object>) contentMap
                            .get("hits");
                    assertThat(100, is(hitsMap.get("total")));
                    assertThat(
                            50,
                            is(((List<Map<String, Object>>) hitsMap.get("hits")).size()));
                }

                query = "{\"lang\":\"handlebars\",\"template\":{\"id\":\"search_query_1\"},"
                        + "\"params\":{\"my_field\":\"category\",\"my_value\":\"1\",\"my_size\":\"50\"}}";
                try (CurlResponse curlResponse = Curl
                        .post(node, "/" + index + "/" + type + "/_search/template")
                        .body(query).execute()) {
                    Map<String, Object> contentMap = curlResponse.getContentAsMap();
                    Map<String, Object> hitsMap = (Map<String, Object>) contentMap
                            .get("hits");
                    assertThat(100, is(hitsMap.get("total")));
                    assertThat(
                            50,
                            is(((List<Map<String, Object>>) hitsMap.get("hits")).size()));
                }

                query = "{\"lang\":\"handlebars\",\"template\":{\"file\":\"search_query_2\"},"
                        + "\"params\":{\"my_field\":\"category\",\"my_value\":\"1\",\"my_size\":\"50\"}}";
                try (CurlResponse curlResponse = Curl
                        .post(node, "/" + index + "/" + type + "/_search/template")
                        .body(query).execute()) {
                    Map<String, Object> contentMap = curlResponse.getContentAsMap();
                    Map<String, Object> hitsMap = (Map<String, Object>) contentMap
                            .get("hits");
                    assertThat(100, is(hitsMap.get("total")));
                    assertThat(
                            50,
                            is(((List<Map<String, Object>>) hitsMap.get("hits")).size()));
                }

                query = "{\"lang\":\"handlebars\",\"template\":\"{\\\"query\\\":{\\\"match\\\":{\\\"{{my_field}}\\\":\\\"{{my_value}}\\\"}},\\\"size\\\":\\\"{{my_size}}\\\"}\","
                        + "\"params\":{\"my_field\":\"category\",\"my_value\":\"1\",\"my_size\":\"50\"}}";
                try (CurlResponse curlResponse = Curl
                        .post(node, "/" + index + "/" + type + "/_search/template")
                        .body(query).execute()) {
                    Map<String, Object> contentMap = curlResponse.getContentAsMap();
                    Map<String, Object> hitsMap = (Map<String, Object>) contentMap
                            .get("hits");
                    assertThat(100, is(hitsMap.get("total")));
                    assertThat(
                            50,
                            is(((List<Map<String, Object>>) hitsMap.get("hits")).size()));
                }
         */
        query = "{\"lang\":\"handlebars\",\"template\":\"{\\\"query\\\":{\\\"match\\\":{\\\"{{my_field}}\\\":\\\"{{valueHelper this}}\\\"}},\\\"size\\\":\\\"{{my_size}}\\\"}\","
                + "\"params\":{\"my_field\":\"category\",\"my_value\":\"11\",\"my_size\":\"50\"}}";
        try (CurlResponse curlResponse = Curl
                .post(node, "/" + index + "/" + type + "/_search/template")
                .body(query).execute()) {
            final Map<String, Object> contentMap = curlResponse
                    .getContentAsMap();
            final Map<String, Object> hitsMap = (Map<String, Object>) contentMap
                    .get("hits");
            assertThat(100, is(hitsMap.get("total")));
            assertThat(
                    50,
                    is(((List<Map<String, Object>>) hitsMap.get("hits")).size()));
        }
    }
}
