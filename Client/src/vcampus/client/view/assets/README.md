# assets

存放客户端界面用到的图片资源，纯静态文件，不参与编译。

- `seu-emblem.png` —— 东南大学校徽，透明背景的圆形裁切图。
  [SeuEmblem.java](../SeuEmblem.java) 运行时按相对路径
  `Client/src/vcampus/client/view/assets/seu-emblem.png` 加载这张图；
  文件缺失或加载失败时会自动回退成手绘的矢量校徽，不会导致程序崩溃。
