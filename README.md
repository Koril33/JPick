# JPick | A simple bilibili video downloader
JPick | 一个简单的 Bilibili 视频下载器
---

## 简介
该项目简单的尝试了下 JavaFX 的技术，配合对 Bilibili 的视频真实路径的解析，实现了
一个 B 站下载器的 GUI 版本。

---

## 技术
1. GUI 框架：JavaFX https://openjfx.io/index.html
2. HTML 解析: Jsoup https://jsoup.org/
3. JSON 解析: Jackson https://github.com/FasterXML/jackson
4. 视频处理: FFmpeg https://ffmpeg.org/

## TODO
1. Bilibili 视频地址代码解析的分离。
2. 多线程下载多视频文件。
3. 加入选项卡和菜单栏。
4. 改成 MVC 架构，分离 UI 代码。
