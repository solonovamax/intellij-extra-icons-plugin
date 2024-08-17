// SPDX-License-Identifier: MIT

package lermitage.intellij.extra.icons.cfg.dialogs;

import com.intellij.ide.ui.laf.darcula.ui.DarculaTextBorder;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.ui.CheckBoxList;
import com.intellij.ui.ComboboxSpeedSearch;
import com.intellij.ui.JBColor;
import com.intellij.ui.ListUtil;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.IconUtil;
import lermitage.intellij.extra.icons.ExtraIconProvider;
import lermitage.intellij.extra.icons.ExtraIcons;
import lermitage.intellij.extra.icons.Globals;
import lermitage.intellij.extra.icons.IconType;
import lermitage.intellij.extra.icons.Model;
import lermitage.intellij.extra.icons.ModelCondition;
import lermitage.intellij.extra.icons.ModelType;
import lermitage.intellij.extra.icons.cfg.SettingsForm;
import lermitage.intellij.extra.icons.cfg.services.SettingsIDEService;
import lermitage.intellij.extra.icons.utils.BundledIcon;
import lermitage.intellij.extra.icons.utils.ComboBoxWithImageRenderer;
import lermitage.intellij.extra.icons.utils.FileChooserUtils;
import lermitage.intellij.extra.icons.utils.I18nUtils;
import lermitage.intellij.extra.icons.utils.IconUtils;
import lermitage.intellij.extra.icons.utils.ImageWrapper;
import lermitage.intellij.extra.icons.utils.ProjectUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static lermitage.intellij.extra.icons.cfg.dialogs.ModelConditionDialog.FIELD_SEPARATOR;

public class ModelDialog extends DialogWrapper {

    private final SettingsForm settingsForm;
    private final Project project;

    private CheckBoxList<ModelCondition> conditionsCheckboxList;
    private JPanel pane;
    private JBTextField modelIDField;
    private JBTextField descriptionField;
    private JComboBox<String> typeComboBox;
    private JLabel iconLabel;
    private JButton chooseIconButton;
    private JPanel conditionsPanel;
    private JBLabel idLabel;
    private JComboBox<Object> chooseIconSelector;
    private JBTextField ideIconOverrideTextField;
    private JLabel ideIconOverrideLabel;
    private JBLabel ideIconOverrideTip;
    private JTextField testTextField;
    private JLabel testLabel;
    private JLabel descriptionLabel;
    private JLabel typeLabel;
    private JLabel iconLeftLabel;
    private JBTextField iconPackField;
    private JLabel iconPackLabel;

    private ImageWrapper customIconImage;
    private JPanel toolbarPanel;

    private Model modelToEdit;

    public ModelDialog(SettingsForm settingsForm, Project project) {
        super(true);
        this.settingsForm = settingsForm;
        this.project = project;
        init();
        setTitle(I18nUtils.RESOURCE_BUNDLE.getString("model.dialog.title"));
        initComponents();
        this.conditionsPanel.addComponentListener(new ComponentAdapter() {
        });
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return this.pane;
    }

