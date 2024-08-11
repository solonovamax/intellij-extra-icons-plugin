// SPDX-License-Identifier: MIT

package lermitage.intellij.extra.icons.cfg;

import com.google.common.base.Strings;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.ComboboxSpeedSearch;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.table.JBTable;
import com.intellij.util.system.OS;
import lermitage.intellij.extra.icons.Model;
import lermitage.intellij.extra.icons.ModelTag;
import lermitage.intellij.extra.icons.ModelType;
import lermitage.intellij.extra.icons.UITypeIconsPreference;
import lermitage.intellij.extra.icons.cfg.dialogs.AskSingleTextDialog;
import lermitage.intellij.extra.icons.cfg.dialogs.IconPackUninstallerDialog;
import lermitage.intellij.extra.icons.cfg.dialogs.ModelDialog;
import lermitage.intellij.extra.icons.cfg.models.PluginIconsSettingsTableModel;
import lermitage.intellij.extra.icons.cfg.models.UserIconsSettingsTableModel;
import lermitage.intellij.extra.icons.cfg.services.SettingsIDEService;
import lermitage.intellij.extra.icons.cfg.services.SettingsProjectService;
import lermitage.intellij.extra.icons.cfg.services.SettingsService;
import lermitage.intellij.extra.icons.lic.ExtraIconsLicenseStatus;
import lermitage.intellij.extra.icons.messaging.RefreshIconsNotifierService;
import lermitage.intellij.extra.icons.utils.ComboBoxWithImageItem;
import lermitage.intellij.extra.icons.utils.ComboBoxWithImageRenderer;
import lermitage.intellij.extra.icons.utils.FileChooserUtils;
import lermitage.intellij.extra.icons.utils.I18nUtils;
import lermitage.intellij.extra.icons.utils.IconPackUtils;
import lermitage.intellij.extra.icons.utils.IconUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.table.TableStringConverter;
import java.awt.event.ItemEvent;
import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class SettingsForm implements Configurable, Configurable.NoScroll {

    private static final @NonNls Logger LOGGER = Logger.getInstance(SettingsForm.class);

    private JPanel pane;
    private JButton buttonEnableAll;
    private JButton buttonDisableAll;
    private JCheckBox overrideSettingsCheckbox;
    private JTextField ignoredPatternTextField;
    private JBLabel ignoredPatternTitle;
    private JTabbedPane iconsTabbedPane;
    private JBTable pluginIconsTable;
    private JPanel userIconsTablePanel;
    private final JBTable userIconsTable = new JBTable();
    private JPanel overrideSettingsPanel;
    private JCheckBox addToIDEUserIconsCheckbox;
    private JLabel filterLabel;
    private JTextField filterTextField;
    private JButton filterResetBtn;
    private JBLabel bottomTip;
    private JLabel additionalUIScaleTitle;
    private JTextField additionalUIScaleTextField;
    private JComboBox<ComboBoxWithImageItem> comboBoxIconsGroupSelector;
    private JLabel disableOrEnableOrLabel;
    private JLabel disableOrEnableLabel;
    private JButton buttonReloadProjectsIcons;
    private JLabel iconPackLabel;
    private JButton buttonImportIconPackFromFile;
    private JButton buttonExportUserIconsAsIconPack;
    private JButton buttonUninstallIconPack;
    private JLabel iconPackContextHelpLabel;
    private JButton buttonShowIconPacksFromWeb;
    private JPanel iconPackPanel;
    private JComboBox<ComboBoxWithImageItem> uiTypeSelector;
    private JLabel uiTypeSelectorTitle;
    private JLabel uiTypeSelectorHelpLabel;
    private JTabbedPane mainTabbedPane;
    private JPanel experimentalPanel;
    private JCheckBox useIDEFilenameIndexCheckbox;
    private JBLabel useIDEFilenameIndexTip;
    private JButton detectAdditionalUIScaleButton;
    private JLabel resetHintsTitle;
    private JButton resetHintsButton;
    private JButton buttonKnownIssue1;
    private JButton buttonKnownIssue2;
    private JLabel labelKnownIssue1;
    private JLabel labelKnownIssue2;
    private JLabel labelKnownIssueTitle;
    private JLabel labelKnownIssue3;
    private JButton buttonKnownIssue3;
    private JLabel licenseMissingLabel;

    private PluginIconsSettingsTableModel pluginIconsSettingsTableModel;
    private UserIconsSettingsTableModel userIconsSettingsTableModel;
    private @Nullable Project project;
    private List<Model> customModels = new ArrayList<>();

    private boolean forceUpdate = false;

    private static final ResourceBundle i18n = I18nUtils.getResourceBundle();

    public SettingsForm() {
        this.buttonEnableAll.addActionListener(e -> enableAll(true));
        this.buttonDisableAll.addActionListener(e -> enableAll(false));
        this.filterResetBtn.addActionListener(e -> resetFilter());
        this.filterTextField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                applyFilter();
            }
        });
        this.buttonReloadProjectsIcons.addActionListener(al -> {
            try {
                RefreshIconsNotifierService.getInstance().triggerAllIconsRefreshAndIconEnablersReinit();
                Messages.showInfoMessage(
                    i18n.getString("icons.reloaded"),
                    i18n.getString("icons.reloaded.title")
                );
            } catch (Exception e) {
                LOGGER.warn("Config updated, but failed to reload icons for project", e);
                Messages.showErrorDialog(
                    i18n.getString("icons.failed.to.reload"),
                    i18n.getString("icons.failed.to.reload.title")
                );
            }
        });
        this.buttonImportIconPackFromFile.addActionListener(al -> {
            try {
                Optional<String> iconPackPath = FileChooserUtils.chooseFile(i18n.getString("dialog.import.icon.pack.title"),
                    this.pane, "*.json", "json"); //NON-NLS
                if (iconPackPath.isPresent()) {
                    IconPack iconPack = IconPackUtils.fromJsonFile(new File(iconPackPath.get()));
                    for (Model model : iconPack.getModels()) {
                        if (iconPack.getName() != null && !iconPack.getName().isBlank()) {
                            model.setIconPack(iconPack.getName());
                        }
                        this.customModels.add(model);
                    }
                    foldersFirst(this.customModels);
                    setUserIconsTableModel();
                    apply();
                    Messages.showInfoMessage(
                        i18n.getString("dialog.import.icon.pack.success"),
                        i18n.getString("dialog.import.icon.pack.success.title")
                    );
                }
            } catch (Exception e) {
                LOGGER.error("Failed to import Icon Pack", e); // TODO replace by error dialog
            }
        });
        this.buttonShowIconPacksFromWeb.addActionListener(al ->
            BrowserUtil.browse("https://github.com/jonathanlermitage/intellij-extra-icons-plugin/blob/master/themes/THEMES.md#downloadable-icon-packs"));
        this.buttonExportUserIconsAsIconPack.addActionListener(al -> {
            try {
                Optional<String> folderPath = FileChooserUtils.chooseFolder(i18n.getString("dialog.export.icon.pack.title"), this.pane);
                if (folderPath.isPresent()) {
                    String filename = "extra-icons-" + System.currentTimeMillis() + "-icon-pack.json"; //NON-NLS
                    AskSingleTextDialog askSingleTextDialog = new AskSingleTextDialog( // TODO replace by Messages.showInputDialog
                        i18n.getString("dialog.export.ask.icon.pack.name.window.title"),
                        i18n.getString("dialog.export.ask.icon.pack.name.title"));
                    String iconPackName = "";
                    if (askSingleTextDialog.showAndGet()) {
                        iconPackName = askSingleTextDialog.getTextFromInput();
                    }
                    File exportFile = new File(folderPath.get() + "/" + filename);
                    IconPackUtils.writeToJsonFile(exportFile, new IconPack(iconPackName, getBestSettingsService(this.project).getCustomModels()));
                    Messages.showInfoMessage(
                        i18n.getString("dialog.export.icon.pack.success") + "\n" + exportFile.getAbsolutePath(),
                        i18n.getString("dialog.export.icon.pack.success.title")
                    );
                }
            } catch (Exception e) {
                LOGGER.error("Failed to export user icons", e); // TODO replace by error dialog
            }
        });
        this.buttonUninstallIconPack.addActionListener(al -> {
            try {
                apply();
                IconPackUninstallerDialog iconPackUninstallerDialog = new IconPackUninstallerDialog(this.customModels);
                if (iconPackUninstallerDialog.showAndGet()) {
                    String iconPackToUninstall = iconPackUninstallerDialog.getIconPackNameFromInput();
                    if (!iconPackToUninstall.isBlank()) {
                        this.customModels = this.customModels.stream()
                            .filter(model -> !iconPackToUninstall.equals(model.getIconPack()))
                            .collect(Collectors.toList());
                        foldersFirst(this.customModels);
                        setUserIconsTableModel();
                        apply();
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Failed to uninstall Icon Pack", e); // TODO replace by error dialog
            }
        });
        this.detectAdditionalUIScaleButton.addActionListener(al -> {
            String uiScale = Double.toString(SettingsService.DEFAULT_ADDITIONAL_UI_SCALE);
            this.additionalUIScaleTextField.setText(uiScale);
            this.additionalUIScaleTextField.grabFocus();
            Messages.showInfoMessage(
                MessageFormat.format(i18n.getString("btn.scalefactor.detect.infomessage.message"), uiScale),
                i18n.getString("btn.scalefactor.detect.infomessage.title"));
        });
        this.resetHintsButton.addActionListener(al -> {
            SettingsIDEService settingsIDEService = SettingsIDEService.getInstance();
            settingsIDEService.setIconviewerShouldRenderSVGHintNotifDisplayed(false);
            settingsIDEService.setPluginIsConfigurableHintNotifDisplayed(false);
            settingsIDEService.setLifetimeLicIntroHintNotifDisplayed(false);
            this.resetHintsButton.setEnabled(false);
            Messages.showInfoMessage(
                i18n.getString("reset.hints.success.subtitle"),
                i18n.getString("reset.hints.success.title")
            );
        });
    }

    public SettingsForm(@NotNull Project project) {
        this();
        this.project = project;
    }

    public boolean isProjectForm() {
        return this.project != null;
    }

    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) String getDisplayName() {
        return "Extra Icons"; //NON-NLS
    }

    @Override
    public @Nullable String getHelpTopic() {
        return null;
    }

    @Override
    public @Nullable JComponent createComponent() {
        initComponents();
        return this.pane;
    }

    @Override
    public boolean isModified() {
        if (this.forceUpdate) {
            return true;
        }

        SettingsService bestSettingsService = getBestSettingsService(this.project);
        SettingsIDEService settingsIDEService = SettingsIDEService.getInstance();

        if (isProjectForm()) {
            //noinspection DataFlowIssue
            SettingsProjectService projectService = SettingsProjectService.getInstance(this.project);
            if (projectService.isOverrideIDESettings() != this.overrideSettingsCheckbox.isSelected()) {
                return true;
            }
            if (projectService.isAddToIDEUserIcons() != this.addToIDEUserIconsCheckbox.isSelected()) {
                return true;
            }
        }
        if (!CollectionUtils.isEqualCollection(collectDisabledModelIds(), bestSettingsService.getDisabledModelIds())) {
            return true;
        }
        if (!CollectionUtils.isEqualCollection(this.customModels, bestSettingsService.getCustomModels())) {
            return true;
        }
        if (!CollectionUtils.isEqualCollection(this.customModels.stream().map(Model::isEnabled).collect(Collectors.toList()), collectUserIconEnabledStates())) {
            return true;
        }
        if (settingsIDEService.getUiTypeIconsPreference() != getSelectedUITypeIconsPreference()) {
            return true;
        }
        if (!bestSettingsService.getIgnoredPattern().equals(this.ignoredPatternTextField.getText())) {
            return true;
        }
        if (settingsIDEService.getUseIDEFilenameIndex2() != this.useIDEFilenameIndexCheckbox.isSelected()) {
            return true;
        }
        return !this.ignoredPatternTextField.getText().equals(bestSettingsService.getIgnoredPattern())
            || !this.additionalUIScaleTextField.getText().equals(Double.toString(settingsIDEService.getAdditionalUIScale2()));
    }

    private List<String> collectDisabledModelIds() {
        if (this.pluginIconsSettingsTableModel == null) {
            return Collections.emptyList();
        }

        List<String> disabledModelIds = new ArrayList<>();
        for (int settingsTableRow = 0; settingsTableRow < this.pluginIconsSettingsTableModel.getRowCount(); settingsTableRow++) {
            boolean iconEnabled = (boolean) this.pluginIconsSettingsTableModel.getValueAt(settingsTableRow, PluginIconsSettingsTableModel.ICON_ENABLED_COL_NUMBER);
            if (!iconEnabled) {
                disabledModelIds.add((String) this.pluginIconsSettingsTableModel.getValueAt(settingsTableRow, PluginIconsSettingsTableModel.ICON_ID_COL_NUMBER));
            }
        }
        return disabledModelIds;
    }

    private List<Boolean> collectUserIconEnabledStates() {
        if (this.userIconsSettingsTableModel == null) {
            return Collections.emptyList();
        }

        return IntStream.range(0, this.userIconsSettingsTableModel.getRowCount()).mapToObj(
            index -> ((boolean) this.userIconsSettingsTableModel.getValueAt(index, UserIconsSettingsTableModel.ICON_ENABLED_COL_NUMBER))
        ).collect(Collectors.toList());
    }

    private UITypeIconsPreference getSelectedUITypeIconsPreference() {
        int selectedIndex = this.uiTypeSelector.getSelectedIndex();
        if (selectedIndex == 0) {
            return UITypeIconsPreference.BASED_ON_ACTIVE_UI_TYPE;
        } else if (selectedIndex == 1) {
            return UITypeIconsPreference.PREFER_OLD_UI_ICONS;
        } else {
            return UITypeIconsPreference.PREFER_NEW_UI_ICONS;
        }
    }

    private void setSelectedUITypeIconsPreference(UITypeIconsPreference uiTypeIconsPreference) {
        switch (uiTypeIconsPreference) {
            case BASED_ON_ACTIVE_UI_TYPE -> this.uiTypeSelector.setSelectedIndex(0);
            case PREFER_OLD_UI_ICONS -> this.uiTypeSelector.setSelectedIndex(1);
            case PREFER_NEW_UI_ICONS -> this.uiTypeSelector.setSelectedIndex(2);
        }
    }

    @Override
    public void apply() {
        SettingsService bestSettingsService = getBestSettingsService(this.project);
        SettingsIDEService settingsIDEService = SettingsIDEService.getInstance();

        if (isProjectForm()) {
            //noinspection DataFlowIssue
            SettingsProjectService projectService = SettingsProjectService.getInstance(this.project);
            projectService.setOverrideIDESettings(this.overrideSettingsCheckbox.isSelected());
            projectService.setAddToIDEUserIcons(this.addToIDEUserIconsCheckbox.isSelected());
        }
        settingsIDEService.setUseIDEFilenameIndex2(this.useIDEFilenameIndexCheckbox.isSelected());
        settingsIDEService.setUiTypeIconsPreference(getSelectedUITypeIconsPreference());
        bestSettingsService.setDisabledModelIds(collectDisabledModelIds());
        bestSettingsService.setIgnoredPattern(this.ignoredPatternTextField.getText());
        try {
            settingsIDEService.setAdditionalUIScale2(Double.valueOf(this.additionalUIScaleTextField.getText()));
        } catch (NumberFormatException e) {
            Messages.showErrorDialog(
                MessageFormat.format(i18n.getString("invalid.ui.scalefactor"), this.additionalUIScaleTextField.getText()),
                i18n.getString("invalid.ui.scalefactor.title")
            );
        }
        List<Boolean> enabledStates = collectUserIconEnabledStates();
        for (int i = 0; i < this.customModels.size(); i++) {
            Model model = this.customModels.get(i);
            model.setEnabled(enabledStates.get(i));
        }
        bestSettingsService.setCustomModels(this.customModels);

        try {
            if (isProjectForm()) {
                RefreshIconsNotifierService.getInstance().triggerProjectIconEnablersReinit(this.project);
                RefreshIconsNotifierService.getInstance().triggerProjectIconsRefresh(this.project);
            } else {
                RefreshIconsNotifierService.getInstance().triggerAllIconsRefreshAndIconEnablersReinit();
            }
        } catch (Exception e) {
            LOGGER.warn("Config updated, but failed to reload icons", e);
        }

        this.forceUpdate = false;
    }

    public @Nullable Project getProject() {
        return this.project;
    }

    private void initComponents() {
        this.licenseMissingLabel.setText(i18n.getString("license.not.found.config.title"));
        this.licenseMissingLabel.setVisible(!ExtraIconsLicenseStatus.isLicenseActivated());

        this.uiTypeSelector.setRenderer(new ComboBoxWithImageRenderer());
        this.uiTypeSelector.addItem(new ComboBoxWithImageItem(
            "extra-icons/plugin-internals/auto.svg", //NON-NLS
            i18n.getString("uitype.selector.auto.select")));
        this.uiTypeSelector.addItem(new ComboBoxWithImageItem(
            "extra-icons/plugin-internals/folder_oldui.svg",//NON-NLS
            i18n.getString("uitype.selector.prefer.old")));
        this.uiTypeSelector.addItem(new ComboBoxWithImageItem(
            "extra-icons/plugin-internals/folder_newui.svg", //NON-NLS
            i18n.getString("uitype.selector.prefer.new")));
        setSelectedUITypeIconsPreference(SettingsIDEService.getInstance().getUiTypeIconsPreference());

        this.disableOrEnableLabel.setText(i18n.getString("quick.action.label"));

        this.buttonEnableAll.setText(i18n.getString("btn.enable.all"));
        this.buttonEnableAll.setIcon(IconLoader.getIcon("extra-icons/plugin-internals/gutterCheckBoxSelected.svg", SettingsForm.class)); //NON-NLS

        this.buttonDisableAll.setText(i18n.getString("btn.disable.all"));
        this.buttonDisableAll.setIcon(IconLoader.getIcon("extra-icons/plugin-internals/gutterCheckBox.svg", SettingsForm.class)); //NON-NLS

        this.ignoredPatternTitle.setText(i18n.getString("label.regex.ignore.relative.paths"));
        this.ignoredPatternTextField.setToolTipText(i18n.getString("field.regex.ignore.relative.paths"));
        this.additionalUIScaleTitle.setText(i18n.getString("label.ui.scalefactor"));
        this.additionalUIScaleTextField.setToolTipText(i18n.getString("field.ui.scalefactor"));
        this.detectAdditionalUIScaleButton.setText(i18n.getString("btn.scalefactor.detect"));
        this.additionalUIScaleTextField.setColumns(4);
        this.filterLabel.setText(i18n.getString("plugin.icons.table.filter"));
        this.filterTextField.setText("");
        this.filterTextField.setToolTipText(i18n.getString("plugin.icons.table.filter.tooltip"));
        this.filterResetBtn.setText(i18n.getString("btn.plugin.icons.table.filter.reset"));
        this.bottomTip.setText(i18n.getString("plugin.icons.table.bottom.tip"));
        this.resetHintsTitle.setText(i18n.getString("reset.hints.title"));
        this.resetHintsButton.setText(i18n.getString("reset.hints.btn"));

        initCheckbox();

        loadPluginIconsTable();
        //TableSpeedSearch.installOn(pluginIconsTable); // TODO install a SpeedSearch on icons table once 232 is the new IDE min version, and make it work (for now, the settings search field steals focus)

        this.userIconsTable.setShowHorizontalLines(false);
        this.userIconsTable.setShowVerticalLines(false);
        this.userIconsTable.setFocusable(false);
        this.userIconsTable.setRowSelectionAllowed(true);
        this.userIconsTablePanel.add(createToolbarDecorator());
        loadUserIconsTable();
        loadIgnoredPattern();
        loadAdditionalUIScale();

        if (isProjectForm()) {
            this.additionalUIScaleTitle.setVisible(false);
            this.additionalUIScaleTextField.setVisible(false);
            this.buttonReloadProjectsIcons.setVisible(false);
            this.iconPackPanel.setVisible(false);
            this.uiTypeSelector.setVisible(false);
            this.uiTypeSelectorTitle.setVisible(false);
            this.uiTypeSelectorHelpLabel.setVisible(false);
            this.experimentalPanel.setVisible(false);
            this.detectAdditionalUIScaleButton.setVisible(false);
            this.resetHintsTitle.setVisible(false);
            this.resetHintsButton.setVisible(false);
        }

        this.overrideSettingsCheckbox.setText(i18n.getString("checkbox.override.ide.settings"));
        this.overrideSettingsCheckbox.setToolTipText(i18n.getString("checkbox.override.ide.settings.tooltip"));
        this.overrideSettingsCheckbox.addItemListener(item -> {
            boolean enabled = item.getStateChange() == ItemEvent.SELECTED;
            setComponentState(enabled);
        });

        this.addToIDEUserIconsCheckbox.setText(i18n.getString("checkbox.dont.overwrite.ide.user.icons"));
        this.addToIDEUserIconsCheckbox.setToolTipText(i18n.getString("checkbox.dont.overwrite.ide.user.icons.tooltip"));

        this.buttonReloadProjectsIcons.setText(i18n.getString("btn.reload.project.icons"));
        this.buttonReloadProjectsIcons.setToolTipText(i18n.getString("btn.reload.project.icons.tooltip"));
        this.buttonReloadProjectsIcons.setIcon(IconLoader.getIcon("extra-icons/plugin-internals/refresh.svg", SettingsForm.class)); //NON-NLS

        this.comboBoxIconsGroupSelector.setRenderer(new ComboBoxWithImageRenderer());
        this.comboBoxIconsGroupSelector.addItem(new ComboBoxWithImageItem(i18n.getString("icons")));
        Arrays.stream(ModelTag.values()).forEach(modelTag -> this.comboBoxIconsGroupSelector.addItem(
            new ComboBoxWithImageItem(modelTag, MessageFormat.format(i18n.getString("icons.tag.name"), modelTag.getName()))
        ));
        ComboboxSpeedSearch.installSpeedSearch(this.comboBoxIconsGroupSelector, ComboBoxWithImageItem::getTitle);

        this.iconPackLabel.setText(i18n.getString("icon.pack.label"));

        this.buttonImportIconPackFromFile.setText(i18n.getString("btn.import.icon.pack.file"));
        this.buttonImportIconPackFromFile.setIcon(IconLoader.getIcon("extra-icons/plugin-internals/import.svg", SettingsForm.class)); //NON-NLS

        this.buttonShowIconPacksFromWeb.setText(i18n.getString("btn.import.icon.pack.web"));
        this.buttonShowIconPacksFromWeb.setIcon(IconLoader.getIcon("extra-icons/plugin-internals/web.svg", SettingsForm.class)); //NON-NLS
        this.buttonShowIconPacksFromWeb.setToolTipText(i18n.getString("btn.import.icon.pack.web.tooltip"));

        this.buttonExportUserIconsAsIconPack.setText(i18n.getString("btn.export.icon.pack"));
        this.buttonExportUserIconsAsIconPack.setIcon(IconLoader.getIcon("extra-icons/plugin-internals/export.svg", SettingsForm.class)); //NON-NLS

        this.iconPackContextHelpLabel.setText("");
        this.iconPackContextHelpLabel.setIcon(IconLoader.getIcon("extra-icons/plugin-internals/contextHelp.svg", SettingsForm.class)); //NON-NLS
        this.iconPackContextHelpLabel.setToolTipText(i18n.getString("icon.pack.context.help"));

        this.uiTypeSelectorTitle.setText(i18n.getString("uitype.selector.context.title"));
        this.uiTypeSelectorHelpLabel.setText("");
        this.uiTypeSelectorHelpLabel.setIcon(IconLoader.getIcon("extra-icons/plugin-internals/contextHelp.svg", SettingsForm.class)); //NON-NLS
        this.uiTypeSelectorHelpLabel.setToolTipText(i18n.getString("uitype.selector.context.help"));

        this.buttonUninstallIconPack.setText(i18n.getString("btn.uninstall.icon.pack"));
        this.buttonUninstallIconPack.setIcon(IconLoader.getIcon("extra-icons/plugin-internals/remove.svg", SettingsForm.class)); //NON-NLS

        this.mainTabbedPane.setTitleAt(0, " " + i18n.getString("main.pane.main.config.title") + " ");
        this.mainTabbedPane.setTitleAt(1, " " + i18n.getString("main.pane.advanced.config.title") + " ");
        this.mainTabbedPane.setTitleAt(2, " " + i18n.getString("main.pane.known.issues.title") + " ");

        this.iconsTabbedPane.setTitleAt(0, " " + i18n.getString("plugin.icons.table.tab.name") + " ");
        this.iconsTabbedPane.setTitleAt(1, " " + i18n.getString("user.icons.table.tab.name") + " ");

        this.experimentalPanel.setBorder(IdeBorderFactory.createTitledBorder(i18n.getString("experimental.panel.title")));

        this.useIDEFilenameIndexCheckbox.setSelected(SettingsIDEService.getInstance().getUseIDEFilenameIndex2());
        this.useIDEFilenameIndexCheckbox.setText(i18n.getString("checkbox.use.ide.filename.index.label"));
        this.useIDEFilenameIndexTip.setText(i18n.getString("checkbox.use.ide.filename.index.tip"));

        this.labelKnownIssueTitle.setText(i18n.getString("known.issues.title"));

        this.labelKnownIssue1.setText(i18n.getString("known.issue.label1"));
        this.labelKnownIssue2.setText(i18n.getString("known.issue.label2"));
        this.labelKnownIssue3.setText(i18n.getString("known.issue.label3"));
        this.buttonKnownIssue1.setText(i18n.getString("known.issue.btn1"));
        this.buttonKnownIssue2.setText(i18n.getString("known.issue.btn2"));
        this.buttonKnownIssue3.setText(i18n.getString("known.issue.btn3"));
        this.buttonKnownIssue1.setIcon(IconLoader.getIcon("extra-icons/plugin-internals/web.svg", SettingsForm.class)); //NON-NLS
        this.buttonKnownIssue2.setIcon(IconLoader.getIcon("extra-icons/plugin-internals/web.svg", SettingsForm.class)); //NON-NLS
        this.buttonKnownIssue3.setIcon(IconLoader.getIcon("extra-icons/plugin-internals/web.svg", SettingsForm.class)); //NON-NLS
        this.buttonKnownIssue1.addActionListener(al ->
            BrowserUtil.browse("https://youtrack.jetbrains.com/issue/IDEA-247819"));
        this.buttonKnownIssue2.addActionListener(al ->
            BrowserUtil.browse("https://youtrack.jetbrains.com/issue/IDEA-339254"));
        this.buttonKnownIssue3.addActionListener(al ->
            BrowserUtil.browse("https://youtrack.jetbrains.com/issue/RIDER-101621"));

        initCheckbox();
    }

    private void createUIComponents() {
        // Use default project here because project is not available yet
    }

    private void initCheckbox() {
        if (!isProjectForm()) {
            this.overrideSettingsPanel.setVisible(false);
            return;
        }
        //noinspection DataFlowIssue  project is not null here
        SettingsProjectService settingsService = SettingsProjectService.getInstance(this.project);
        boolean shouldOverride = settingsService.isOverrideIDESettings();
        this.overrideSettingsCheckbox.setSelected(shouldOverride);
        setComponentState(shouldOverride);
        boolean shouldAdd = settingsService.isAddToIDEUserIcons();
        this.addToIDEUserIconsCheckbox.setSelected(shouldAdd);
    }

    private void setComponentState(boolean enabled) {
        Stream.of(this.pluginIconsTable, this.userIconsTable, this.ignoredPatternTitle, this.ignoredPatternTextField,
            this.iconsTabbedPane, this.addToIDEUserIconsCheckbox, this.filterLabel,
            this.filterTextField, this.filterResetBtn, this.buttonEnableAll,
            this.disableOrEnableOrLabel, this.buttonDisableAll, this.disableOrEnableLabel,
            this.comboBoxIconsGroupSelector, this.mainTabbedPane).forEach(jComponent -> jComponent.setEnabled(enabled));
    }

    @Override
    public void reset() {
        initCheckbox();
        loadPluginIconsTable();
        loadUserIconsTable();
        loadIgnoredPattern();
        loadAdditionalUIScale();
    }

    private void loadUserIconsTable() {
        this.customModels = new ArrayList<>(getBestSettingsService(this.project).getCustomModels());
        foldersFirst(this.customModels);
        setUserIconsTableModel();
    }

    private void setUserIconsTableModel() {
        SettingsIDEService settingsIDEService = SettingsIDEService.getInstance();

        int currentSelected = this.userIconsSettingsTableModel != null ? this.userIconsTable.getSelectedRow() : -1;
        this.userIconsSettingsTableModel = new UserIconsSettingsTableModel();
        final Double additionalUIScale = settingsIDEService.getAdditionalUIScale2();
        final UITypeIconsPreference uiTypeIconsPreference = settingsIDEService.getUiTypeIconsPreference();
        this.customModels.forEach(m -> {
                try {
                    this.userIconsSettingsTableModel.addRow(new Object[]{
                        IconUtils.getIcon(m, additionalUIScale, uiTypeIconsPreference),
                        m.isEnabled(),
                        m.getDescription(),
                        m.getIconPack()
                    });
                } catch (Throwable e) {
                    LOGGER.warn(e);
                }
            }
        );
        this.userIconsTable.setModel(this.userIconsSettingsTableModel);
        this.userIconsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.userIconsTable.setRowHeight(28);
        TableColumnModel columnModel = this.userIconsTable.getColumnModel();
        columnModel.getColumn(UserIconsSettingsTableModel.ICON_COL_NUMBER).setMaxWidth(28);
        columnModel.getColumn(UserIconsSettingsTableModel.ICON_COL_NUMBER).setWidth(28);
        columnModel.getColumn(UserIconsSettingsTableModel.ICON_ENABLED_COL_NUMBER).setWidth(28);
        columnModel.getColumn(UserIconsSettingsTableModel.ICON_ENABLED_COL_NUMBER).setMaxWidth(28);
        columnModel.getColumn(UserIconsSettingsTableModel.ICON_LABEL_COL_NUMBER).sizeWidthToFit();
        columnModel.getColumn(UserIconsSettingsTableModel.ICON_PACK_COL_NUMBER).setMinWidth(250);
        columnModel.getColumn(UserIconsSettingsTableModel.ICON_PACK_COL_NUMBER).setMaxWidth(450);
        columnModel.getColumn(UserIconsSettingsTableModel.ICON_PACK_COL_NUMBER).setPreferredWidth(280);
        if (currentSelected != -1 && currentSelected < this.userIconsTable.getRowCount()) {
            this.userIconsTable.setRowSelectionInterval(currentSelected, currentSelected);
        }
    }

    /**
     * Get the selected tag for quick action. Empty if "all icons" is selected, otherwise returns selected tag.
     */
    private Optional<ModelTag> getSelectedTag() {
        int selectedItemIdx = this.comboBoxIconsGroupSelector.getSelectedIndex();
        if (selectedItemIdx == 0) {
            return Optional.empty();
        }
        return Optional.of(ModelTag.values()[selectedItemIdx - 1]);
    }

    private void enableAll(boolean enable) {
        boolean isPluginIconsSettingsTableModelSelected = this.iconsTabbedPane.getSelectedIndex() == 0;
        DefaultTableModel tableModel = isPluginIconsSettingsTableModelSelected ? this.pluginIconsSettingsTableModel : this.userIconsSettingsTableModel;
        for (int settingsTableRow = 0; settingsTableRow < tableModel.getRowCount(); settingsTableRow++) {
            Optional<ModelTag> selectedTag = getSelectedTag();
            if (selectedTag.isEmpty()) {
                tableModel.setValueAt(enable, settingsTableRow, PluginIconsSettingsTableModel.ICON_ENABLED_COL_NUMBER); // Enabled column number is the same for both models
            } else if (isPluginIconsSettingsTableModelSelected) {
                @SuppressWarnings("unchecked") List<ModelTag> rowTags = (List<ModelTag>) tableModel.getValueAt(settingsTableRow, PluginIconsSettingsTableModel.ICON_TAGS_ENUM_LIST_COL_NUMBER);
                if (rowTags.contains(selectedTag.get())) {
                    tableModel.setValueAt(enable, settingsTableRow, PluginIconsSettingsTableModel.ICON_ENABLED_COL_NUMBER);
                }
            }
        }
    }

    private void applyFilter() {
        String filter = this.filterTextField.getText();
        TableRowSorter<PluginIconsSettingsTableModel> sorter = new TableRowSorter<>(((PluginIconsSettingsTableModel) this.pluginIconsTable.getModel()));
        if (StringUtil.isEmpty(filter)) {
            filter = ".*";
        }
        try {
            sorter.setStringConverter(new TableStringConverter() {
                @Override
                public String toString(TableModel model, int row, int column) {
                    String desc = model.getValueAt(row, PluginIconsSettingsTableModel.ICON_LABEL_COL_NUMBER).toString();
                    return desc + " " + desc.toLowerCase(Locale.ENGLISH) + " " + desc.toUpperCase(Locale.ENGLISH);
                }
            });
            // "yes"/"no" to filter by icons enabled/disabled, otherwise regex filter
            boolean isYesFilter = "yes".equalsIgnoreCase(filter); //NON-NLS
            if (isYesFilter || "no".equalsIgnoreCase(filter)) { //NON-NLS
                sorter.setRowFilter(new RowFilter<>() {
                    @Override
                    public boolean include(Entry<? extends PluginIconsSettingsTableModel, ? extends Integer> entry) {
                        boolean iconEnabled = ((boolean) entry.getValue(PluginIconsSettingsTableModel.ICON_ENABLED_COL_NUMBER));
                        if (isYesFilter) {
                            return iconEnabled;
                        } else {
                            return !iconEnabled;
                        }
                    }
                });
            } else {
                sorter.setRowFilter(RowFilter.regexFilter(filter));
            }
            this.pluginIconsTable.setRowSorter(sorter);
        } catch (PatternSyntaxException pse) {
            LOGGER.warnWithDebug(pse);
        }
    }

    private void resetFilter() {
        this.filterTextField.setText("");
        applyFilter();
    }

    private void loadPluginIconsTable() {
        SettingsIDEService settingsIDEService = SettingsIDEService.getInstance();

        int currentSelected = this.pluginIconsSettingsTableModel != null ? this.pluginIconsTable.getSelectedRow() : -1;
        this.pluginIconsSettingsTableModel = new PluginIconsSettingsTableModel();
        List<Model> allRegisteredModels = SettingsService.getAllRegisteredModels();
        if (isProjectForm()) {
            // IDE icon overrides work at IDE level only, not a project level, that's why
            // the project-level icons list won't show IDE icons.
            allRegisteredModels = allRegisteredModels.stream()
                .filter(model -> model.getModelType() != ModelType.ICON)
                .collect(Collectors.toList());
        }
        foldersFirst(allRegisteredModels);
        List<String> disabledModelIds = getBestSettingsService(this.project).getDisabledModelIds();
        final Double additionalUIScale = settingsIDEService.getAdditionalUIScale2();
        final UITypeIconsPreference uiTypeIconsPreference = settingsIDEService.getUiTypeIconsPreference();
        final Icon restartIcon = IconLoader.getIcon("extra-icons/plugin-internals/reboot.svg", SettingsForm.class); //NON-NLS
        allRegisteredModels.forEach(m -> this.pluginIconsSettingsTableModel.addRow(new Object[]{
                IconUtils.getIcon(m, additionalUIScale, uiTypeIconsPreference),
                !disabledModelIds.contains(m.getId()),
                m.getDescription(),
                Arrays.toString(m.getTags().stream().map(ModelTag::getName).toArray()).replaceAll("\\[|]*", "").trim(),
                Strings.isNullOrEmpty(m.getIdeIcon()) ? null : restartIcon,
                m.getTags(),
                m.getId()
            })
        );
        this.pluginIconsTable.setModel(this.pluginIconsSettingsTableModel);
        this.pluginIconsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.pluginIconsTable.setRowHeight(28);
        TableColumnModel columnModel = this.pluginIconsTable.getColumnModel();
        columnModel.getColumn(PluginIconsSettingsTableModel.ICON_COL_NUMBER).setMaxWidth(28);
        columnModel.getColumn(PluginIconsSettingsTableModel.ICON_COL_NUMBER).setWidth(28);
        columnModel.getColumn(PluginIconsSettingsTableModel.ICON_ENABLED_COL_NUMBER).setWidth(28);
        columnModel.getColumn(PluginIconsSettingsTableModel.ICON_ENABLED_COL_NUMBER).setMaxWidth(28);
        columnModel.getColumn(PluginIconsSettingsTableModel.ICON_LABEL_COL_NUMBER).sizeWidthToFit();
        columnModel.getColumn(PluginIconsSettingsTableModel.ICON_TAGS_LABEL_COL_NUMBER).setMaxWidth(120);
        columnModel.getColumn(PluginIconsSettingsTableModel.ICON_TAGS_LABEL_COL_NUMBER).setMinWidth(120);
        columnModel.getColumn(PluginIconsSettingsTableModel.ICON_TAGS_LABEL_COL_NUMBER).setMaxWidth(120);
        columnModel.getColumn(PluginIconsSettingsTableModel.ICON_TAGS_LABEL_COL_NUMBER).setMinWidth(120);
        int requireRestartColWidth = I18nUtils.isChineseUIEnabled() ? 100 : 80;
        if (OS.CURRENT == OS.Windows) {
            requireRestartColWidth -= 5;
        }
        columnModel.getColumn(PluginIconsSettingsTableModel.ICON_REQUIRE_IDE_RESTART).setMaxWidth(requireRestartColWidth);
        columnModel.getColumn(PluginIconsSettingsTableModel.ICON_REQUIRE_IDE_RESTART).setMinWidth(requireRestartColWidth);

        // set invisible but keep data
        columnModel.removeColumn(columnModel.getColumn(PluginIconsSettingsTableModel.ICON_ID_COL_NUMBER));
        columnModel.removeColumn(columnModel.getColumn(PluginIconsSettingsTableModel.ICON_TAGS_ENUM_LIST_COL_NUMBER));
        if (currentSelected != -1) {
            this.pluginIconsTable.setRowSelectionInterval(currentSelected, currentSelected);
        }
    }

    private void loadIgnoredPattern() {
        this.ignoredPatternTextField.setText(getBestSettingsService(this.project).getIgnoredPattern());
    }

    private void loadAdditionalUIScale() {
        this.additionalUIScaleTextField.setText(Double.toString(SettingsIDEService.getInstance().getAdditionalUIScale2()));
    }

    private JComponent createToolbarDecorator() {
        ToolbarDecorator decorator = ToolbarDecorator.createDecorator(this.userIconsTable)

            .setAddAction(anActionButton -> {
                ModelDialog modelDialog = new ModelDialog(this, this.project);
                if (modelDialog.showAndGet()) {
                    Model newModel = modelDialog.getModelFromInput();
                    this.customModels.add(newModel);
                    foldersFirst(this.customModels);
                    setUserIconsTableModel();
                }
            })

            .setEditAction(anActionButton -> {
                int currentSelected = this.userIconsTable.getSelectedRow();
                ModelDialog modelDialog = new ModelDialog(this, this.project);
                modelDialog.setModelToEdit(this.customModels.get(currentSelected));
                if (modelDialog.showAndGet()) {
                    Model newModel = modelDialog.getModelFromInput();
                    this.customModels.set(currentSelected, newModel);
                    setUserIconsTableModel();
                }
            })

            .setRemoveAction(anActionButton -> {
                this.customModels.remove(this.userIconsTable.getSelectedRow());
                setUserIconsTableModel();
            })

            .setButtonComparator(i18n.getString("btn.add"), i18n.getString("btn.edit"), i18n.getString("btn.remove"))

            .setMoveUpAction(anActionButton -> reorderUserIcons(MoveDirection.UP, this.userIconsTable.getSelectedRow()))

            .setMoveDownAction(anActionButton -> reorderUserIcons(MoveDirection.DOWN, this.userIconsTable.getSelectedRow()));
        return decorator.createPanel();
    }

    private void reorderUserIcons(MoveDirection moveDirection, int selectedItemIdx) {
        Model modelToMove = this.customModels.get(selectedItemIdx);
        int newSelectedItemIdx = moveDirection == MoveDirection.UP ? selectedItemIdx - 1 : selectedItemIdx + 1;
        boolean selectedItemIsEnabled = (boolean) this.userIconsTable.getValueAt(selectedItemIdx, UserIconsSettingsTableModel.ICON_ENABLED_COL_NUMBER);
        boolean newSelectedItemIsEnabled = (boolean) this.userIconsTable.getValueAt(newSelectedItemIdx, UserIconsSettingsTableModel.ICON_ENABLED_COL_NUMBER);
        List<Boolean> itemsAreEnabled = new ArrayList<>();
        for (int i = 0; i < this.userIconsTable.getRowCount(); i++) {
            itemsAreEnabled.add((Boolean) this.userIconsTable.getValueAt(i, UserIconsSettingsTableModel.ICON_ENABLED_COL_NUMBER));
        }

        this.customModels.set(selectedItemIdx, this.customModels.get(newSelectedItemIdx));
        this.customModels.set(newSelectedItemIdx, modelToMove);
        setUserIconsTableModel();

        // User may have enabled or disabled some items, but changes are not applied yet to customModels, so setUserIconsTableModel will reset
        // the Enabled column to previous state. We need to reapply user changes on this column.
        for (int i = 0; i < this.userIconsTable.getRowCount(); i++) {
            this.userIconsTable.setValueAt(itemsAreEnabled.get(i), i, UserIconsSettingsTableModel.ICON_ENABLED_COL_NUMBER);
        }
        this.userIconsTable.setValueAt(selectedItemIsEnabled, newSelectedItemIdx, UserIconsSettingsTableModel.ICON_ENABLED_COL_NUMBER);
        this.userIconsTable.setValueAt(newSelectedItemIsEnabled, selectedItemIdx, UserIconsSettingsTableModel.ICON_ENABLED_COL_NUMBER);

        this.userIconsTable.clearSelection();
        this.userIconsTable.setRowSelectionInterval(newSelectedItemIdx, newSelectedItemIdx);

        // TODO fix Model and ModelCondition equals & hashCode methods in order
        //  to fix CollectionUtils.isEqualCollection(customModels, service.getCustomModels()).
        //  For now, the comparison returns true when customModels ordering changed. It should return false.
        this.forceUpdate = true;
    }

    /**
     * Returns the Project settings service if the project is not null, otherwise returns the IDE settings service.
     */
    private @NotNull SettingsService getBestSettingsService(@Nullable Project project) {
        if (project != null) {
            return SettingsProjectService.getInstance(project);
        }
        return SettingsIDEService.getInstance();
    }

    private enum MoveDirection {
        UP,
        DOWN
    }

    private void foldersFirst(List<Model> models) {
        models.sort((o1, o2) -> {
            // folders first, then files
            return ModelType.compare(o1.getModelType(), o2.getModelType());
        });
    }
}
