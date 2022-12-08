package cn.korilweb.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.net.URL;
import java.nio.file.Path;


/**
 * 用户下载的地址通过 Jsoup 解析 DOM 后，
 * 存储文件信息于该类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
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
