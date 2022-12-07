package cn.korilweb.entity;

import lombok.Data;

import java.net.URL;
import java.nio.file.Path;

@Data
public class DownloadInfoDTO {

    /**
     * 视频标题名称
     */
    private String title;

    /**
     * 文件大小
     */
    private long size;

    /**
     * 视频原始路径
     */
    private URL originURL;

    /**
     * 视频下载真实路径
     */
    private URL videoURL;

    /**
     * 音频下载真实路径
     */
    private URL audioURL;
}
