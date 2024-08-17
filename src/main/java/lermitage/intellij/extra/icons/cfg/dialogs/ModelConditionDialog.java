// SPDX-License-Identifier: MIT

package lermitage.intellij.extra.icons.cfg.dialogs;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.components.JBLabel;
import lermitage.intellij.extra.icons.ModelCondition;
import lermitage.intellij.extra.icons.utils.I18nUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.text.MessageFormat;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class ModelConditionDialog extends DialogWrapper {

    public static final String FIELD_SEPARATOR = ";";
    public static final String FIELD_SEPARATOR_NAME = "semicolon"; //NON-NLS

    private JPanel dialogPanel;
    private JCheckBox regexCheckBox;
    private JTextField regexTextField;
    private JCheckBox parentsCheckBox;
    private JTextField parentsTextField;
    private JCheckBox namesCheckBox;
    private JTextField namesTextField;
    private JCheckBox extensionsCheckBox;
    private JTextField extensionsTextField;
    private JRadioButton mayEndWithRadioButton;
    private JRadioButton endsWithRadioButton;
    private JRadioButton startsWithRadioButton;
    private JRadioButton equalsRadioButton;
    private JCheckBox noDotCheckBox;
    private JBLabel tipsLabel;
    private JCheckBox facetsCheckBox;
    private JTextField facetsTextField;

    public ModelConditionDialog() {
        super(false);
        init();
        setTitle(I18nUtils.RESOURCE_BUNDLE.getString("model.condition.dialog.title"));
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        initComponents();
        return this.dialogPanel;
    }

    private void initComponents() {
        ButtonGroup endButtonGroup = new ButtonGroup();
        endButtonGroup.add(this.endsWithRadioButton);
        endButtonGroup.add(this.mayEndWithRadioButton);
        this.endsWithRadioButton.setSelected(true);
        this.endsWithRadioButton.setEnabled(false);
        this.mayEndWithRadioButton.setEnabled(false);

        ButtonGroup namesButtonGroup = new ButtonGroup();
        namesButtonGroup.add(this.startsWithRadioButton);
        namesButtonGroup.add(this.equalsRadioButton);
        this.startsWithRadioButton.setSelected(true);
        this.startsWithRadioButton.setEnabled(false);
        this.equalsRadioButton.setEnabled(false);
        this.noDotCheckBox.setEnabled(false);

        this.regexCheckBox.addItemListener(item -> {
            boolean selected = item.getStateChange() == ItemEvent.SELECTED;
            this.regexTextField.setEnabled(selected);
        });

        this.parentsCheckBox.addItemListener(item -> {
            boolean selected = item.getStateChange() == ItemEvent.SELECTED;
            this.parentsTextField.setEnabled(selected);
        });

        this.namesCheckBox.addItemListener(item -> {
            boolean selected = item.getStateChange() == ItemEvent.SELECTED;
            this.namesTextField.setEnabled(selected);
            this.startsWithRadioButton.setEnabled(selected);
            this.equalsRadioButton.setEnabled(selected);
        });

        this.extensionsCheckBox.addItemListener(item -> {
            boolean selected = item.getStateChange() == ItemEvent.SELECTED;
            this.extensionsTextField.setEnabled(selected);
            this.mayEndWithRadioButton.setEnabled(selected);
            this.endsWithRadioButton.setEnabled(selected);
        });

        this.startsWithRadioButton.addItemListener(item -> {
            boolean selected = item.getStateChange() == ItemEvent.SELECTED;
            this.noDotCheckBox.setEnabled(selected);
        });

        this.startsWithRadioButton.addPropertyChangeListener("enabled", propertyChange ->
                this.noDotCheckBox.setEnabled((boolean) propertyChange.getNewValue())
        );

        this.facetsCheckBox.addItemListener(item -> {
            boolean selected = item.getStateChange() == ItemEvent.SELECTED;
            this.facetsTextField.setEnabled(selected);
        });

        this.regexTextField.setEnabled(false);
        this.parentsTextField.setEnabled(false);
        this.namesTextField.setEnabled(false);
        this.extensionsTextField.setEnabled(false);
        this.facetsTextField.setEnabled(false);

        this.tipsLabel.setText(MessageFormat.format(I18nUtils.RESOURCE_BUNDLE.getString("model.condition.dialog.tips"), FIELD_SEPARATOR_NAME));

        this.regexCheckBox.setText(I18nUtils.RESOURCE_BUNDLE.getString("model.condition.dialog.regex.checkbox"));
        this.parentsCheckBox.setText(I18nUtils.RESOURCE_BUNDLE.getString("model.condition.dialog.parents.checkbox"));
        this.namesCheckBox.setText(I18nUtils.RESOURCE_BUNDLE.getString("model.condition.dialog.names.checkbox"));
        this.startsWithRadioButton.setText(I18nUtils.RESOURCE_BUNDLE.getString("model.condition.dialog.startswith.checkbox"));
        this.equalsRadioButton.setText(I18nUtils.RESOURCE_BUNDLE.getString("model.condition.dialog.equals.checkbox"));
        this.noDotCheckBox.setText(I18nUtils.RESOURCE_BUNDLE.getString("model.condition.dialog.nodot.checkbox"));
        this.extensionsCheckBox.setText(I18nUtils.RESOURCE_BUNDLE.getString("model.condition.dialog.extensions.checkbox"));
        this.endsWithRadioButton.setText(I18nUtils.RESOURCE_BUNDLE.getString("model.condition.dialog.endswith.checkbox"));
        this.mayEndWithRadioButton.setText(I18nUtils.RESOURCE_BUNDLE.getString("model.condition.dialog.mayendwith.checkbox"));
        this.facetsCheckBox.setText(I18nUtils.RESOURCE_BUNDLE.getString("model.condition.dialog.facets.checkbox"));
    }

    private void createUIComponents() {
    }

    @Override
    protected @Nullable ValidationInfo doValidate() {
        if (this.regexCheckBox.isSelected()) {
            String regex = this.regexTextField.getText();
            PatternSyntaxException exception = tryCompileRegex(regex);
            if (regex.isEmpty() || exception != null) {
                String message = I18nUtils.RESOURCE_BUNDLE.getString("model.condition.dialog.err.invalid.regex");
                if (exception != null) {
                    message += " ( " + exception.getMessage() + ")";
                }
                return new ValidationInfo(message);
            }
        }

        if (this.parentsCheckBox.isSelected()) {
            if (this.parentsTextField.getText().isEmpty()) {
                return new ValidationInfo(I18nUtils.RESOURCE_BUNDLE.getString("model.condition.dialog.err.parent.missing"), this.parentsTextField);
            }
        }

        if (this.namesCheckBox.isSelected()) {
            if (this.namesTextField.getText().isEmpty()) {
                return new ValidationInfo(I18nUtils.RESOURCE_BUNDLE.getString("model.condition.dialog.err.name.missing"), this.namesTextField);
            }
        }

        if (this.extensionsCheckBox.isSelected()) {
            if (this.extensionsTextField.getText().isEmpty()) {
                return new ValidationInfo(I18nUtils.RESOURCE_BUNDLE.getString("model.condition.dialog.err.extension.missing"), this.extensionsTextField);
            }
        }

        if (this.mayEndWithRadioButton.isSelected() && this.mayEndWithRadioButton.isEnabled()) {
            if (!this.namesCheckBox.isSelected()) {
                return new ValidationInfo(I18nUtils.RESOURCE_BUNDLE.getString("model.condition.dialog.err.names.checkbox.if.may.end"), this.namesCheckBox);
            }
        }

        if (this.facetsCheckBox.isSelected()) {
            if (this.facetsTextField.getText().isEmpty()) {
                return new ValidationInfo(I18nUtils.RESOURCE_BUNDLE.getString("model.condition.dialog.err.facet.missing"), this.facetsTextField);
            }
        }

        if (!getModelConditionFromInput().isValid()) {
            return new ValidationInfo(I18nUtils.RESOURCE_BUNDLE.getString("model.condition.dialog.err.select.at.least.one.checkbox"));
        }

        return null;
    }

    /**
     * Creates a {@link ModelCondition} object from the user input.
     */
    public ModelCondition getModelConditionFromInput() {
        ModelCondition modelCondition = new ModelCondition();

        if (this.regexCheckBox.isSelected()) {
            modelCondition.setRegex(this.regexTextField.getText());
        }

        if (this.parentsCheckBox.isSelected()) {
            String[] parents = this.parentsTextField.getText().split(FIELD_SEPARATOR);
            modelCondition.setParents(parents);
        }

        if (this.namesCheckBox.isSelected()) {
            String[] names = this.namesTextField.getText().split(FIELD_SEPARATOR);
            if (this.startsWithRadioButton.isSelected()) {
                modelCondition.setStart(names);
                if (this.noDotCheckBox.isSelected()) {
                    modelCondition.setNoDot();
                }
            } else {
                modelCondition.setEq(names);
            }
        }

        if (this.extensionsCheckBox.isSelected()) {
            String[] extensions = this.extensionsTextField.getText().split(FIELD_SEPARATOR);
            if (this.mayEndWithRadioButton.isSelected()) {
                modelCondition.setMayEnd(extensions);
            } else {
                modelCondition.setEnd(extensions);
            }
        }

        if (this.facetsCheckBox.isSelected()) {
            String[] facets = this.facetsTextField.getText().toLowerCase().split(FIELD_SEPARATOR);
            modelCondition.setFacets(facets);
        }

        return modelCondition;
    }

    /**
     * Sets a condition that can be edited using this dialog.
     */
    public void setCondition(ModelCondition modelCondition) {
        setTitle(I18nUtils.RESOURCE_BUNDLE.getString("model.condition.dialog.edit.condition.title"));

        if (modelCondition.hasRegex()) {
            this.regexCheckBox.setSelected(true);
            this.regexTextField.setText(modelCondition.getRegex());
        }

        if (modelCondition.hasCheckParent()) {
            this.parentsCheckBox.setSelected(true);
            this.parentsTextField.setText(String.join(FIELD_SEPARATOR, modelCondition.getParents()));
        }

        if (modelCondition.hasStart() || modelCondition.hasEq()) {
            this.namesCheckBox.setSelected(true);
            this.namesTextField.setText(String.join(FIELD_SEPARATOR, modelCondition.getNames()));
            this.startsWithRadioButton.setSelected(modelCondition.hasStart());
            this.noDotCheckBox.setSelected(modelCondition.hasNoDot());
            this.equalsRadioButton.setSelected(modelCondition.hasEq());
        }

        if (modelCondition.hasEnd() || modelCondition.hasMayEnd()) {
            this.extensionsCheckBox.setSelected(true);
            this.extensionsTextField.setText(String.join(FIELD_SEPARATOR, modelCondition.getExtensions()));
            this.endsWithRadioButton.setSelected(modelCondition.hasEnd());
            this.mayEndWithRadioButton.setSelected(modelCondition.hasMayEnd());
        }

        if (modelCondition.hasFacets()) {
            this.facetsCheckBox.setSelected(true);
            this.facetsTextField.setText(String.join(FIELD_SEPARATOR, modelCondition.getFacets()));
        }
    }

    /**
     * Tries to compile a given regex and returns an exception if it failed.
     */
    private @Nullable PatternSyntaxException tryCompileRegex(String regex) {
        try {
            Pattern.compile(regex);
            return null;
        } catch (PatternSyntaxException ex) {
            return ex;
        }
    }
}
