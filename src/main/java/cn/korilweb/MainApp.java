package cn.korilweb;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author DJH
 * @date 2022-11-23 18:34:52
 */
public class MainApp extends Application {
    @Override
    public void start(Stage stage) {
        initUI(stage);
    }

    private void initUI(Stage stage) {
        var root = new GridPane();
        root.setAlignment(Pos.CENTER);
        root.setHgap(5);
        root.setVgap(5);

        // Label 标签
        var picLabel = new Label("资源路径");
        var desLabel = new Label("保存地址");

        // URL 输入框
        var picInput = new TextField();
        picInput.setPrefWidth(500);
        // 保存地址输入框
        var desInput = new TextField();
        desInput.setPrefWidth(500);
        // 文件夹选择器
        var dirChoose = new DirectoryChooser();
        dirChoose.setTitle("图片下载路径");
        dirChoose.setInitialDirectory(
                // 默认是运行时的目录
                Path.of(
                        System.getProperty("user.dir")
                ).toFile()
        );

        // 文件夹选择器按钮
        var dirChooseBtn = new Button("选择文件夹");
        dirChooseBtn.setOnAction(
                e -> {
                    var selectedFileStr= dirChoose.showDialog(stage).toString();
                    if (Objects.isNull(selectedFileStr)) {
                        selectedFileStr = System.getProperty("user.dir");
                    }
                    desInput.setText(selectedFileStr);
                }
        );

        // 下载进度条
        ProgressBar bar = new ProgressBar();
        bar.setPrefWidth(500);
        // 下载按钮
        Button submit = new Button("下载");
        submit.setOnAction(event -> {
            try {
                download(picInput.getText(), desInput.getText(), bar);
                root.add(bar, 1, 2);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });



        // 将控件添加到根节点
        root.addRow(0, picLabel, picInput);
        root.addRow(1, desLabel, desInput, dirChooseBtn);
        root.add(submit, 1, 3);

        var scene = new Scene(root, 800, 200);

        stage.setScene(scene);
        stage.setTitle("JPick | 一个简单的b站下载器");
        stage.show();

    }

    private void download(String urlStr, String desStr, ProgressBar bar) throws IOException {
        // 判断下载图片还是下载 b 站视频
        // TODO 对输入 URL 进行校验
        final String tag = "bilibili";
        if (urlStr.contains(tag)) {
            downloadBiliBiliVideo(urlStr, desStr, bar);
        } else {
            downloadPicture(urlStr, desStr);
        }
    }

    private void downloadPicture(String urlStr, String desStr) throws IOException {
        URL url = new URL(urlStr);
        System.out.println(url.getPath());
        var urlCon = url.openConnection();
        var in = urlCon.getInputStream();
        // TODO 修改保存图片的名称
        Path des = Path.of(desStr).resolve("img.jpg");

        Files.copy(in, des, StandardCopyOption.REPLACE_EXISTING);
    }

    private void downloadBiliBiliVideo(String urlStr, String desStr, ProgressBar bar) {

        final String startFlag = "window.__playinfo__=";

        try {
            // Jsoup 获取视频原始 DOM
            Document doc = Jsoup.connect(urlStr).get();
            // 找到所有 <script> 元素
            Elements script = doc.select("script");
            // 找到包含特定开头的 <script>
            // 视频和音频的地址就在此 <script> 中，以 JSON 的格式保存
            List<Element> elements = script.stream()
                    .filter(
                            e -> e.data().startsWith(startFlag)
                    )
                    .collect(Collectors.toList());

            // 应该只有一个 <script> 元素
            // TODO 校验列表长度和判空
            String originStr = elements.get(0).data();
            // 去掉开头的特殊字符，获取完整的 JSON 字符串
            String jsonDataStr = originStr.substring(startFlag.length());

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

            Task<Void> downloadTask = new DownloadTask(urlStr, videoUrlStr, audioUrlStr, size, desStr);
            bar.progressProperty().bind(downloadTask.progressProperty());
            Thread downloadThread = new Thread(downloadTask);
            downloadThread.setDaemon(true);
            downloadThread.start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static class DownloadTask extends Task<Void> {
        private final String urlStr;
        private final String videoUrlStr;
        private final String audioUrlStr;
        private final long size;
        private final String desStr;


        public DownloadTask(String urlStr, String videoUrlStr, String audioUrlStr, long size, String desStr) {
            this.urlStr = urlStr;
            this.videoUrlStr = videoUrlStr;
            this.audioUrlStr = audioUrlStr;
            this.size = size;
            this.desStr = desStr;
        }

        @Override
        protected Void call() throws Exception {
            var desStrPath = Path.of(desStr);
            var videoPath = desStrPath.resolve("video.mp4");
            var audioPath = desStrPath.resolve("audio.mp3");
            var outputPath = desStrPath.resolve("output.mp4");

            try (
                    BufferedInputStream videoIn = Jsoup.connect(videoUrlStr)
                            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/107.0.0.0 Safari/537.36")
                            .header("Referer", urlStr)
                            .ignoreContentType(true)
                            .execute().bodyStream();
                    BufferedInputStream audioIn = Jsoup.connect(audioUrlStr)
                            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/107.0.0.0 Safari/537.36")
                            .header("Referer", urlStr)
                            .ignoreContentType(true)
                            .execute().bodyStream();

                    BufferedOutputStream videoOut = new BufferedOutputStream(
                            Files.newOutputStream(
                                    videoPath
                            )
                    );

                    BufferedOutputStream audioOut = new BufferedOutputStream(
                            Files.newOutputStream(
                                    audioPath
                            )
                    )

            ) {
                int videoReadByte;
                int cnt = 0;
                while ((videoReadByte = videoIn.read()) != -1) {
                    videoOut.write(videoReadByte);
                    cnt++;
                    updateProgress(cnt, size);
                }

                int audioReadByte;
                while ((audioReadByte = audioIn.read()) != -1) {
                    audioOut.write(audioReadByte);
                    cnt++;
                    updateProgress(cnt, size);
                }
                System.out.println("real cnt: " + cnt + ", predict size: " + size);

                // ffmpeg.exe
                // -i C:\Users\dingj\Desktop\video.mp4
                // -i C:\Users\dingj\Desktop\audio.mp3
                // -map 0:v
                // -map 1:a
                // -c:v copy
                // -c:a copy C:\Users\dingj\Desktop\output.mp4
                // -y
                String cmd = "ffmpeg.exe " +
                        " -i " + videoPath +
                        " -i " + audioPath +
                        " -map 0:v -map 1:a -c:v copy -c:a copy " +
                        outputPath +
                        " -y";
                System.out.println(cmd);
                Runtime.getRuntime().exec(cmd);
            }
            return null;
        }
    }
}
