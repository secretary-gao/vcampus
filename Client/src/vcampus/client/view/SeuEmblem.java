/*
 * SeuEmblem
 *
 * Version 1.0
 *
 * 2026-09-04
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.client.view;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.io.File;
import java.util.List;

/**
 * 东南大学校徽的绘制工具类。优先加载 {@code assets/seu-emblem.png} 这张
 * 真实校徽图片（圆形裁切后按 {@code diameter} 等比缩放）；找不到图片文件
 * 时，自动回退成纯 JavaFX 形状拼出的简化版（深绿底 + 金色双圈 + 简化的
 * "校门/牌坊"剪影 + "东南"字样），保证界面在没有图片资源时也不会出错。
 * 供登录窗口的大号品牌区和主界面顶部导航栏的小号图标共用。
 */
public final class SeuEmblem {

    /** 校徽图片相对项目根目录的路径；各启动方式（.bat 脚本、IDE launch.json）都以项目根目录为工作目录，可直接用相对路径定位。 */
    private static final String IMAGE_PATH = "Client/src/vcampus/client/view/assets/seu-emblem.png";

    private SeuEmblem() {
    }

    /**
     * 按指定直径绘制校徽：优先用真实校徽图片，没有图片就退回矢量画法。
     *
     * @param diameter 校徽外圆直径（像素）
     * @return 可直接放入布局的校徽节点
     */
    public static StackPane build(double diameter) {
        StackPane badge = new StackPane();
        badge.setPrefSize(diameter, diameter);
        badge.setMinSize(diameter, diameter);
        badge.setMaxSize(diameter, diameter);
        badge.setAlignment(Pos.CENTER);

        ImageView imageView = tryLoadImage(diameter);
        if (imageView != null) {
            badge.getChildren().add(imageView);
        } else {
            badge.getChildren().addAll(buildVectorShapes(diameter));
        }
        return badge;
    }

    /**
     * 尝试从 {@link #IMAGE_PATH} 加载真实校徽图片，裁成圆形并缩放到指定直径。
     *
     * @param diameter 目标直径
     * @return 加载成功时返回可用的 {@link ImageView}；文件不存在或加载失败时返回 {@code null}
     */
    private static ImageView tryLoadImage(double diameter) {
        File file = new File(IMAGE_PATH);
        if (!file.isFile()) {
            return null;
        }
        try {
            Image image = new Image(file.toURI().toString(), diameter, diameter, false, true, true);
            if (image.isError()) {
                return null;
            }
            ImageView view = new ImageView(image);
            view.setFitWidth(diameter);
            view.setFitHeight(diameter);
            view.setPreserveRatio(false);
            view.setSmooth(true);
            view.setClip(new Circle(diameter / 2.0, diameter / 2.0, diameter / 2.0));
            return view;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 没有图片资源时的兜底方案：纯矢量画出简化校徽。
     *
     * @param diameter 目标直径
     * @return 组成校徽图案的节点列表
     */
    private static List<Node> buildVectorShapes(double diameter) {
        double r = diameter / 2.0;

        Circle outerRing = new Circle(r);
        outerRing.setFill(Color.web("#0c5a3e"));
        outerRing.setStroke(Color.web("#d9b355"));
        outerRing.setStrokeWidth(Math.max(1.4, diameter * 0.035));

        Circle innerRing = new Circle(r - Math.max(3, diameter * 0.09));
        innerRing.setFill(Color.TRANSPARENT);
        innerRing.setStroke(Color.web("#d9b355"));
        innerRing.setStrokeWidth(Math.max(1, diameter * 0.018));

        // 简化的校门/牌坊剪影：两根立柱 + 横梁 + 三角小顶。
        double pillarW = diameter * 0.075;
        double pillarH = diameter * 0.30;
        double gap = diameter * 0.18;

        Rectangle leftPillar = new Rectangle(pillarW, pillarH, Color.web("#f4e2ab"));
        leftPillar.setTranslateX(-gap / 2 - pillarW / 2);
        leftPillar.setTranslateY(diameter * 0.06);

        Rectangle rightPillar = new Rectangle(pillarW, pillarH, Color.web("#f4e2ab"));
        rightPillar.setTranslateX(gap / 2 + pillarW / 2);
        rightPillar.setTranslateY(diameter * 0.06);

        Rectangle lintel = new Rectangle(gap + pillarW * 2.4, diameter * 0.055, Color.web("#f4e2ab"));
        lintel.setTranslateY(diameter * 0.06 - pillarH / 2 - diameter * 0.02);

        Polygon roof = new Polygon(
                -(gap / 2 + pillarW * 1.6), 0.0,
                (gap / 2 + pillarW * 1.6), 0.0,
                0.0, -diameter * 0.12);
        roof.setFill(Color.web("#f4e2ab"));
        roof.setTranslateY(diameter * 0.06 - pillarH / 2 - diameter * 0.045);

        Label seal = new Label("东南");
        seal.setTextFill(Color.web("#f4e2ab"));
        seal.setFont(Font.font("System", FontWeight.BOLD, Math.max(9, diameter * 0.16)));
        seal.setTranslateY(diameter * 0.30);

        return List.of(outerRing, innerRing, leftPillar, rightPillar, lintel, roof, seal);
    }
}
