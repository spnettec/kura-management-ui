/*******************************************************************************
 * Copyright (c) 2026 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.client.ui;

import java.util.List;
import java.util.Optional;

import org.eclipse.kura.web.shared.model.GwtPasswordStrenghtRequirements;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.InputGroup;
import org.gwtbootstrap3.client.ui.InputGroupButton;
import org.gwtbootstrap3.client.ui.base.mixin.ErrorHandlerMixin;
import org.gwtbootstrap3.client.ui.constants.IconType;
import org.gwtbootstrap3.client.ui.constants.InputType;
import org.gwtbootstrap3.client.ui.form.validator.Validator;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.editor.client.EditorError;
import com.google.gwt.editor.client.HasEditorErrors;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.shared.HandlerRegistration;

public class NewPasswordInputForm extends InputGroup implements HasEditorErrors<String> {

    private final ErrorHandlerMixin<String> errorHandlerMixin = new ErrorHandlerMixin<String>(this);

    private final NewPasswordInput newPasswordInput;
    private final Button showPasswordButton;
    private final InputGroupButton showPasswordButtonGroup;

    /* Constructor */
    public NewPasswordInputForm() {
        this.newPasswordInput = initializePasswordInput();
        this.showPasswordButton = initializeShowPasswordButton();
        this.showPasswordButtonGroup = initializeShowPasswordButtonGroup();
        this.getElement().getStyle().setProperty("gap", "10px");
        this.add(this.showPasswordButtonGroup);
        this.add(this.newPasswordInput);
    }

    /* Items initialization */

    private NewPasswordInput initializePasswordInput() {
        NewPasswordInput input = new NewPasswordInput();
        input.setAllowPlaceholder(true);
        input.setType(InputType.PASSWORD);
        input.setId("password");
        input.setEnabled(true);
        input.setVisible(true);
        return input;
    }

    private Button initializeShowPasswordButton() {
        Button button = new Button();
        button.setIcon(IconType.EYE);
        button.setEnabled(true);
        button.setVisible(true);
        button.setIconFixedWidth(true);
        button.addClickHandler(handler -> {
            if (this.isShowPasswordButtonEnabled()) {
                if (this.getInputPasswordType().equals(InputType.PASSWORD)) {
                    this.setInputPasswordType(InputType.TEXT);
                    this.setShowPasswordButtonIcon(IconType.EYE_SLASH);
                } else {
                    this.setInputPasswordType(InputType.PASSWORD);
                    this.setShowPasswordButtonIcon(IconType.EYE);
                }
            }
        });
        return button;
    }

    private InputGroupButton initializeShowPasswordButtonGroup() {
        InputGroupButton groupButton = new InputGroupButton();
        groupButton.add(this.showPasswordButton);
        groupButton.getElement().getStyle().setPaddingRight(5, Unit.PX);
        return groupButton;
    }

    /* Public Utils */
    public boolean validateInputPassword() {
        return this.newPasswordInput.validate();
    }

    public boolean validateInputPassword(boolean show) {
        return this.newPasswordInput.validate(show);
    }

    public HandlerRegistration addChangeHandler(ChangeHandler handler) {
        return addDomHandler(handler, ChangeEvent.getType());
    }

    /* Public Password Input Getters */
    public String getInputPasswordText() {
        return this.newPasswordInput.getText();
    }

    public InputType getInputPasswordType() {
        return this.newPasswordInput.getType();
    }

    public boolean isInputPasswordEnabled() {
        return this.newPasswordInput.isEnabled();
    }

    public boolean isInputPasswordVisible() {
        return this.newPasswordInput.isVisible();
    }

    /* Public Show/Hide Password Button Getters */

    public boolean isShowPasswordButtonEnabled() {
        return this.showPasswordButton.isEnabled();
    }

    public boolean isShowPasswordButtonVisible() {
        return this.showPasswordButton.isVisible();
    }

    /* Show/Hide Password Button Customization */
    public void setShowPasswordButtonEnabled(boolean enabled) {
        this.showPasswordButton.setEnabled(enabled);
    }

    public void setShowPasswordButtonVisible(boolean visible) {
        this.showPasswordButton.setVisible(visible);
    }

    public void setShowPasswordButtonIcon(IconType icon) {
        this.showPasswordButton.setIcon(icon);
    }

    public void setShowPasswordButtonMouseOverHandler(MouseOverHandler handler) {
        this.showPasswordButton.addMouseOverHandler(handler);
    }

    /* Password Input Customization */
    public void setInputPasswordValue(String value) {
        this.newPasswordInput.setValue(value);
    }

    public void setInputPasswordText(String text) {
        this.newPasswordInput.setText(text);
    }

    public void setInputPasswordType(InputType type) {
        this.newPasswordInput.setType(type);
    }

    public void setInputPasswordEnabled(boolean enabled) {
        this.newPasswordInput.setEnabled(enabled);
    }

    public void setInputPasswordVisible(boolean visible) {
        this.newPasswordInput.setVisible(visible);
    }

    @SafeVarargs
    public final void setInputPasswordValidators(final Validator<String>... validators) {
        this.newPasswordInput.setValidators(validators);
    }

    public final void setInputPasswordValidatorsFrom(Optional<String> identityName,
            GwtPasswordStrenghtRequirements userOptions) {
        this.newPasswordInput.setValidatorsFrom(identityName, userOptions);
    }

    public void addInputPasswordValidator(Validator<String> validator) {
        this.newPasswordInput.addValidator(validator);
    }

    public void setInputPasswordClickHandler(ClickHandler handler) {
        this.newPasswordInput.addClickHandler(handler);
    }

    public void setInputPasswordMouseOverHandler(MouseOverHandler handler) {
        this.newPasswordInput.addMouseOverHandler(handler);
    }

    public void setInputPasswordMouseOutHandler(MouseOutHandler handler) {
        this.newPasswordInput.addMouseOutHandler(handler);
    }

    public void setInputPasswordChangeHandler(ChangeHandler handler) {
        this.newPasswordInput.addChangeHandler(handler);
    }

    public void setInputPasswordKeyUpHandler(KeyUpHandler handler) {
        this.newPasswordInput.addKeyUpHandler(handler);
    }

    public void setInputPasswordBlurHandler(BlurHandler handler) {
        this.newPasswordInput.addBlurHandler(handler);
    }

    public void setInputPasswordAllowBlank(boolean allowBlank) {
        this.newPasswordInput.setAllowBlank(allowBlank);
    }

    public void setInputPasswordReadOnly(boolean readOnly) {
        this.newPasswordInput.setReadOnly(readOnly);
    }

    public void setInputPasswordFocus(boolean focus) {
        this.newPasswordInput.setFocus(focus);
    }

    public void resetPasswordInput() {
        this.newPasswordInput.reset();
    }

    /** {@inheritDoc} */
    @Override
    public void showErrors(List<EditorError> errors) {
        errorHandlerMixin.showErrors(errors);
    }
}