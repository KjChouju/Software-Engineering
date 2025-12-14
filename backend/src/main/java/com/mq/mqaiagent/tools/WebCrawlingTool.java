package com.mq.mqaiagent.tools;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.IOException;


public class WebCrawlingTool {

    @Tool(description = "Crawl web pages")
    public String crawl(@ToolParam(description = "URL of the web page to Crawl") String url){
        try {
            Document doc = Jsoup.connect(url).get();
            return doc.html();
        } catch (IOException e) {
            return "Error Crawl web pages: " + e.getMessage();
        }
    }
}
