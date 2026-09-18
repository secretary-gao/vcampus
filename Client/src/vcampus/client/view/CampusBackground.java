/*
 * CampusBackground
 *
 * Version 1.1
 *
 * 2026-09-09
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.client.view;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * 登录窗口的东南大学校园背景轮播：一组真实校园照片（校门、图书馆、
 * 校园风光），每隔几秒交叉淡入淡出切换一次，上面盖一层半透明深色
 * 遮罩保证登录卡片和文字始终看得清楚。照片按 {@link SeuEmblem} 同样的
 * 约定放在项目相对路径下，找不到图片时静默跳过、退回纯色背景，不会
 * 导致程序报错。只挑了横版（landscape）照片放进轮播——竖版照片按"裁剪
 * 填满"缩放到宽屏窗口会需要放大好几倍，画面糊、取景也会显得很突兀。
 */
public final class CampusBackground {

    /** 轮播图片的 classpath 资源路径（打包进 jar 后也按这个路径读取，不依赖源码目录）。 */
    private static final String[] IMAGE_PATHS = {
            "/vcampus/client/view/assets/campus/campus1.jpg",
            "/vcampus/client/view/assets/campus/campus2.jpg",
            "/vcampus/client/view/assets/campus/campus3.jpg",
            "/vcampus/client/view/assets/campus/campus4.jpg",
    };

    /** 每张照片停留的时长。 */
    private static final Duration HOLD_DURATION = Duration.seconds(5);

    /** 交叉淡入淡出的过渡时长。 */
    private static final Duration FADE_DURATION = Duration.seconds(1.6);

    private CampusBackground() {
    }

    /**
     * 构建背景轮播节点：铺满父容器，随父容器尺寸变化自动按"裁剪填满"
     * （类似 CSS 的 {@code background-size: cover}）方式缩放，不会变形。
     *
     * @return 可以直接作为 {@code StackPane} 第一个子节点（最底层）的背景节点
     */
    public static Region build() {
        StackPane container = new StackPane();
        container.setAlignment(Pos.CENTER);

        List<Image> images = loadImages();
        if (images.isEmpty()) {
            // 一张图片都加载不到（比如资源被误删），静默退回透明，
            // LoginFrame 原有的渐变背景色会照常显示，不会白屏或报错。
            return container;
        }

        ImageView bottomView = new ImageView(images.get(0));
        ImageView topView = new ImageView();
        topView.setOpacity(0);
        configureCoverFit(bottomView, container);
        configureCoverFit(topView, container);

        Region scrim = new Region();
        scrim.setStyle("-fx-background-color: rgba(8,18,32,0.4);");

        container.getChildren().addAll(bottomView, topView, scrim);

        if (images.size() > 1) {
            int[] nextIndex = {1};
            Timeline timeline = new Timeline(new KeyFrame(HOLD_DURATION, event -> {
                Image next = images.get(nextIndex[0]);
                topView.setImage(next);
                topView.setOpacity(0);
                FadeTransition fade = new FadeTransition(FADE_DURATION, topView);
                fade.setFromValue(0);
                fade.setToValue(1);
                fade.setOnFinished(e -> {
                    bottomView.setImage(next);
                    topView.setOpacity(0);
                });
                fade.play();
                nextIndex[0] = (nextIndex[0] + 1) % images.size();
            }));
            timeline.setCycleCount(Timeline.INDEFINITE);
            timeline.play();
        }

        return container;
    }

    /**
     * 让一个 {@link ImageView} 按"裁剪填满"的方式跟随容器尺寸缩放：
     * 取宽高缩放比里较大的一个，保证图片完全覆盖容器且不变形，多出的
     * 部分自然超出容器边界（父容器是撑满整个窗口的根节点，超出部分不
     * 会显示在窗口外，等价于视觉上的裁剪）。
     *
     * <p>用 {@link Bindings#createDoubleBinding} 而不是手动
     * {@code addListener}：手动监听器只在属性"发生变化"时才触发，如果
     * 容器在监听器挂上之前就已经完成了第一次布局（时机竞争，跟机器
     * 快慢、图片加载速度有关），这次变化就会被错过，导致 fitWidth/
     * fitHeight 一直停在初始值 0，图片显示不出来——这正是"有时候没图片
     * 显示"的原因。而 {@code createDoubleBinding} 在绑定的那一刻就会
     * 用当前的真实值计算一次，不存在"错过初始值"的问题。</p>
     *
     * @param view      要设置的图片视图
     * @param container 参照的容器（一般是撑满整个窗口的根节点）
     */
    private static void configureCoverFit(ImageView view, StackPane container) {
        view.setPreserveRatio(true);
        view.setSmooth(true);

        DoubleBinding scale = Bindings.createDoubleBinding(() -> {
            Image image = view.getImage();
            double containerW = container.getWidth();
            double containerH = container.getHeight();
            if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0
                    || containerW <= 0 || containerH <= 0) {
                return 0.0;
            }
            return Math.max(containerW / image.getWidth(), containerH / image.getHeight());
        }, view.imageProperty(), container.widthProperty(), container.heightProperty());

        view.fitWidthProperty().bind(Bindings.createDoubleBinding(
                () -> view.getImage() == null ? 0.0 : view.getImage().getWidth() * scale.get(),
                scale, view.imageProperty()));
        view.fitHeightProperty().bind(Bindings.createDoubleBinding(
                () -> view.getImage() == null ? 0.0 : view.getImage().getHeight() * scale.get(),
                scale, view.imageProperty()));
    }

    /**
     * 依次尝试加载 {@link #IMAGE_PATHS} 里的每张图片，跳过找不到或加载
     * 失败的，不抛异常。
     *
     * @return 成功加载的图片列表，可能为空
     */
    private static List<Image> loadImages() {
        List<Image> images = new ArrayList<>();
        for (String path : IMAGE_PATHS) {
            URL url = CampusBackground.class.getResource(path);
            if (url == null) {
                continue;
            }
            try {
                // 同步加载（backgroundLoading=false），保证构造完成时 getWidth()/getHeight()
                // 就能拿到真实尺寸，不然裁剪填满的缩放计算会因为尺寸暂时是 0 而算出 NaN。
                Image image = new Image(url.toExternalForm(), 0, 0, true, true, false);
                if (!image.isError()) {
                    images.add(image);
                }
            } catch (Exception e) {
                // 单张图片加载失败不影响其它照片，跳过即可。
            }
        }
        return images;
    }
}
