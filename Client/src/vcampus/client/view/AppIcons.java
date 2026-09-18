/*
 * AppIcons
 *
 * Version 1.0
 *
 * 2026-09-11
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.client.view;

import javafx.collections.ListChangeListener;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.net.URL;

/**
 * 给整个客户端进程里所有窗口（登录窗、主界面、各模块弹出的独立窗口、
 * 以及 {@code Alert} 提示框内部用到的窗口）统一挂上东南大学校徽图标，
 * 替换掉 Windows 默认的 Java 咖啡杯图标。
 *
 * <p>实现方式：监听全局的 {@link Window#getWindows()} 列表，只要有新
 * 窗口出现就自动给它挂图标——这样不用去每个模块、每处 {@code new
 * Alert(...)} 的地方分别加代码（那样要改的文件太多，也容易漏），只需要
 * 在整个客户端最早的入口（{@link LoginFrame#start}）调用一次
 * {@link #installGlobalIcon()} 就能覆盖后续打开的所有窗口。</p>
 */
public final class AppIcons {

    /** 校徽图片相对项目根目录的路径，跟 {@link SeuEmblem} 用的是同一张图。 */
    private static final String ICON_PATH = "/vcampus/client/view/assets/seu-emblem.png";

    /** 缓存加载好的图标，避免重复读取文件。 */
    private static Image icon;

    /** 保证全局监听器只安装一次。 */
    private static boolean installed = false;

    private AppIcons() {
    }

    /**
     * 安装全局图标监听器（只在第一次调用时真正生效，重复调用是安全的）。
     * 找不到图标文件时静默跳过，不会影响程序正常运行。
     */
    public static synchronized void installGlobalIcon() {
        if (installed) {
            return;
        }
        installed = true;

        icon = loadIcon();
        if (icon == null) {
            return;
        }

        // 给已经存在的窗口（比如调用时机稍晚，已经有窗口打开了）先补上图标。
        for (Window window : Window.getWindows()) {
            applyIcon(window);
        }

        // 监听后续新打开的每一个窗口。
        Window.getWindows().addListener((ListChangeListener<Window>) change -> {
            while (change.next()) {
                if (!change.wasAdded()) {
                    continue;
                }
                for (Window window : change.getAddedSubList()) {
                    applyIcon(window);
                }
            }
        });
    }

    /**
     * 给一个窗口挂上图标（只处理 {@link Stage}，且该窗口还没有自己设置
     * 过图标时才挂，避免覆盖某个窗口特意设置的其它图标）。
     *
     * @param window 待处理的窗口
     */
    private static void applyIcon(Window window) {
        if (window instanceof Stage stage && stage.getIcons().isEmpty()) {
            stage.getIcons().add(icon);
        }
    }

    /**
     * 尝试从 {@link #ICON_PATH} 加载图标图片。
     *
     * @return 加载成功返回图片；文件不存在或加载失败返回 {@code null}
     */
    private static Image loadIcon() {
        URL url = AppIcons.class.getResource(ICON_PATH);
        if (url == null) {
            return null;
        }
        try {
            Image image = new Image(url.toExternalForm());
            return image.isError() ? null : image;
        } catch (Exception e) {
            return null;
        }
    }
}
