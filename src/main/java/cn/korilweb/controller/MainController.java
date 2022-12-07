package cn.korilweb.controller;

public class MainController {

    private static IndexController indexController;

    public static IndexController getIndexController() {
        return indexController;
    }

    public static void setIndexController(IndexController controller) {
        indexController = controller;
    }
}