    private void initComponents() {
        setIdComponentsVisible(false);
        this.ideIconOverrideTip.setText(I18nUtils.RESOURCE_BUNDLE.getString("model.dialog.override.ide.tip"));
        this.ideIconOverrideTip.setToolTipText(I18nUtils.RESOURCE_BUNDLE.getString("model.dialog.override.ide.tip.tooltip"));
        this.ideIconOverrideTip.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        this.ideIconOverrideTip.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    Desktop.getDesktop().browse(new URI("https://jetbrains.design/intellij/resources/icons_list/"));
                } catch (Exception ex) {
                    //ignore
                }
            }
        });
        this.conditionsCheckboxList = new CheckBoxList<>((index, value) -> {
            //noinspection ConstantConditions
            this.conditionsCheckboxList.getItemAt(index).setEnabled(value);
        });

        this.chooseIconButton.addActionListener(al -> {
            try {
                this.customIconImage = loadCustomIcon();
                if (this.customIconImage != null) {
                    this.chooseIconSelector.setSelectedIndex(0);
                    this.iconLabel.setIcon(IconUtil.createImageIcon(((ImageWrapper.Base64Image) this.customIconImage).getImage()));
                }
            } catch (IllegalArgumentException ex) {
                Messages.showErrorDialog(ex.getMessage(), I18nUtils.RESOURCE_BUNDLE.getString("model.dialog.choose.icon.failed.to.load.icon"));
            }
        });

        this.conditionsCheckboxList.getEmptyText().setText(I18nUtils.RESOURCE_BUNDLE.getString("model.dialog.choose.icon.no.conditions.added"));
        this.conditionsCheckboxList.addPropertyChangeListener(evt -> testModel(getModelFromInput(), this.testTextField));

        this.toolbarPanel = createConditionsListToolbar();
        this.conditionsPanel.add(this.toolbarPanel, BorderLayout.CENTER);

        this.typeComboBox.addItem(ModelType.FILE.getI18nFriendlyName());
        this.typeComboBox.addItem(ModelType.DIR.getI18nFriendlyName());
        if (!this.settingsForm.isProjectForm()) {
            this.typeComboBox.addItem(ModelType.ICON.getI18nFriendlyName());
        }

        this.typeComboBox.addActionListener(e -> updateUIOnTypeChange());

        this.chooseIconSelector.addItem(I18nUtils.RESOURCE_BUNDLE.getString("model.dialog.choose.icon.first.item"));
        ExtraIconProvider.allModels().stream()
            .map(Model::getIcon)
            .sorted()
            .distinct()
            .forEach(iconPath -> this.chooseIconSelector.addItem(new BundledIcon(
                    iconPath, MessageFormat.format(I18nUtils.RESOURCE_BUNDLE.getString("model.dialog.choose.icon.bundled.icon"),
                iconPath.replace("extra-icons/", ""))))); //NON-NLS
        ComboBoxWithImageRenderer renderer = new ComboBoxWithImageRenderer();
        // customIconImage
        this.chooseIconSelector.setRenderer(renderer);
        this.chooseIconSelector.setToolTipText(I18nUtils.RESOURCE_BUNDLE.getString("model.dialog.choose.icon.tooltip"));
        this.chooseIconSelector.addItemListener(event -> {
            if (event.getStateChange() == ItemEvent.SELECTED) {
                Object item = event.getItem();
                if (item instanceof BundledIcon bundledIcon) {
                    this.iconLabel.setIcon(ExtraIcons.getIcon(bundledIcon.getIconPath()));
                    this.customIconImage = new ImageWrapper.BundledImage(bundledIcon.getIconPath());
                } else if (item instanceof String) {
                    this.iconLabel.setIcon(new ImageIcon());
                }
            }
        });

        ComboboxSpeedSearch.installSpeedSearch(this.chooseIconSelector, Object::toString);

        this.testLabel.setText(I18nUtils.RESOURCE_BUNDLE.getString("model.dialog.model.tester"));
        this.testTextField.setText("");
        this.testTextField.setToolTipText(I18nUtils.RESOURCE_BUNDLE.getString("model.dialog.model.tester.tooltip"));
        this.testTextField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                testModel(getModelFromInput(), ModelDialog.this.testTextField);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                testModel(getModelFromInput(), ModelDialog.this.testTextField);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                testModel(getModelFromInput(), ModelDialog.this.testTextField);
            }
        });

        this.idLabel.setText(I18nUtils.RESOURCE_BUNDLE.getString("model.dialog.id.label"));
        this.descriptionLabel.setText(I18nUtils.RESOURCE_BUNDLE.getString("model.dialog.description.label"));
        this.typeLabel.setText(I18nUtils.RESOURCE_BUNDLE.getString("model.dialog.type.label"));
        this.iconPackLabel.setText(I18nUtils.RESOURCE_BUNDLE.getString("model.dialog.iconpack.label"));
        this.ideIconOverrideLabel.setText(I18nUtils.RESOURCE_BUNDLE.getString("model.dialog.icons.name.label"));
        this.iconLeftLabel.setText(I18nUtils.RESOURCE_BUNDLE.getString("model.dialog.icon.type.selector.label"));
        this.chooseIconButton.setText(I18nUtils.RESOURCE_BUNDLE.getString("model.dialog.icon.chooser.btn"));
        this.testLabel.setText(I18nUtils.RESOURCE_BUNDLE.getString("model.dialog.tester.label"));

        updateUIOnTypeChange();
    }

    private void testModel(Model model, JTextField testTextField) {
        if (testTextField.getText().isEmpty()) {
            testTextField.setBorder(new DarculaTextBorder());
        } else {
            if (model == null || model.getConditions().isEmpty()) {
                testTextField.setBorder(new LineBorder(JBColor.RED, 1));
            } else {
                boolean checked = model.check(
                    parent(testTextField.getText().toLowerCase()),
                    filenameOnly(testTextField.getText().toLowerCase()),
                    testTextField.getText().toLowerCase(),
                    Collections.emptySet(), null);
                if (checked) {
                    testTextField.setBorder(new LineBorder(JBColor.GREEN, 1));
                } else {
                    testTextField.setBorder(new LineBorder(JBColor.RED, 1));
                }
            }
        }
    }

    private void updateUIOnTypeChange() {
        Object selectedItem = this.typeComboBox.getSelectedItem();
        this.testLabel.setVisible(true);
        this.testTextField.setVisible(true);
        if (selectedItem != null) {
            Optional<ModelType> selectedModelType = getSelectedModelType();
            boolean ideIconOverrideSelected = selectedModelType.isPresent() && selectedModelType.get() == ModelType.ICON;
            this.ideIconOverrideLabel.setVisible(ideIconOverrideSelected);
            this.ideIconOverrideTextField.setVisible(ideIconOverrideSelected);
            this.ideIconOverrideTip.setVisible(ideIconOverrideSelected);
            this.conditionsPanel.setVisible(!ideIconOverrideSelected);
            if (ideIconOverrideSelected) {
                this.testLabel.setVisible(false);
                this.testTextField.setVisible(false);
            }
        }
    }

    /**
     * Creates a new model from the user input.
     */
    public Model getModelFromInput() {
        String icon = null;
        IconType iconType = null;
        if (this.customIconImage != null) {
            iconType = this.customIconImage.getIconType();
            if (this.customIconImage instanceof ImageWrapper.BundledImage bundledImage) {
                icon = bundledImage.getBundledIconRef();
            } else {
                icon = IconUtils.toBase64(this.customIconImage);
            }
        } else if (this.modelToEdit != null) {
            icon = this.modelToEdit.getIcon();
            iconType = this.modelToEdit.getIconType();
        }

        Optional<ModelType> selectedModlType = getSelectedModelType();
        Model newModel = null;
        if (selectedModlType.isPresent()) {
            if (selectedModlType.get() == ModelType.ICON) {
                newModel = Model.createIdeIconModel(
                        this.modelIDField.isVisible() ? this.modelIDField.getText() : null,
                        this.ideIconOverrideTextField.getText(),
                    icon,
                        this.descriptionField.getText(),
                    selectedModlType.get(),
                    iconType,
                        this.iconPackField.getText()
                );
            } else {
                newModel = Model.createFileOrFolderModel(
                        this.modelIDField.isVisible() ? this.modelIDField.getText() : null,
                    icon,
                        this.descriptionField.getText(),
                    selectedModlType.get(),
                    iconType,
                        this.iconPackField.getText(),
                    IntStream.range(0, this.conditionsCheckboxList.getItemsCount())
                        .mapToObj(index -> this.conditionsCheckboxList.getItemAt(index))
                        .collect(Collectors.toList())
                );
            }
        }

        if (this.modelToEdit != null && newModel != null) {
            newModel.setEnabled(this.modelToEdit.isEnabled());
        }
        return newModel;
    }

    private Optional<ModelType> getSelectedModelType() {
        int selectedIndex = this.typeComboBox.getSelectedIndex();
        if (selectedIndex < 0) {
            return Optional.empty();
        }
        return Optional.of(ModelType.values()[this.typeComboBox.getSelectedIndex()]);
    }

    private int getModelTypeIdx(ModelType modelType) {
        return switch (modelType) {
            case FILE -> 0;
            case DIR -> 1;
            case ICON -> 2;
        };
    }

    /**
     * Sets a model that will be edited using this dialog.
     */
    public void setModelToEdit(Model model) {
        this.modelToEdit = model;
        setTitle(I18nUtils.RESOURCE_BUNDLE.getString("model.dialog.model.editor"));
        boolean hasModelId = model.getId() != null;
        setIdComponentsVisible(hasModelId);
        if (hasModelId) {
            this.modelIDField.setText(model.getId());
        }
        this.descriptionField.setText(model.getDescription());
        this.ideIconOverrideTextField.setText(model.getIdeIcon());
        this.typeComboBox.setSelectedIndex(getModelTypeIdx(model.getModelType()));
        this.typeComboBox.updateUI();
        this.iconPackField.setText(model.getIconPack());

        SettingsIDEService settingsIDEService = SettingsIDEService.getInstance();
        Double additionalUIScale = settingsIDEService.getAdditionalUIScale2();
        SwingUtilities.invokeLater(() -> this.iconLabel.setIcon(IconUtils.getIcon(model, additionalUIScale, settingsIDEService.getUiTypeIconsPreference())));
        if (model.getIconType() == IconType.PATH) {
            for (int itemIdx = 0; itemIdx < this.chooseIconSelector.getItemCount(); itemIdx++) {
                Object item = this.chooseIconSelector.getItemAt(itemIdx);
                if (item instanceof BundledIcon && ((BundledIcon) item).getIconPath().equals(model.getIcon())) {
                    this.chooseIconSelector.setSelectedIndex(itemIdx);
                    break;
                }
            }
        }
        model.getConditions().forEach(modelCondition ->
                this.conditionsCheckboxList.addItem(modelCondition, modelCondition.asReadableString(FIELD_SEPARATOR), modelCondition.isEnabled()));

        updateUIOnTypeChange();
    }

    /**
     * Adds a toolbar with add, edit and remove actions to the CheckboxList.
     */
    private JPanel createConditionsListToolbar() {
        return ToolbarDecorator.createDecorator(this.conditionsCheckboxList).setAddAction(anActionButton -> {
            ModelConditionDialog modelConditionDialog = new ModelConditionDialog();
            if (modelConditionDialog.showAndGet()) {
                ModelCondition modelCondition = modelConditionDialog.getModelConditionFromInput();
                this.conditionsCheckboxList.addItem(modelCondition, modelCondition.asReadableString(FIELD_SEPARATOR), modelCondition.isEnabled());
            }
            testModel(getModelFromInput(), this.testTextField);
        }).setEditAction(anActionButton -> {
            int selectedItem = this.conditionsCheckboxList.getSelectedIndex();
            ModelCondition selectedCondition = Objects.requireNonNull(this.conditionsCheckboxList.getItemAt(selectedItem));
            boolean isEnabled = this.conditionsCheckboxList.isItemSelected(selectedCondition);

            ModelConditionDialog modelConditionDialog = new ModelConditionDialog();
            modelConditionDialog.setCondition(selectedCondition);
            if (modelConditionDialog.showAndGet()) {
                ModelCondition newCondition = modelConditionDialog.getModelConditionFromInput();
                this.conditionsCheckboxList.updateItem(selectedCondition, newCondition, newCondition.asReadableString(FIELD_SEPARATOR));
                newCondition.setEnabled(isEnabled);
            }
            testModel(getModelFromInput(), this.testTextField);
        }).setRemoveAction(anActionButton -> {
                ListUtil.removeSelectedItems(this.conditionsCheckboxList);
                testModel(getModelFromInput(), this.testTextField);
            }
        ).setButtonComparator(
                I18nUtils.RESOURCE_BUNDLE.getString("model.dialog.creator.condition.col.add"),
                I18nUtils.RESOURCE_BUNDLE.getString("model.dialog.creator.condition.col.edit"),
                I18nUtils.RESOURCE_BUNDLE.getString("model.dialog.creator.condition.col.remove")
        ).createPanel();
    }

    /**
     * Opens a file chooser dialog and loads the icon.
     */
    private ImageWrapper loadCustomIcon() {
        String iconPath = FileChooserUtils.chooseFile("", this.pane,
            Globals.ALLOWED_ICON_FILE_EXTENSIONS_FILE_SELECTOR_LABEL,
            Globals.ALLOWED_ICON_FILE_EXTENSIONS);
        Project projectToLinkToModalProgress = this.project;
        if (projectToLinkToModalProgress == null) {
            projectToLinkToModalProgress = ProjectUtils.getFirstOpenedProject(ProjectManager.getInstance());
        }
        if (projectToLinkToModalProgress == null) {
            if (iconPath != null) {
                // TODO User wants to edit a User Icon when no project is opened. We have no workaround to
                //  avoid "Slow operations are prohibited on EDT" error log in this situation, but, this is only
                //  a log message, nothing is broken. I think we can leave it as is, and remove this code once
                //  issue #126 has a better fix.
                VirtualFile fileByUrl = VirtualFileManager.getInstance().findFileByNioPath(Path.of(iconPath));
                if (fileByUrl != null) {
                    return IconUtils.loadFromVirtualFile(fileByUrl);
                }
            }
        } else {
            if (iconPath != null) {
                // FIXME temporary workaround for "Slow operations are prohibited on EDT" issue
                //  https://github.com/jonathanlermitage/intellij-extra-icons-plugin/issues/126
                //  We should be able to use VirtualFileManager.getInstance().findFileByNioPath directly
                return ActionUtil.underModalProgress(projectToLinkToModalProgress, "Loading selected icon", //NON-NLS
                    () -> {
                        VirtualFile fileByUrl = VirtualFileManager.getInstance().findFileByNioPath(Path.of(iconPath));
                        if (fileByUrl != null) {
                            return IconUtils.loadFromVirtualFile(fileByUrl);
                        }
                        return null;
                    });
            }
        }
        return null;
    }

    private void setIdComponentsVisible(boolean visible) {
        this.idLabel.setVisible(visible);
        this.modelIDField.setVisible(visible);
    }

    @Override
    protected @Nullable ValidationInfo doValidate() {
        if (this.modelIDField.isVisible() && this.modelIDField.getText().isEmpty()) {
            return new ValidationInfo(I18nUtils.RESOURCE_BUNDLE.getString("model.dialog.validation.id.missing"), this.modelIDField);
        }
        if (this.descriptionField.getText().isEmpty()) {
            return new ValidationInfo(I18nUtils.RESOURCE_BUNDLE.getString("model.dialog.validation.desc.missing"), this.descriptionField);
        }
        if (this.customIconImage == null && this.modelToEdit == null) {
            return new ValidationInfo(I18nUtils.RESOURCE_BUNDLE.getString("model.dialog.validation.icon.missing"), this.chooseIconButton);
        }

        int selectedItemIdx = this.typeComboBox.getSelectedIndex();
        if (selectedItemIdx != -1 && selectedItemIdx == getModelTypeIdx(ModelType.ICON)) {
            if (this.ideIconOverrideTextField.getText().trim().isEmpty()) {
                return new ValidationInfo(I18nUtils.RESOURCE_BUNDLE.getString("model.dialog.validation.ide.icon.name.missing"), this.ideIconOverrideTextField);
            } else if (!this.ideIconOverrideTextField.getText().endsWith(".svg")) {
                return new ValidationInfo(I18nUtils.RESOURCE_BUNDLE.getString("model.dialog.validation.ide.icon.must.end.svg"), this.ideIconOverrideTextField);
            }
        } else {
            if (this.conditionsCheckboxList.isEmpty()) {
                return new ValidationInfo(I18nUtils.RESOURCE_BUNDLE.getString("model.dialog.validation.condition.missing"), this.toolbarPanel);
            }
        }

        return super.doValidate();
    }

    /**
     * Find parent folder of given path, otherwise return empty string.
     */
    private String parent(String path) {
        path = path.replace("\\\\", "/");
        Matcher matcher = Pattern.compile(".*/(.+)/.+").matcher(path);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    private String filenameOnly(String path) {
        path = path.replace("\\\\", "/");
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        if (path.contains("/")) {
            return path.substring(path.lastIndexOf('/') + 1);
        }
        return path;
    }
}
