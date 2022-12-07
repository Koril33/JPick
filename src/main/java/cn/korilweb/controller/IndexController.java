package cn.korilweb.controller;

import cn.korilweb.entity.DownloadInfoDTO;
import cn.korilweb.entity.TaskDTO;
import cn.korilweb.parser.BilibiliParser;
import cn.korilweb.task.DownloadTask;
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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;


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
        HBox hBox = (HBox) pane.getChildren().get(1);
        Label label = (Label) hBox.getChildren().get(0);
        ProgressBar bar = (ProgressBar) pane.getChildren().get(0);
        stack.getChildren().add(pane);

        downloadBiliBiliVideo(label, bar, dto);

    }

    private void downloadBiliBiliVideo(Label label, ProgressBar bar, TaskDTO taskDTO) {

        DownloadInfoDTO downloadInfoDTO = null;
        try {
            downloadInfoDTO = BilibiliParser.getDownloadInfoDTO(taskDTO.getSrcPathStr());
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (downloadInfoDTO != null) {
            Task<Void> downloadTask = new DownloadTask(downloadInfoDTO, taskDTO.getDesPathStr());

            label.setText(downloadInfoDTO.getTitle());
            bar.progressProperty().bind(downloadTask.progressProperty());

            Thread downloadThread = new Thread(downloadTask);
            downloadThread.setDaemon(true);
            downloadThread.start();
        }
    }
}
