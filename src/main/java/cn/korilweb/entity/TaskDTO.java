package cn.korilweb.entity;

import lombok.Data;

@Data
public class TaskDTO {

    private String srcPathStr;

    private String desPathStr;

    private boolean onlyVideo;

    private boolean onlyAudio;

    private boolean total;

}
