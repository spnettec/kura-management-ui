/*******************************************************************************
 * Copyright (c) 2016, 2026 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 *  Red Hat
 *******************************************************************************/
package org.eclipse.kura.web.client.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.util.LabelComparator;
import org.eclipse.kura.web.client.util.ValidationUtil;
import org.eclipse.kura.web.client.util.request.RequestQueue;
import org.eclipse.kura.web.shared.model.GwtCloudEntry;
import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.eclipse.kura.web.shared.model.GwtConfigParameter;
import org.eclipse.kura.web.shared.model.GwtConfigParameter.GwtConfigParameterType;
import org.eclipse.kura.web.shared.model.GwtScriptValidationError;
import org.eclipse.kura.web.shared.model.GwtScriptValidationResult;
import org.eclipse.kura.web.shared.service.GwtComponentService;
import org.eclipse.kura.web.shared.service.GwtComponentServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.gwtbootstrap3.client.ui.Anchor;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.DropDown;
import org.gwtbootstrap3.client.ui.DropDownHeader;
import org.gwtbootstrap3.client.ui.DropDownMenu;
import org.gwtbootstrap3.client.ui.FormGroup;
import org.gwtbootstrap3.client.ui.FormLabel;
import org.gwtbootstrap3.client.ui.HelpBlock;
import org.gwtbootstrap3.client.ui.InlineHelpBlock;
import org.gwtbootstrap3.client.ui.InlineRadio;
import org.gwtbootstrap3.client.ui.Input;
import org.gwtbootstrap3.client.ui.ListBox;
import com.google.gwt.user.client.ui.Panel;
import org.gwtbootstrap3.client.ui.TextArea;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.client.ui.base.TextBoxBase;
import org.gwtbootstrap3.client.ui.constants.ButtonSize;
import org.gwtbootstrap3.client.ui.constants.ButtonType;
import org.gwtbootstrap3.client.ui.constants.IconType;
import org.gwtbootstrap3.client.ui.constants.Toggle;
import org.gwtbootstrap3.client.ui.constants.ValidationState;
import org.gwtbootstrap3.client.ui.form.error.BasicEditorError;
import org.gwtbootstrap3.client.ui.form.validator.Validator;
import org.gwtbootstrap3.client.ui.gwt.FlowPanel;

