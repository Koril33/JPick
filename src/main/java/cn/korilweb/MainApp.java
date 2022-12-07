package cn.korilweb;


import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

/**
 * @author DJH
 * @date 2022-11-23 18:34:52
 */
public class MainApp extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("controller/index.fxml"));

        Parent rootNode;
        rootNode = fxmlLoader.load();
        stage.setScene(new Scene(rootNode));
        stage.setTitle("JPick | 一个简单的b站下载器");
        stage.getIcons().add(
                new Image(
                    Objects.requireNonNull(
                            getClass().getResourceAsStream("icon.png")
                    )
                )
        );
        stage.show();

    }

}
