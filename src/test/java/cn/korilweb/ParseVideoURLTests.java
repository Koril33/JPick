package cn.korilweb;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author DJH
 * @date 2022-11-25 10:42:38
 */
public class ParseVideoURLTests {


    @Test
    void jsoupStrTest() {
        String html = "<html><head><title>First parse</title></head>"
                + "<body><p>Parsed HTML into a doc.</p></body></html>";
        Document doc = Jsoup.parse(html);
//        System.out.println(doc);
//        doc.body();
        System.out.println(doc.body());
    }

    @Test
    void jsoupURLTest() {
        String urlStr = "https://www.bilibili.com/video/BV1AG4y187AH/?vd_source=e3cd65375a6679c8f162694a7431cd66";
        String startFlag = "window.__playinfo__=";

        try {
            Document doc = Jsoup.connect(urlStr).get();
            Elements script = doc.select("script");
//            script.eachText().forEach(System.out::println);
            List<Element> elements = script.stream().filter(e -> e.data().startsWith(startFlag)).collect(Collectors.toList());

            Assertions.assertEquals(1, elements.size());

            String originStr = elements.get(0).data();
            String jsonDataStr = originStr.substring(startFlag.length());
            System.out.println(jsonDataStr);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(jsonDataStr);
            JsonNode urlNode = node.get("data").get("dash").get("video").get(0).get("baseUrl");

            BufferedInputStream in = Jsoup.connect(urlNode.asText())
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/107.0.0.0 Safari/537.36")
                    .header("Referer", urlStr)
                    .ignoreContentType(true)
                    .execute().bodyStream();

            Files.copy(in, Path.of("./test.mp4"));
//            System.out.println(urlNode.asText());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
