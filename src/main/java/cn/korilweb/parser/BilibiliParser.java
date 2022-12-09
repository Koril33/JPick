package cn.korilweb.parser;

import cn.korilweb.entity.DownloadInfoDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 负责解析页面 DOM
 * 提取出视频文件的真是下载路径
 */
public class BilibiliParser {

    private final static String START_FLAG = "window.__playinfo__=";

    private static final DownloadInfoDTO dto = new DownloadInfoDTO();

    private static final Map<String, String> headers = new HashMap<>();


    public static DownloadInfoDTO getDownloadInfoDTO(String originURLStr) throws IOException {

//        headers.put("cookie", "");
        headers.put("referer", "https://www.bilibili.com/movie/?spm_id_from=333.1007.0.0");
        headers.put("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36");

        dto.setOriginURL(new URL(originURLStr));

        // Jsoup 获取视频原始 DOM
        Document doc = Jsoup.connect(originURLStr)
                            .headers(headers)
                            .get();
        // 视频标题
        String title = doc.title();
        dto.setTitle(title);

        // 找到所有 <script> 元素
        Elements script = doc.select("script");
        // 找到包含特定开头的 <script>
        // 视频和音频的地址就在此 <script> 中，以 JSON 的格式保存
        List<Element> elements = script.stream()
                .filter(
                        e -> e.data().startsWith(START_FLAG)
                )
                .collect(Collectors.toList());

        // 应该只有一个 <script> 元素
        // TODO 校验列表长度和判空
        String originStr = elements.get(0).data();
        // 去掉开头的特殊字符，获取完整的 JSON 字符串
        String jsonDataStr = originStr.substring(START_FLAG.length());
        System.out.println(jsonDataStr);
        // Jackson 解析 JSON 字符串
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(jsonDataStr);
        // 找到视频的原始地址
        // TODO 视频和音频是分开存储的，还需要找到音频的地址，最后再进行合并处理
        JsonNode videoUrlNode = node.get("data").get("dash").get("video").get(0).get("baseUrl");
        JsonNode audioUrlNode = node.get("data").get("dash").get("audio").get(0).get("baseUrl");
        // 视频+音频总大小
//        JsonNode videoSizeNode = node.get("data").get("dash").get("video").get(0).get("bandwidth");
//        JsonNode audioSizeNode = node.get("data").get("dash").get("audio").get(0).get("bandwidth");
//        JsonNode durationNode = node.get("data").get("dash").get("duration");
//        long size = (videoSizeNode.asLong() + audioSizeNode.asLong()) * durationNode.asLong() / 8;

        String videoUrlStr = videoUrlNode.asText();
        String audioUrlStr = audioUrlNode.asText();

        // 获取视频文件总大小
        URLConnection vCon = new URL(videoUrlStr).openConnection();
        vCon.setRequestProperty("referer", dto.getOriginURL().toString());
        long vSize = vCon.getContentLengthLong();

        System.out.println("video size: " + vSize);

        // 获取音频文件总大小
        URLConnection aCon = new URL(audioUrlStr).openConnection();
        aCon.setRequestProperty("referer", dto.getOriginURL().toString());
        long aSize = aCon.getContentLengthLong();

        System.out.println("audio size: " + aSize);


        dto.setVideoURL(new URL(videoUrlStr));
        dto.setAudioURL(new URL(audioUrlStr));
        dto.setSize(vSize + aSize);

        return dto;
    }
}
