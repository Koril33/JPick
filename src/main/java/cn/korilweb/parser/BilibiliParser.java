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
import java.util.List;
import java.util.stream.Collectors;

public class BilibiliParser {

    private final static String START_FLAG = "window.__playinfo__=";

    private static final DownloadInfoDTO dto = new DownloadInfoDTO();

    public static DownloadInfoDTO getDownloadInfoDTO(String originURLStr) throws IOException {

        dto.setOriginURL(new URL(originURLStr));

        // Jsoup 获取视频原始 DOM
        Document doc = Jsoup.connect(originURLStr).get();

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

        // Jackson 解析 JSON 字符串
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(jsonDataStr);
        // 找到视频的原始地址
        // TODO 视频和音频是分开存储的，还需要找到音频的地址，最后再进行合并处理
        JsonNode videoUrlNode = node.get("data").get("dash").get("video").get(0).get("baseUrl");
        JsonNode audioUrlNode = node.get("data").get("dash").get("audio").get(0).get("baseUrl");
        // 视频+音频总大小
        JsonNode videoSizeNode = node.get("data").get("dash").get("video").get(0).get("bandwidth");
        JsonNode audioSizeNode = node.get("data").get("dash").get("audio").get(0).get("bandwidth");
        JsonNode durationNode = node.get("data").get("dash").get("duration");
        long size = (videoSizeNode.asLong() + audioSizeNode.asLong()) * durationNode.asLong() / 8;

        String videoUrlStr = videoUrlNode.asText();
        String audioUrlStr = audioUrlNode.asText();

        dto.setVideoURL(new URL(videoUrlStr));
        dto.setAudioURL(new URL(audioUrlStr));
        dto.setSize(size);

        return dto;
    }
}
