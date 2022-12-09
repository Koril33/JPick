package cn.korilweb;

import cn.korilweb.entity.DownloadInfoDTO;
import cn.korilweb.parser.BilibiliParser;
import cn.korilweb.task.DownloadTask;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.concurrent.Task;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * @author DJH
 * @date 2022-11-25 10:42:38
 */
public class ParseVideoURLTests {

    @Test
    void jsoupURLTest() {
        String urlStr = "https://www.bilibili.com/video/BV1N24y117QE/?spm_id_from=333.1007.tianma.1-2-2.click&vd_source=9c191f5acdee17439ba76e3e048ef3f7";
        // 视频音频信息存储在 该字符串开头的 <script> 中
        String startFlag = "window.__playinfo__=";

        try {
            Document doc = Jsoup.connect(urlStr).get();
            System.out.println(doc.title());
            Elements script = doc.select("script");
            List<Element> elements = script.stream().filter(e -> e.data().startsWith(startFlag)).collect(Collectors.toList());
            Assertions.assertEquals(1, elements.size());

            String originStr = elements.get(0).data();
            String jsonDataStr = originStr.substring(startFlag.length());
            System.out.println(jsonDataStr);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(jsonDataStr);
            JsonNode urlNode = node.get("data").get("dash").get("video").get(0).get("baseUrl");
            Assertions.assertNotNull(urlNode);
            try (
                    BufferedInputStream in = Jsoup.connect(urlNode.asText())
                            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/107.0.0.0 Safari/537.36")
                            .header("Referer", urlStr)
                            .ignoreContentType(true)
                            .execute().bodyStream();

                    BufferedOutputStream videoOut = new BufferedOutputStream(
                            Files.newOutputStream(
                                    Path.of("C:\\Users\\dingj\\Desktop\\test.mp4")
                            )
                    )
            ) {

//            Files.copy(in, Path.of("./test.mp4"), StandardCopyOption.REPLACE_EXISTING);
                int videoReadByte;
                while ((videoReadByte = in.read()) != -1) {
                    videoOut.write(videoReadByte);
                }
            }


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void parseTest() throws IOException {
        DownloadInfoDTO downloadInfoDTO = BilibiliParser.getDownloadInfoDTO("https://www.bilibili.com/video/BV1tP4y1Q7jC/?vd_source=9c191f5acdee17439ba76e3e048ef3f7");
        System.out.println(downloadInfoDTO.getVideoURL());
    }

    @Test
    void downloadTaskTest() throws IOException {
        // 短视频
        String shortUrlStr = "https://www.bilibili.com/video/BV1N24y117QE/?spm_id_from=333.1007.tianma.1-2-2.click&vd_source=9c191f5acdee17439ba76e3e048ef3f7";
        // 免费电影
        String longFreeUrlStr = "https://www.bilibili.com/bangumi/play/ss12548?theme=movie&from_spmid=666.7.hotlist.2";
        // 付费电影
        String longUrlStr = "https://www.bilibili.com/bangumi/play/ep469285?theme=movie&from_spmid=666.7.recommend.1";
        DownloadInfoDTO dto = BilibiliParser.getDownloadInfoDTO(longUrlStr);
        String desStr = "C:\\Users\\dingj\\Desktop";
        DownloadTask task = new DownloadTask(dto, desStr);
        task.download();
    }


    @Test
    void getSizeTest() throws IOException {
        DownloadInfoDTO dto = BilibiliParser.getDownloadInfoDTO("https://www.bilibili.com/video/BV1SP411A7QL/");

        URLConnection connection = dto.getVideoURL().openConnection();
        connection.setRequestProperty("referer", dto.getOriginURL().toString());
        long contentLengthLong = connection.getContentLengthLong();
        System.out.println(contentLengthLong);
    }

    @Test
    void singleThreadDownload() throws IOException {
        DownloadInfoDTO dto = BilibiliParser.getDownloadInfoDTO("https://www.bilibili.com/video/BV1SP411A7QL/");
        URLConnection connection = dto.getVideoURL().openConnection();
        connection.setRequestProperty("referer", dto.getOriginURL().toString());

    }
}