import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.Editor;
import com.google.gwt.editor.client.EditorError;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public abstract class AbstractServicesUi extends Composite {

    private static final String TARGET_SUFFIX = ".target";

    protected static final Logger logger = Logger.getLogger(AbstractServicesUi.class.getSimpleName());
    protected static final Logger errorLogger = Logger.getLogger("ErrorLogger");

    private static final String PLACEHOLDER = "Placeholder";

    protected static final Messages MSGS = GWT.create(Messages.class);
    protected static final LabelComparator<String> DROPDOWN_LABEL_COMPARATOR = new LabelComparator<>();

    private final GwtComponentServiceAsync gwtComponentService = GWT.create(GwtComponentService.class);
    private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);

    protected List<GwtCloudEntry> cloudInstancesBinder;

    protected GwtConfigComponent configurableComponent;

    protected HashMap<String, Boolean> valid = new HashMap<>();

    // Code editors whose language follows another parameter's value (|Editor:$<param> or |code), keyed by the id of
    // that controlling parameter; updated live when the controlling field changes.
    private final Map<String, List<CodeEditorTextArea>> dynamicLangEditors = new HashMap<>();

    // Conventional ids of a "script language" field that a bare |code marker follows, in priority order.
    private static final String[] CONVENTIONAL_LANGUAGE_PARAMS = { "language", "scriptEngineName", "lang" };

    public abstract void setDirty(boolean flag);

    public abstract boolean isDirty();

    protected abstract void reset();

    protected abstract void renderForm();

    protected void renderMultiFieldConfigParameter(GwtConfigParameter mParam) {
        String value;
        String[] values = mParam.getValues();
        boolean isFirstInstance = true;
        FormGroup formGroup = new FormGroup();
        for (int i = 0; i < Math.min(mParam.getCardinality(), 10); i++) {
            // temporary set the param value to the current one in the array
            // use a value from the one passed in if we have it.
            value = null;
            if (values != null && i < values.length) {
                value = values[i];
            }
            mParam.setValue(value);
            renderConfigParameter(mParam, isFirstInstance, formGroup);
            if (isFirstInstance) {
                isFirstInstance = false;
            }
        }
        // restore a null current value
        mParam.setValue(null);
    }

    // passes the parameter to the corresponding method depending on the type of
    // field to be rendered
    protected void renderConfigParameter(GwtConfigParameter param, boolean isFirstInstance, FormGroup formGroup) {
        Map<String, String> options = param.getOptions();
        if (options != null && options.size() > 0) {
            renderChoiceField(param, isFirstInstance, formGroup);
        } else if (param.getType().equals(GwtConfigParameterType.BOOLEAN)) {
            renderBooleanField(param, isFirstInstance, formGroup);
        } else if (param.getType().equals(GwtConfigParameterType.PASSWORD)) {
            renderPasswordField(param, isFirstInstance, formGroup);
        } else {
            renderTextField(param, isFirstInstance, formGroup);
        }
    }

    // Checks if all the fields are valid according to the Validate() method
    public boolean isValid() {
        // check if all fields are valid
        for (Map.Entry<String, Boolean> entry : this.valid.entrySet()) {
            if (!entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    // Field Render based on Type
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected void renderTextField(final GwtConfigParameter param, boolean isFirstInstance, final FormGroup formGroup) {

        this.valid.put(param.getId(), true);

        if (isFirstInstance) {
            FormLabel formLabel = new FormLabel();
            formLabel.setText(param.getName());
            if (param.isRequired()) {
                formLabel.setShowRequiredIndicator(true);
            }
            formLabel.setTitle(param.getId());
            formGroup.add(formLabel);

            InlineHelpBlock ihb = new InlineHelpBlock();
            ihb.setIconType(IconType.EXCLAMATION_TRIANGLE);
            formGroup.add(ihb);

            HelpBlock tooltip = new HelpBlock();
            tooltip.setText(getDescription(param));
            formGroup.add(tooltip);
        }

        final TextBoxBase textBox = createTextBox(param);

        String formattedValue = "";

        // TODO: Probably this formatting step has no
        // sense. But it seems that, if not in debug,
        // all the browsers are able to display the
        // double value as expected
        switch (param.getType()) {
        case LONG:
            if (param.getValue() != null && !"".equals(param.getValue().trim())) {
                formattedValue = String.valueOf(Long.parseLong(param.getValue()));
            }
            break;
        case DOUBLE:
            if (param.getValue() != null && !"".equals(param.getValue().trim())) {
                formattedValue = String.valueOf(Double.parseDouble(param.getValue()));
            }
            break;
        case FLOAT:
            if (param.getValue() != null && !"".equals(param.getValue().trim())) {
                formattedValue = String.valueOf(Float.parseFloat(param.getValue()));
            }
            break;
        case SHORT:
            if (param.getValue() != null && !"".equals(param.getValue().trim())) {
                formattedValue = String.valueOf(Short.parseShort(param.getValue()));
            }
            break;
        case BYTE:
            if (param.getValue() != null && !"".equals(param.getValue().trim())) {
                formattedValue = String.valueOf(Byte.parseByte(param.getValue()));
            }
            break;
        case INTEGER:
            if (param.getValue() != null && !"".equals(param.getValue().trim())) {
                formattedValue = String.valueOf(Integer.parseInt(param.getValue()));
            }
            break;
        default:
            formattedValue = param.getValue();
            break;
        }

        if (param.getValue() != null) {
            textBox.setText(formattedValue);
        } else {
            textBox.setText("");
        }

        if (param.getMin() != null && param.getMin().equals(param.getMax())) {
            textBox.setReadOnly(true);
            textBox.setEnabled(false);
        }

        formGroup.add(textBox);

        if (textBox instanceof CodeEditorTextArea) {
            addScriptValidationControls((CodeEditorTextArea) textBox, formGroup);
        }

        textBox.addValidator(new Validator() {

            @Override
            public List<EditorError> validate(Editor editor, Object value) {
                return validateTextBox(param, formGroup);
            }

            @Override
            public int getPriority() {
                return 0;
            }
        });
        textBox.addKeyUpHandler(event -> {
            textBox.validate(true);
        });
        final String originValue = formattedValue == null ? "" : formattedValue;
        textBox.addValueChangeHandler(event -> {
            if (originValue.equals(event.getValue())) {
                setDirty(false);
            } else {
                setDirty(true);
            }
            updateDynamicEditors(param.getId(), event.getValue());
        });

        if (param.getId().endsWith(TARGET_SUFFIX)) {
            textBox.setReadOnly(true);
            String targetedService = param.getId().split(TARGET_SUFFIX)[0];

            DropDown dropDown = new DropDown();
            Anchor dropDownAnchor = new Anchor();
            dropDownAnchor.setText(MSGS.selectAvailableTargets());
            dropDownAnchor.setDataToggle(Toggle.DROPDOWN);

            dropDown.add(dropDownAnchor);

            final DropDownMenu dropDownMenu = new DropDownMenu();
            dropDownMenu.addStyleName("drop-down");

            DropDownHeader dropDownHeader = new DropDownHeader();
            dropDownHeader.setVisible(false);
            dropDownMenu.add(dropDownHeader);

            dropDown.add(dropDownMenu);
            KuraTextBox kuraTextBox = (KuraTextBox) textBox;
            kuraTextBox.setData(formattedValue);
            kuraTextBox.setText(MSGS.noTargetsAvailable());
            RequestQueue.submit(context -> this.gwtXSRFService.generateSecurityToken(
                    context.callback(token -> AbstractServicesUi.this.gwtComponentService.getPidNamesFromTarget(token,
                            getSCRComponentName(this.configurableComponent), targetedService, context.callback(data -> {
                                dropDownHeader.setText(MSGS.noTargetsAvailable());
                                if (!data.isEmpty()) {
                                    final String targetData;
                                    if (kuraTextBox.getData() != null && kuraTextBox.getData().contains("=")) {
                                        targetData = kuraTextBox.getData().split("=")[1].replace(")", "");
                                    } else {
                                        targetData = "";
                                    }
                                    data.forEach((targetEntryKey, targetEntryName) -> {
                                        KuraAnchorListItem listItem = createListItem(kuraTextBox, targetEntryKey,
                                                targetEntryName, targetData);
                                        dropDownMenu.add(listItem);
                                    });
                                }
                                dropDownHeader.setVisible(true);
                            })))));

            formGroup.add(dropDown);
        }

        textBox.validate(true);
    }

    private static final String getSCRComponentName(final GwtConfigComponent component) {
        final String factoryPid = component.getFactoryId();

        if (factoryPid != null) {
            return factoryPid;
        }

        return component.getComponentId();
    }

    /** ACE editor modes the backend {@code ScriptValidationService} is able to compile-check. */
    private static final Set<String> VALIDATABLE_EDITOR_MODES = new HashSet<>(
            Arrays.asList("js", "javascript", "ecmascript", "groovy", "python", "py", "wasm", "camel-java", "camel-xml",
                    "camel-yaml"));

    /**
     * Adds a "Validate" button (and a status line) under a code editor whose language the backend can compile-check.
     * Clicking it sends the current script to the {@code ScriptValidationService} and shows any syntax / compilation
     * errors inline (gutter annotations) and as text, so the author doesn't have to dig through the gateway logs.
     */
    private void addScriptValidationControls(final CodeEditorTextArea editor, final FormGroup formGroup) {
        // The button is always added: the editor language may change at runtime (|Editor:$<param> / |code), so whether
        // it can be compile-checked is re-evaluated on each click rather than fixed at render time.
        final Button validateButton = new Button();
        validateButton.setText(MSGS.scriptValidateButton());
        validateButton.setType(ButtonType.DEFAULT);
        validateButton.setSize(ButtonSize.SMALL);
        validateButton.addStyleName("kura-code-editor-validate");

        final HelpBlock status = new HelpBlock();
        status.addStyleName("kura-code-editor-validate-status");

        validateButton.addClickHandler(event -> {
            final String language = editor.getMode();
            if (language == null || !VALIDATABLE_EDITOR_MODES.contains(language.toLowerCase())) {
                editor.clearAnnotations();
                status.setText(MSGS.scriptValidateUnsupported());
                status.getElement().getStyle().setColor("#8a6d3b");
                return;
            }
            final String script = editor.getEditorValue();
            if (script == null || script.trim().isEmpty()) {
                editor.clearAnnotations();
                status.setText(MSGS.scriptValidateEmpty());
                status.getElement().getStyle().setColor("#8a6d3b");
                return;
            }
            status.setText(MSGS.scriptValidateInProgress());
            status.getElement().getStyle().clearColor();
            RequestQueue.submit(context -> this.gwtXSRFService.generateSecurityToken(
                    context.callback(token -> AbstractServicesUi.this.gwtComponentService.validateScript(token, language,
                            script, context.callback(result -> showScriptValidationResult(editor, status, result))))));
        });

        formGroup.add(validateButton);
        formGroup.add(status);
    }

    private void showScriptValidationResult(final CodeEditorTextArea editor, final HelpBlock status,
            final GwtScriptValidationResult result) {
        if (result.isValid()) {
            editor.clearAnnotations();
            status.setText(MSGS.scriptValidateOk());
            status.getElement().getStyle().setColor("#3c763d");
            return;
        }

        editor.setAnnotations(result.getErrors());

        boolean hasError = false;
        final StringBuilder sb = new StringBuilder();
        for (final GwtScriptValidationError error : result.getErrors()) {
            if (!"warning".equalsIgnoreCase(error.getSeverity())) {
                hasError = true;
            }
            if (sb.length() > 0) {
                sb.append('\n');
            }
            if (error.getLine() > 0) {
                sb.append(MSGS.scriptValidateErrorAtLine(String.valueOf(error.getLine()), error.getMessage()));
            } else {
                sb.append(error.getMessage());
            }
        }
        status.setText(sb.toString());
        // red for real compile errors, amber when the only thing reported is a "validator unavailable" warning
        status.getElement().getStyle().setColor(hasError ? "#a94442" : "#8a6d3b");
    }

    private KuraAnchorListItem createListItem(final KuraTextBox textBox, String targetEntryKey, String targetEntryName,
            String targetData) {
        String textDisplay = (targetEntryName == null ? targetEntryKey : targetEntryName) + "(" + targetEntryKey + ")";
        if (targetEntryName == null || "".equals(targetEntryName))
            textDisplay = targetEntryKey;
        if (targetEntryKey.equals(targetData)) {
            textBox.setText(textDisplay);
        }
        KuraAnchorListItem listItem = new KuraAnchorListItem();
        listItem.setText(textDisplay);
        listItem.setData("(kura.service.pid=" + targetEntryKey + ")");
        listItem.addClickHandler(event -> {
            KuraAnchor eventGenerator = (KuraAnchor) event.getSource();
            textBox.setData(eventGenerator.getData());
            textBox.setText(eventGenerator.getText());
            setDirty(true);
        });
        return listItem;
    }

    private TextBoxBase createTextBox(final GwtConfigParameter param) {
        if (param.getId().endsWith(TARGET_SUFFIX)) {
            return new KuraTextBox();
        }
        final EditorSpec editorSpec = getEditorSpec(param);
        if (editorSpec != null) {
            // Markers |Editor:<lang> / |Editor:$<param>[:default] / |code[:default] swap the textarea for an ACE
            // code editor (falls back to a plain textarea internally if window.ace failed to load). When the language
            // is driven by another field, register the editor so it follows that field's value live.
            final CodeEditorTextArea editor = new CodeEditorTextArea(resolveEditorMode(editorSpec));
            if (editorSpec.controllingParamId != null) {
                editor.setLanguagePrefix(editorSpec.prefix);
                this.dynamicLangEditors.computeIfAbsent(editorSpec.controllingParamId, k -> new ArrayList<>())
                        .add(editor);
            }
            return editor;
        }
        if (param.getDescription() != null && param.getDescription().contains("\u200B\u200B\u200B\u200B\u200B")) {
            final ExtendedTextArea result = createTextArea();
            result.setHeight("500px");
            return result;
        }
        if (isTextArea(param)) {
            return createTextArea();
        }
        return new ExtendedTextBox();
    }

    /**
     * How a code-editor field gets its language: an optional literal {@code prefix} prepended to a controlling
     * parameter's value (or to the default). E.g. {@code camel-$file.extension} → prefix {@code camel-}, controlling
     * {@code file.extension}, so a selected value {@code xml} becomes the language {@code camel-xml}.
     */
    private static final class EditorSpec {
        private final String prefix;
        private final String controllingParamId;
        private final String defaultLang;

        EditorSpec(final String prefix, final String controllingParamId, final String defaultLang) {
            this.prefix = prefix == null ? "" : prefix;
            this.controllingParamId = controllingParamId;
            this.defaultLang = defaultLang;
        }
    }

    /**
     * Parses a parameter's description marker into an {@link EditorSpec}, or {@code null} when the field is not a code
     * editor. Supported markers (after the trailing {@code |}):
     * <ul>
     * <li>{@code Editor:<lang>} \u2014 fixed language (e.g. {@code Editor:groovy}).</li>
     * <li>{@code Editor:$<paramId>} \u2014 language taken live from sibling parameter {@code <paramId>}.</li>
     * <li>{@code Editor:$<paramId>:<defaultLang>} \u2014 same, with a default used when that parameter is empty.</li>
     * <li>{@code code} / {@code code:<defaultLang>} \u2014 follows the first present conventional language field
     * ({@link #CONVENTIONAL_LANGUAGE_PARAMS}), with an optional default.</li>
     * <li>{@code Editor:<prefix>$<paramId>[:<defaultLang>]} \u2014 a literal prefix may precede {@code $}; it is prepended
     * to the resolved value (e.g. {@code camel-$file.extension} \u2192 {@code camel-xml}).</li>
     * </ul>
     */
    private EditorSpec getEditorSpec(final GwtConfigParameter param) {
        if (param == null || param.getType() != GwtConfigParameterType.STRING) {
            return null;
        }
        final String description = param.getDescription();
        if (description == null) {
            return null;
        }
        final String[] result = splitDescription(description);
        if (result.length < 2 || result[1] == null) {
            return null;
        }
        final String marker = result[1].trim();

        // |code  or  |code:<defaultLang>
        if (marker.regionMatches(true, 0, "code", 0, 4) && (marker.length() == 4 || marker.charAt(4) == ':')) {
            final String def = marker.length() > 5 ? emptyToNull(marker.substring(5).trim()) : null;
            return new EditorSpec("", firstPresentLanguageParam(), def);
        }

        // |Editor:...
        final String editorPrefix = "Editor:";
        if (!marker.regionMatches(true, 0, editorPrefix, 0, editorPrefix.length())) {
            return null;
        }
        final String body = marker.substring(editorPrefix.length()).trim();
        if (body.isEmpty()) {
            return null;
        }
        final int dollar = body.indexOf('$');
        if (dollar >= 0) {
            final String literalPrefix = body.substring(0, dollar);
            final String rest = body.substring(dollar + 1).trim();
            final int colon = rest.indexOf(':');
            if (colon >= 0) {
                return new EditorSpec(literalPrefix, emptyToNull(rest.substring(0, colon).trim()),
                        emptyToNull(rest.substring(colon + 1).trim()));
            }
            return new EditorSpec(literalPrefix, emptyToNull(rest), null);
        }
        // fixed language
        return new EditorSpec("", null, body);
    }

    /** Resolves the initial ACE mode for a spec: prefix + (controlling field's current value, else default), else text. */
    private String resolveEditorMode(final EditorSpec spec) {
        String base = null;
        if (spec.controllingParamId != null) {
            final String value = currentParamValue(spec.controllingParamId);
            if (value != null && !value.isEmpty()) {
                base = value;
            }
        }
        if (base == null) {
            base = spec.defaultLang;
        }
        if (base == null || base.isEmpty()) {
            return "text";
        }
        return (spec.prefix + base).toLowerCase();
    }

    private String firstPresentLanguageParam() {
        if (this.configurableComponent == null) {
            return null;
        }
        for (final String candidate : CONVENTIONAL_LANGUAGE_PARAMS) {
            if (this.configurableComponent.getParameter(candidate) != null) {
                return candidate;
            }
        }
        return null;
    }

    private String currentParamValue(final String paramId) {
        if (this.configurableComponent == null) {
            return null;
        }
        final GwtConfigParameter p = this.configurableComponent.getParameter(paramId);
        if (p == null) {
            return null;
        }
        return p.getValue() != null ? p.getValue() : p.getDefault();
    }

    /** Pushes a new language onto every code editor whose marker follows the given controlling parameter. */
    private void updateDynamicEditors(final String controllingParamId, final String language) {
        final List<CodeEditorTextArea> editors = this.dynamicLangEditors.get(controllingParamId);
        if (editors == null) {
            return;
        }
        for (final CodeEditorTextArea editor : editors) {
            editor.applyLanguageValue(language);
        }
    }

    private static String emptyToNull(final String value) {
        return value == null || value.isEmpty() ? null : value;
    }

    private boolean isTextArea(final GwtConfigParameter param) {
        if (param == null) {
            return false;
        }

        if (param.getType() != GwtConfigParameterType.STRING) {
            return false;
        }

        final String description = param.getDescription();

        if (description == null) {
            return false;
        }

        final String[] result = splitDescription(description);
        if (result.length < 2 || result[1] == null) {
            return false;
        }

        return result[1].equalsIgnoreCase("TextArea");
    }

    private String getDescription(final GwtConfigParameter param) {
        if (param == null || param.getDescription() == null) {
            return null;
        }

        final String[] result = splitDescription(param.getDescription());
        if (result.length > 0) {
            return result[0];
        }
        return "";
    }

    private static String[] splitDescription(final String description) {
        final int idx = description.lastIndexOf('|');
        if (idx < 0) {
            return new String[] { description };
        }
        if (idx < 1) {
            return new String[] { "", description.substring(idx + 1) };
        }
        return new String[] { description.substring(0, idx), description.substring(idx + 1) };
    }

    private ExtendedTextArea createTextArea() {
        final ExtendedTextArea textArea = new ExtendedTextArea();
        textArea.setVisibleLines(10);
        textArea.setCharacterWidth(120);
        return textArea;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected void renderPasswordField(final GwtConfigParameter param, boolean isFirstInstance, FormGroup formGroup) {
        this.valid.put(param.getId(), true);

        if (isFirstInstance) {
            FormLabel formLabel = new FormLabel();
            formLabel.setText(param.getName());
            if (param.isRequired()) {
                formLabel.setShowRequiredIndicator(true);
            }
            formLabel.setTitle(param.getId());
            formGroup.add(formLabel);

            InlineHelpBlock ihb = new InlineHelpBlock();
            ihb.setIconType(IconType.EXCLAMATION_TRIANGLE);
            formGroup.add(ihb);

            if (param.getDescription() != null) {
                HelpBlock toolTip = new HelpBlock();
                toolTip.setText(getDescription(param));
                formGroup.add(toolTip);
            }
        }

        final NewPasswordInputForm input = new NewPasswordInputForm();

        if (param.getValue() != null) {
            input.setInputPasswordText(param.getValue());
        } else {
            input.setInputPasswordText("");
        }

        input.setShowPasswordButtonEnabled(!PLACEHOLDER.equals(input.getInputPasswordText()));

        input.setInputPasswordClickHandler(handler -> {
            if (input.isInputPasswordEnabled() && input.getInputPasswordText().equals(PLACEHOLDER)) {
                input.setInputPasswordText("");
                input.validateInputPassword();
                input.setShowPasswordButtonEnabled(true);
            }
        });

        if (param.getMin() != null && param.getMin().equals(param.getMax())) {
            input.setInputPasswordReadOnly(true);
            input.setInputPasswordEnabled(false);
        }

        input.addInputPasswordValidator(new Validator() {

            @Override
            public List<EditorError> validate(Editor editor, Object value) {

                List<EditorError> result = new ArrayList<>();
                if ((input.getInputPasswordText() == null || "".equals(input.getInputPasswordText().trim()))
                        && param.isRequired()) {
                    // null in required field
                    result.add(new BasicEditorError(input, input.getInputPasswordText(), MSGS.formRequiredParameter()));
                    AbstractServicesUi.this.valid.put(param.getId(), false);
                } else {
                    param.setValue(input.getInputPasswordText());
                    AbstractServicesUi.this.valid.put(param.getId(), true);
                }

                return result;
            }

            @Override
            public int getPriority() {
                return 0;
            }
        });

        input.setInputPasswordKeyUpHandler(event -> {
            input.validateInputPassword(true);
            setDirty(true);
        });

        formGroup.add(input);

        input.validateInputPassword(true);
    }

    protected void renderBooleanField(final GwtConfigParameter param, boolean isFirstInstance, FormGroup formGroup) {
        this.valid.put(param.getId(), true);

        if (isFirstInstance) {
            FormLabel formLabel = new FormLabel();
            formLabel.setText(param.getName());
            if (param.isRequired()) {
                formLabel.setShowRequiredIndicator(true);
            }
            formLabel.setTitle(param.getId());
            formGroup.add(formLabel);

            if (param.getDescription() != null) {
                HelpBlock toolTip = new HelpBlock();
                toolTip.setText(getDescription(param));
                formGroup.add(toolTip);
            }
        }

        FlowPanel flowPanel = new FlowPanel();

        InlineRadio radioTrue = new InlineRadio(param.getName());
        radioTrue.setText(MSGS.trueLabel());
        radioTrue.setFormValue("true");

        InlineRadio radioFalse = new InlineRadio(param.getName());
        radioFalse.setText(MSGS.falseLabel());
        radioFalse.setFormValue("false");

        radioTrue.setValue(Boolean.parseBoolean(param.getValue()));
        radioFalse.setValue(!Boolean.parseBoolean(param.getValue()));

        if (param.getMin() != null && param.getMin().equals(param.getMax())) {
            radioTrue.setEnabled(false);
            radioFalse.setEnabled(false);
        }

        flowPanel.add(radioTrue);
        flowPanel.add(radioFalse);

        radioTrue.addValueChangeHandler(event -> {

            InlineRadio box = (InlineRadio) event.getSource();
            if (box.getValue()) {
                param.setValue(String.valueOf(true));
            }
            setDirty(true);
        });

        radioFalse.addValueChangeHandler(event -> {
            InlineRadio box = (InlineRadio) event.getSource();
            if (box.getValue()) {
                param.setValue(String.valueOf(false));
            }
            setDirty(true);
        });

        formGroup.add(flowPanel);
    }

    protected void renderChoiceField(final GwtConfigParameter param, boolean isFirstInstance, FormGroup formGroup) {
        this.valid.put(param.getId(), true);

        if (isFirstInstance) {
            FormLabel formLabel = new FormLabel();
            formLabel.setText(param.getName());
            if (param.isRequired()) {
                formLabel.setShowRequiredIndicator(true);
            }
            formLabel.setTitle(param.getId());
            formGroup.add(formLabel);

            if (param.getDescription() != null) {
                HelpBlock toolTip = new HelpBlock();
                toolTip.setText(getDescription(param));
                formGroup.add(toolTip);
            }
        }

        ListBox listBox = new ListBox();

        int i = 0;
        Map<String, String> oMap = param.getOptions();

        ArrayList<Entry<String, String>> sortedOptions = new ArrayList<>(oMap.entrySet());
        Collections.sort(sortedOptions, DROPDOWN_LABEL_COMPARATOR);

        final String selection = param.getValue() != null ? param.getValue() : param.getDefault();

        for (Entry<String, String> current : sortedOptions) {
            String label = current.getKey();
            String value = current.getValue();

            listBox.addItem(label, value);

            if (value.equals(selection)) {
                listBox.setSelectedIndex(i);
            }

            i++;
        }

        listBox.addChangeHandler(event -> {
            ListBox box = (ListBox) event.getSource();
            param.setValue(box.getSelectedValue());
            setDirty(true);
            updateDynamicEditors(param.getId(), box.getSelectedValue());
        });

        formGroup.add(listBox);
    }

    protected List<EditorError> validateTextBox(final GwtConfigParameter param, final FormGroup group) {

        group.setValidationState(ValidationState.NONE);

        final List<EditorError> editorErrors = new ArrayList<>();

        this.valid.put(param.getId(), true);

        int widgetCount = group.getWidgetCount();
        for (int i = 0; i < widgetCount; i++) {
            Widget widget = group.getWidget(i);
            if (!(widget instanceof TextBoxBase)) {
                continue;
            }

            final TextBoxBase currentText = (TextBoxBase) widget;

            final String text = currentText.getText();

            if ((text == null || text.isEmpty()) && !param.isRequired()) {
                return editorErrors;
            }
            ValidationUtil.validateParameter(param, text, errorDescription -> {
                AbstractServicesUi.this.valid.put(param.getId(), false);
                editorErrors.add(new BasicEditorError(currentText, text, errorDescription));
            });

        }

        return editorErrors;
    }

    protected void fillUpdatedConfiguration(FormGroup fg) {
        List<String> multiFieldValues = new ArrayList<>();

        final GwtConfigParameter param = fillUpdatedConfigurationInternal(fg, null, multiFieldValues);

        if (!multiFieldValues.isEmpty() && param != null) {
            param.setValues(multiFieldValues.toArray(new String[] {}));
        }
    }

    private GwtConfigParameter fillUpdatedConfigurationInternal(final Panel fg, GwtConfigParameter target,
            List<String> multiFieldValues) {
        for (final Widget w : fg) {
            logger.fine("Widget: " + w.getClass());

            if (w instanceof FormLabel) {
                target = this.configurableComponent.getParameter(w.getTitle());
            } else if (w instanceof Panel) {
                target = fillUpdatedConfigurationInternal((Panel) w, target, multiFieldValues);
            } else if (w instanceof ListBox || w instanceof Input || w instanceof TextBoxBase) {

                if (target == null) {
                    errorLogger.warning("Missing parameter");
                    continue;
                }
                String value = getUpdatedFieldConfiguration(target, w);
                if (value == null) {
                    continue;
                }
                if (target.getCardinality() == 0 || target.getCardinality() == 1 || target.getCardinality() == -1) {
                    target.setValue(value);
                } else {
                    multiFieldValues.add(value);
                }
            }
        }

        return target;
    }

    protected void restoreConfiguration(GwtConfigComponent originalConfig) {
        this.configurableComponent = new GwtConfigComponent();
        this.configurableComponent.setComponentDescription(originalConfig.getComponentDescription());
        this.configurableComponent.setComponentIcon(originalConfig.getComponentIcon());
        this.configurableComponent.setComponentId(originalConfig.getComponentId());
        this.configurableComponent.setComponentName(originalConfig.getComponentName());

        List<GwtConfigParameter> originalParameters = new ArrayList<>();
        for (GwtConfigParameter parameter : originalConfig.getParameters()) {
            GwtConfigParameter tempParam = new GwtConfigParameter(parameter);
            originalParameters.add(tempParam);
        }
        this.configurableComponent.setParameters(originalParameters);

        Map<String, Object> originalProperties = new HashMap<>();
        originalProperties.putAll(originalConfig.getProperties());
        this.configurableComponent.setProperties(originalProperties);
    }

    private String getUpdatedFieldConfiguration(GwtConfigParameter param, Widget wg) {
        Map<String, String> options = param.getOptions();
        if (options != null && !options.isEmpty()) {
            Map<String, String> oMap = param.getOptions();
            if (wg instanceof ListBox) {
                return oMap.get(((ListBox) wg).getSelectedItemText());
            } else {
                return null;
            }
        } else {
            switch (param.getType()) {
            case BOOLEAN:
                return param.getValue();
            case LONG:
            case DOUBLE:
            case FLOAT:
            case SHORT:
            case BYTE:
            case INTEGER:
            case CHAR:
            case STRING:
                TextBoxBase tb = (TextBoxBase) wg;
                String value = tb.getText();
                if (tb instanceof KuraTextBox) {
                    value = ((KuraTextBox) tb).getData();
                }
                if (value != null) {
                    return value.trim();
                } else {
                    return null;
                }
            case PASSWORD:
                return wg instanceof Input ? ((Input) wg).getValue() : null;
            default:
                break;
            }
        }
        return null;
    }

}
