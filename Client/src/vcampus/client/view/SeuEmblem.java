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
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.control.Label;

/**
 * 东南大学校徽的简化矢量画法：深绿底 + 金色双圈，中央用几何图形拼出
 * 一个简化的"校门/牌坊"剪影，下方叠一行小字"东南"。不依赖任何外部
 * 图片资源，纯 JavaFX 形状绘制，可按 {@code diameter} 等比缩放，
 * 供登录窗口的大号品牌区和主界面顶部导航栏的小号图标共用。
 */
public final class SeuEmblem {

    private SeuEmblem() {
    }

    /**
     * 按指定直径绘制校徽。
     *
     * @param diameter 校徽外圆直径（像素）
     * @return 可直接放入布局的校徽节点
     */
    public static StackPane build(double diameter) {
        double r = diameter / 2.0;

        StackPane badge = new StackPane();
        badge.setPrefSize(diameter, diameter);
        badge.setMinSize(diameter, diameter);
        badge.setMaxSize(diameter, diameter);
        badge.setAlignment(Pos.CENTER);

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

        badge.getChildren().addAll(outerRing, innerRing, leftPillar, rightPillar, lintel, roof, seal);
        return badge;
    }
}
