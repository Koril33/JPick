package cn.korilweb.controller;

import cn.korilweb.entity.TaskDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
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
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.stream.Collectors;


public class IndexController implements Initializable {

    @FXML
    private Button createTask;

    @FXML
    private Button runningTasks;

    @FXML
    private Button finishedTasks;

    @FXML
    private VBox taskStack;

    @FXML
    private ScrollPane scrollPane;


    @FXML
    void newTask(ActionEvent event) {
        System.out.println(this);
        Stage stage = new Stage();

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("task.fxml"));
        Parent rootNode;
        try {
            rootNode = fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        stage.setScene(new Scene(rootNode));
        stage.setTitle("JPick | 一个简单的b站下载器");
        stage.show();
    }

    @FXML
    void startDownload(TaskDTO taskDTO) {
        createDownloadTaskBar(taskStack, taskDTO);
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        MainController.setIndexController(this);
    }

    private void createDownloadTaskBar(VBox stack, TaskDTO dto) {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("running.fxml"));
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Pane pane = fxmlLoader.getRoot();
        Label label = (Label) pane.getChildren().get(0);
        ProgressBar bar = (ProgressBar) pane.getChildren().get(1);
        stack.getChildren().add(pane);

        downloadBiliBiliVideo(label, bar, dto);

    }

    private void downloadBiliBiliVideo(Label label, ProgressBar bar, TaskDTO dto) {

        final String startFlag = "window.__playinfo__=";

        String urlStr = dto.getSrcPathStr();
        String desStr = dto.getDesPathStr();

        try {
            // Jsoup 获取视频原始 DOM
            Document doc = Jsoup.connect(urlStr).get();

            // 视频标题
            String title = doc.title();
            String fileName = title.replace(" ", "_")
                                   .replace("\\", "_")
                                   .replace("/", "_")
                                   .replace(".", "_");
            label.setText(fileName);

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

            Task<Void> downloadTask = new DownloadTask(urlStr, videoUrlStr, audioUrlStr, size, desStr, fileName);
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

        private final String fileName;


        public DownloadTask(String urlStr, String videoUrlStr, String audioUrlStr, long size, String desStr, String fileName) {
            this.urlStr = urlStr;
            this.videoUrlStr = videoUrlStr;
            this.audioUrlStr = audioUrlStr;
            this.size = size;
            this.desStr = desStr;
            this.fileName = fileName;
        }

        @Override
        protected Void call() throws Exception {
            var desStrPath = Path.of(desStr);
            var videoPath = desStrPath.resolve(fileName + "_video.mp4");
            var audioPath = desStrPath.resolve(fileName + "_audio.mp3");
            var outputPath = desStrPath.resolve(fileName + "_output.mp4");

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
