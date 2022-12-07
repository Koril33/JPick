package cn.korilweb.controller;

import cn.korilweb.entity.TaskDTO;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.net.URL;
import java.nio.file.Path;
import java.util.ResourceBundle;

public class TaskController implements Initializable {

    @FXML
    private Button newTaskBtn;

    @FXML
    private Button openFolderBtn;

    @FXML
    private TextField taskUrlTextField;

    @FXML
    private TextField desPathTextField;

    @FXML
    private AnchorPane anchorPane;

    @FXML
    private IndexController indexController;

    @FXML
    private CheckBox onlyVideoCheckBox;

    @FXML
    private CheckBox onlyAudioCheckBox;

    @FXML
    private CheckBox totalCheckBox;


    @FXML
    void newTaskConfirm(ActionEvent event) {
        TaskDTO taskDTO = new TaskDTO();
        taskDTO.setSrcPathStr(taskUrlTextField.getText());
        taskDTO.setDesPathStr(desPathTextField.getText());

        taskDTO.setOnlyVideo(onlyVideoCheckBox.isSelected());
        taskDTO.setOnlyAudio(onlyAudioCheckBox.isSelected());
        taskDTO.setTotal(totalCheckBox.isSelected());


        Stage window = (Stage) anchorPane.getScene().getWindow();
        window.close();

        indexController.startDownload(taskDTO);
    }

    @FXML
    void openFolder(ActionEvent event) {
        var defaultDesPath = Path.of(
                System.getProperty("user.dir")
        ).toFile();

        var dirChoose = new DirectoryChooser();
        dirChoose.setTitle("下载路径");
        dirChoose.setInitialDirectory(
                // 默认是运行时的目录
                defaultDesPath
        );
        var selectedFolder= dirChoose.showDialog(anchorPane.getScene().getWindow());
        if (selectedFolder == null) {
            selectedFolder = defaultDesPath;
        }
        desPathTextField.setText(selectedFolder.toString());
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        indexController = MainController.getIndexController();
    }
}
