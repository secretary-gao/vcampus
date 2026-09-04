# assets

存放客户端界面用到的图片资源，纯静态文件，不参与编译。

- `seu-emblem.png` —— 东南大学校徽。建议用背景干净、无水印的方形版本
  （圆形校徽居中，四周留白或透明皆可）。[SeuEmblem.java](../SeuEmblem.java)
  运行时会按相对路径 `Client/src/vcampus/client/view/assets/seu-emblem.png`
  加载这张图；找不到文件时会自动回退成手绘的矢量校徽，不会导致程序崩溃。
