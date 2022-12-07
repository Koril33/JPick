package cn.korilweb.task;

import cn.korilweb.entity.DownloadInfoDTO;
import javafx.concurrent.Task;
import org.jsoup.Jsoup;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class DownloadTask extends Task<Void> {

    /**
     * 文件信息
     */
    private DownloadInfoDTO dto;


    /**
     * 下载本地路径
     */
    private String desPathStr;

    public DownloadTask(DownloadInfoDTO dto, String desPathStr) {
        this.dto = dto;
        this.desPathStr = desPathStr;
    }

    @Override
    protected Void call() throws Exception {
        var desPath = Path.of(desPathStr);

        String fileName = dto.getTitle().replace(" ", "_")
                                        .replace("/", "_")
                                        .replace("\\", "_")
                                        .replace(".", "_");

        var videoPath = desPath.resolve(fileName + "_video.mp4");
        var audioPath = desPath.resolve(fileName + "_audio.mp3");
        var outputPath = desPath.resolve(fileName + "_output.mp4");

        try (
                BufferedInputStream videoIn = Jsoup.connect(dto.getVideoURL().toString())
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/107.0.0.0 Safari/537.36")
                        .header("Referer", dto.getOriginURL().toString())
                        .ignoreContentType(true)
                        .execute().bodyStream();
                BufferedInputStream audioIn = Jsoup.connect(dto.getAudioURL().toString())
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/107.0.0.0 Safari/537.36")
                        .header("Referer", dto.getOriginURL().toString())
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
                updateProgress(cnt, dto.getSize());
            }

            int audioReadByte;
            while ((audioReadByte = audioIn.read()) != -1) {
                audioOut.write(audioReadByte);
                cnt++;
                updateProgress(cnt, dto.getSize());
            }
            System.out.println("real cnt: " + cnt + ", predict size: " + dto.getSize());

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

            Runtime.getRuntime().exec(cmd);
        }
        return null;
    }
}
