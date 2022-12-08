package cn.korilweb.entity;

import lombok.Data;


/**
 * 获取用户的各项输入信息
 */
@Data
public class TaskDTO {

    private String srcPathStr;

    private String desPathStr;

    private boolean onlyVideo;

    private boolean onlyAudio;

    private boolean total;

}
