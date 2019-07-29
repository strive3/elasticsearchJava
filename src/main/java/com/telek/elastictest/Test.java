package com.telek.elastictest;

import com.google.gson.Gson;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.get.MultiGetItemResponse;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Requests;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Test implements Serializable {
    private String host = "127.0.0.1";
    TransportClient client = null;


    @org.junit.Before
    public void getClient() throws UnknownHostException {
        Settings settings = Settings.builder().put("cluster.name", "my-application").build();
        client = new PreBuiltTransportClient(settings);
        client = client.addTransportAddress(new TransportAddress(InetAddress.getByName(host), 9300));

        System.out.println(client.toString());
    }

    /**
     * 创建索引
     */
    @org.junit.Test
    public void creatIndex() {
        client.admin().indices().prepareCreate("blog").get();
        client.close();
    }

    /**
     * 删除索引
     */
    @org.junit.Test
    public void deleteIndex() {
        client.admin().indices().prepareDelete("blog").get();

        client.close();
    }

    /**
     * 设置索引的mapping 映射
     * @throws Exception
     */
    @org.junit.Test
    public void setMapping() throws Exception {
        XContentBuilder builder = XContentFactory.jsonBuilder()
                .startObject()
                    .startObject("article")
                        .startObject("properties")
                            .startObject("id")
                            .field("type", "text")
                            .field("store", true)
                            .endObject()

                            .startObject("title")
                            .field("type", "text")
                            .field("store", true)
                            .field("analyzer", "ik_smart")
                            .endObject()

                            .startObject("content")
                            .field("type", "text")
                            .field("store", true)
                            .field("analyzer", "ik_smart")
                            .endObject()
                        .endObject()
                    .endObject()
                .endObject();

        PutMappingRequest mapping = Requests.putMappingRequest("blog").type("article").source(builder);
        client.admin().indices()
                .putMapping(mapping).get();

        client.close();
    }

    /**
     * 创建文档
     * @throws Exception
     */
    @org.junit.Test
    public void createIndexByBuilder() throws Exception {
        XContentBuilder builder = XContentFactory.jsonBuilder().startObject()
                .field("id", "1")
                .field("title", "绯红女巫")
                .field("content", "两封贺信背后的绿色故事")
                .endObject();

        client.prepareIndex("blog", "article", "1").setSource(builder).execute().actionGet();
        client.close();
    }

    /**
     * 索引，类型，文档一并创建，
     */
    @org.junit.Test
    public void creatIndexByJson() {
        Gson gson = new Gson();
        String json = gson.toJson(new Article("4", "看报", "英文报"));
        System.out.println(json);
        //下面的setSource 只能使用map  不能继续使用之前的json字符串了。
        Map<String, Object> map = new HashMap<String, Object>();
        map = gson.fromJson(json, map.getClass());
        IndexResponse indexResponse = client.prepareIndex("blog1", "artile", "3").setSource(map).execute().actionGet();


        System.out.println("getIndex:" + indexResponse.getIndex());
        System.out.println("getType:" + indexResponse.getType());
        System.out.println("getId:" + indexResponse.getId());
        System.out.println("getResult:" + indexResponse.getResult());
        System.out.println("getVersion:" + indexResponse.getVersion());
        client.close();
    }

    /**
     * 根据id查询
     */
    @org.junit.Test
    public void getData() {
        GetResponse response = client.prepareGet("blog1", "artile", "1").get();
        System.out.println(response.getSourceAsString());
        client.close();
    }

    /**
     * 批量根据id查询
     */
    @org.junit.Test
    public void queryMultiIndex() {
        MultiGetResponse responses = client.prepareMultiGet().add("blog1", "artile", "1")
                .add("blog1", "artile", "1", "2", "3").get();

        for (MultiGetItemResponse itemResponse : responses) {
            GetResponse response = itemResponse.getResponse();
            if (response.isExists()) {
                System.out.println(response.getSourceAsString());
            }
        }
        client.close();
    }


    /**
     * 条件查询
     * 查询所有
     */
    @org.junit.Test
    public void matchAllQuery() {
        //查询
        SearchResponse searchResponse = client.prepareSearch("blog1")
                .setTypes("artile")
                .setQuery(QueryBuilders.matchAllQuery()).get();
        //获取查询的对象
        SearchHits hits = searchResponse.getHits();
        System.out.println("结果数量：" + hits.getTotalHits());
        //遍历打印文档内容
        Iterator<SearchHit> iterator = hits.iterator();
        while (iterator.hasNext()) {
            SearchHit hit = iterator.next();
            System.out.println(hit.getSourceAsString());
        }
        //关闭资源
        client.close();
    }

    /**
     * 字段分词查询
     */
    @org.junit.Test
    public void query() {
        //查询
        SearchResponse searchResponse = client.prepareSearch("blog1")
                .setTypes("artile")
                .setQuery(QueryBuilders.queryStringQuery("蜘蛛啊")).get();
        //获取查询的对象
        SearchHits hits = searchResponse.getHits();
        System.out.println("结果数量：" + hits.getTotalHits());
        //遍历打印文档内容
        Iterator<SearchHit> iterator = hits.iterator();
        while (iterator.hasNext()) {
            SearchHit hit = iterator.next();
            System.out.println(hit.getSourceAsString());
        }
        //关闭资源
        client.close();
    }

    /**
     * 词条查询     以一个一个字为索引
     * termQuery
     */
    @org.junit.Test
    public void termQuery() {
        //查询
        SearchResponse searchResponse = client.prepareSearch("blog1")
                .setTypes("artile")
                .setQuery(QueryBuilders.termQuery("content", "蜘")).get();
        //获取查询的对象
        SearchHits hits = searchResponse.getHits();
        System.out.println("结果数量：" + hits.getTotalHits());
        //遍历打印文档内容
        Iterator<SearchHit> iterator = hits.iterator();
        while (iterator.hasNext()) {
            SearchHit hit = iterator.next();
            System.out.println(hit.getSourceAsString());
        }
        //关闭资源
        client.close();
    }


    /**
     * 通配符查询
     * wildcardQuery
     * *代表多个字符
     */
    @org.junit.Test
    public void wildcardQuery() {
        //查询
        SearchResponse searchResponse = client.prepareSearch("blog1")
                .setTypes("artile")
                .setQuery(QueryBuilders.wildcardQuery("content", "*蜘*")).get();
        //获取查询的对象
        SearchHits hits = searchResponse.getHits();
        System.out.println("结果数量：" + hits.getTotalHits());
        //遍历打印文档内容
        Iterator<SearchHit> iterator = hits.iterator();
        while (iterator.hasNext()) {
            SearchHit hit = iterator.next();
            System.out.println(hit.getSourceAsString());
        }
        //关闭资源
        client.close();
    }

    /**
     * 模糊查询   基本上 忽略大小写的
     * fuzzyQuery
     */
    @org.junit.Test
    public void fuzzyQuery() {
        //查询
        SearchResponse searchResponse = client.prepareSearch("blog1")
                .setTypes("artile")
                .setQuery(QueryBuilders.fuzzyQuery("content", "蜘")).get();
        //获取查询的对象
        SearchHits hits = searchResponse.getHits();
        System.out.println("结果数量：" + hits.getTotalHits());
        //遍历打印文档内容
        Iterator<SearchHit> iterator = hits.iterator();
        while (iterator.hasNext()) {
            SearchHit hit = iterator.next();
            System.out.println(hit.getSourceAsString());
        }
        //关闭资源
        client.close();
    }


    /**
     * 更新
     * @throws Exception
     */
    @org.junit.Test
    public void update() throws Exception {
        UpdateRequest updateRequest = new UpdateRequest("blog1", "artile", "2");

        updateRequest.doc(XContentFactory.jsonBuilder().startObject()
                .field("id", "2")
                .field("title", "你好")
                .field("content", "测试用例")
                .endObject());

        UpdateResponse updateResponse = client.update(updateRequest).get();
        System.out.println("getIndex:" + updateResponse.getIndex());
        System.out.println("getType:" + updateResponse.getType());
        System.out.println("getId:" + updateResponse.getId());
        System.out.println("getResult:" + updateResponse.getResult());
        System.out.println("getVersion:" + updateResponse.getVersion());
        client.close();
    }


    /**
     * 如果有的话 更新  ，没有的话 插入
     * @throws Exception
     */
    @org.junit.Test
    public void upsert() throws Exception {
        //这个文档没有就创建
        IndexRequest indexRequest = new IndexRequest("blog1", "artile", "5");
        indexRequest.source(XContentFactory.jsonBuilder().startObject()
                .field("id", "5")
                .field("title", "钢铁侠")
                .field("content", "贾维斯")
                .endObject());
        //有文档内容就更新
        UpdateRequest updateRequest = new UpdateRequest("blog1", "artile", "5");
        updateRequest.doc(XContentFactory.jsonBuilder().startObject()
                .field("id", "5")
                .field("title", "蜘蛛侠")
                .field("content", "小蜘蛛")
                .endObject());
        //相比于上一个更新  加了这么一句话
        updateRequest.upsert(indexRequest);
        //具体更新的操作
        UpdateResponse updateResponse = client.update(updateRequest).get();

        System.out.println("getIndex:" + updateResponse.getIndex());
        System.out.println("getType:" + updateResponse.getType());
        System.out.println("getId:" + updateResponse.getId());
        System.out.println("getResult:" + updateResponse.getResult());
        System.out.println("getVersion:" + updateResponse.getVersion());
        client.close();
    }

    /**
     * 删除文档
     */
    @org.junit.Test
    public void deleteData() {
        DeleteResponse response = client.prepareDelete("blog1", "artile", "2").get();
        client.close();
    }


}
