package vcampus.client.view;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Dialog;

/** Shared Course-inspired visual theme for application windows and dialogs. */
public final class CampusTheme {
    private CampusTheme() { }

    /** Apply once; module styles remain responsible for domain-specific states. */
    public static void apply(Parent root) {
        String css = CampusTheme.class.getResource("campus-theme.css").toExternalForm();
        if (!root.getStylesheets().contains(css)) root.getStylesheets().add(css);
    }

    public static void apply(Scene scene) { apply(scene.getRoot()); }

    public static void apply(Dialog<?> dialog) { apply(dialog.getDialogPane()); }
}
