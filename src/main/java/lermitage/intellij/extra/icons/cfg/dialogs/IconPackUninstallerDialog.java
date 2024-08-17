// SPDX-License-Identifier: MIT

package lermitage.intellij.extra.icons.cfg.dialogs;

import com.intellij.openapi.ui.DialogWrapper;
import lermitage.intellij.extra.icons.Model;
import lermitage.intellij.extra.icons.utils.I18nUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A dialog which asks to choose an icon pack name, or nothing.
 */
public class IconPackUninstallerDialog extends DialogWrapper {

    private final List<Model> models;

    private JPanel pane;
    private JLabel iconPackChooserTitleLabel;
    private JComboBox<String> iconPackComboBox;

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return this.pane;
    }

    public IconPackUninstallerDialog(List<Model> models) {
        super(true);
        this.models = models;
        init();
        setTitle(I18nUtils.RESOURCE_BUNDLE.getString("model.dialog.uninstall.icon.pack.window.title"));
        initComponents();
    }

    private void initComponents() {
        this.iconPackChooserTitleLabel.setText(I18nUtils.RESOURCE_BUNDLE.getString("model.dialog.uninstall.icon.pack.title"));
        Set<String> iconPacks = new HashSet<>();
        this.models.forEach(model -> {
            if (model.getIconPack() != null && !model.getIconPack().isBlank()) {
                iconPacks.add(model.getIconPack());
            }
        });
        iconPacks.stream().sorted().forEach(s -> this.iconPackComboBox.addItem(s));
    }

    public String getIconPackNameFromInput() {
        return this.iconPackComboBox.getItemAt(this.iconPackComboBox.getSelectedIndex());
    }
}
