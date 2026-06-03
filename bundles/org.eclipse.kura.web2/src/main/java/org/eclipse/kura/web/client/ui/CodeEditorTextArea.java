/*******************************************************************************
 * Copyright (c) 2026 YOFC
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.kura.web.client.ui;

import java.util.List;

import org.eclipse.kura.web.shared.model.GwtScriptValidationError;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;

/**
 * TextArea-compatible widget that renders an ACE code editor instead of a
 * plain textarea, while keeping a hidden {@code <textarea>} backing element
 * so all the existing {@link AbstractServicesUi} plumbing (setText / getValue
 * / addValueChangeHandler / addValidator) keeps working unchanged.
 *
 * <h2>Lifecycle</h2>
 * <ol>
 *   <li>Constructor: stores the language mode, sets large visible-lines /
 *       character-width on the underlying TextArea so the fallback (plain
 *       textarea when ACE doesn't load) is at least usable.</li>
 *   <li>{@code onLoad}: hides the underlying {@code <textarea>}, inserts a
 *       {@code <div>} next to it, calls {@code ace.edit(div)}, sets the
 *       mode/theme, copies the current backing value into the editor, and
 *       wires a {@code change} listener that mirrors back into the textarea
 *       (firing GWT {@code ValueChangeEvent} so validators / dirty-state
 *       tracking still run).</li>
 *   <li>{@code onUnload}: destroys the ACE editor and removes the div.</li>
 * </ol>
 *
 * <h2>Implementation notes</h2>
 * Uses JSNI (legacy {@code native .../*-{ ... }-*\/}) instead of JsInterop
 * because Eclipse PDE's bundle classpath doesn't expose
 * {@code jsinterop.annotations} from the embedded GWT dependency. JSNI ships
 * with every GWT version since 1.x and has no extra deps.
 */
public class CodeEditorTextArea extends ExtendedTextArea {

    private String mode;
    private String languagePrefix = "";
    private JavaScriptObject editor;
    private DivElement editorDiv;
    private boolean syncingFromEditor;

    /**
     * @param mode short language id matching an ACE mode file present under
     *        {@code /admin/ace/}, e.g. {@code "java"}, {@code "groovy"},
     *        {@code "xml"}, {@code "yaml"}.
     */
    public CodeEditorTextArea(final String mode) {
        super();
        this.mode = mode;
        // Generous defaults in case ACE never inits and we fall back to the
        // plain textarea.
        setVisibleLines(25);
        setCharacterWidth(120);
    }

    @Override
    protected void onLoad() {
        super.onLoad();
        if (this.editor != null) {
            return;
        }
        if (!isAceLoaded()) {
            GWT.log("ACE not loaded (window.ace undefined) — falling back to plain textarea");
            return;
        }

        // Hide the underlying <textarea>; we still need it in the DOM so all
        // the existing TextBoxBase behaviors (getValue/setValue/validators)
        // work, but the user shouldn't see two editors.
        getElement().getStyle().setDisplay(Style.Display.NONE);

        // Build an empty <div> next to the textarea for ACE to render into.
        this.editorDiv = Document.get().createDivElement();
        this.editorDiv.setClassName("kura-code-editor");
        final Style style = this.editorDiv.getStyle();
        style.setHeight(500, Style.Unit.PX);
        style.setWidth(100, Style.Unit.PCT);
        style.setProperty("border", "1px solid #ccc");
        style.setProperty("borderRadius", "3px");

        final Element parent = getElement().getParentElement();
        if (parent == null) {
            GWT.log("CodeEditorTextArea: no parent yet, skipping editor init");
            return;
        }
        parent.insertAfter(this.editorDiv, getElement());

        // ace.edit must run after the div is in the DOM — defer one tick.
        Scheduler.get().scheduleDeferred(() -> {
            try {
                final String current = getValue();
                this.editor = aceInit(this.editorDiv, toAceMode(this.mode), current == null ? "" : current);
            } catch (final Exception e) {
                GWT.log("Failed to initialize ACE editor", e);
                // Restore textarea visibility on init failure.
                getElement().getStyle().clearDisplay();
                if (this.editorDiv != null) {
                    this.editorDiv.removeFromParent();
                    this.editorDiv = null;
                }
            }
        });
    }

    @Override
    protected void onUnload() {
        try {
            if (this.editor != null) {
                aceDestroy(this.editor);
                this.editor = null;
            }
        } catch (final Exception e) {
            GWT.log("Failed to destroy ACE editor cleanly", e);
        }
        if (this.editorDiv != null) {
            this.editorDiv.removeFromParent();
            this.editorDiv = null;
        }
        super.onUnload();
    }

    @Override
    public void setText(final String text) {
        super.setText(text);
        pushToEditor();
    }

    @Override
    public void setValue(final String value) {
        super.setValue(value);
        pushToEditor();
    }

    @Override
    public void setValue(final String value, final boolean fireEvents) {
        super.setValue(value, fireEvents);
        pushToEditor();
    }

    @Override
    public void setReadOnly(final boolean readOnly) {
        super.setReadOnly(readOnly);
        if (this.editor != null) {
            aceSetReadOnly(this.editor, readOnly);
        }
    }

    /**
     * @return the ACE language mode this editor was created with (e.g. {@code "groovy"}, {@code "js"}). Used as the
     *         language id when requesting backend script validation.
     */
    public String getMode() {
        return this.mode;
    }

    /**
     * Switches the editor's syntax-highlighting language at runtime. Used when the language is driven by another
     * configuration field (a {@code |Editor:$<param>} / {@code |code} marker) so the highlight — and the language the
     * "Validate" button sends to the backend — follow the user's current selection.
     *
     * @param mode the ACE language id; {@code null}/empty falls back to plain text
     */
    public void setMode(final String mode) {
        this.mode = (mode == null || mode.trim().isEmpty()) ? "text" : mode.trim().toLowerCase();
        if (this.editor != null) {
            aceSetMode(this.editor, toAceMode(this.mode));
        }
    }

    /**
     * Sets a literal prefix prepended to the value supplied by {@link #applyLanguageValue(String)}. Used when the
     * marker is e.g. {@code camel-$file.extension}: the prefix is {@code camel-} so a selected value {@code xml}
     * becomes the language {@code camel-xml}.
     */
    public void setLanguagePrefix(final String languagePrefix) {
        this.languagePrefix = languagePrefix == null ? "" : languagePrefix;
    }

    /**
     * Applies a controlling field's current value as the language, prepending the configured {@link #languagePrefix}.
     * Called when that field changes so highlight and validation follow the selection.
     */
    public void applyLanguageValue(final String value) {
        setMode(this.languagePrefix + (value == null ? "" : value));
    }

    /**
     * Maps a backend/script language id to the matching ACE highlight mode. {@link #getMode()} keeps returning the raw
     * id (what the validator wants, e.g. {@code js}); only the highlight uses the ACE mode name (e.g. {@code
     * javascript}, for which the {@code ace/mode/javascript} file exists).
     */
    private static String toAceMode(final String language) {
        if (language == null) {
            return "text";
        }
        switch (language.toLowerCase()) {
        case "js":
        case "ecmascript":
            return "javascript";
        case "py":
            return "python";
        case "camel-java":
            return "java";
        case "camel-xml":
            return "xml";
        case "camel-yaml":
            return "yaml";
        default:
            return language.toLowerCase();
        }
    }

    /**
     * Shows the given validation errors as inline gutter annotations on the matching lines. No-op when ACE failed to
     * load (plain-textarea fallback).
     *
     * @param errors
     *            the problems to annotate; backend line/column numbers are 1-based, ACE expects 0-based
     */
    public void setAnnotations(final List<GwtScriptValidationError> errors) {
        if (this.editor == null) {
            return;
        }
        final JsArray<JavaScriptObject> annotations = JavaScriptObject.createArray().cast();
        for (final GwtScriptValidationError error : errors) {
            final int row = error.getLine() > 0 ? error.getLine() - 1 : 0;
            final int column = error.getColumn() > 0 ? error.getColumn() - 1 : 0;
            final String severity = error.getSeverity() == null ? "error" : error.getSeverity();
            annotations.push(makeAnnotation(row, column, error.getMessage(), severity));
        }
        aceSetAnnotations(this.editor, annotations);
    }

    /**
     * @return the live editor content. Reads straight from ACE when it is active, so validation sees the latest
     *         keystrokes even if the hidden backing textarea mirror lagged; falls back to the textarea value when ACE
     *         never loaded (plain-textarea fallback).
     */
    public String getEditorValue() {
        if (this.editor != null) {
            return aceGetValue(this.editor);
        }
        return getValue();
    }

    /** Clears any inline gutter annotations previously set by {@link #setAnnotations(List)}. */
    public void clearAnnotations() {
        if (this.editor != null) {
            aceClearAnnotations(this.editor);
        }
    }

    /** Push the textarea's current value into ACE if they've diverged. */
    private void pushToEditor() {
        if (this.editor == null || this.syncingFromEditor) {
            return;
        }
        final String value = getValue() == null ? "" : getValue();
        if (!value.equals(aceGetValue(this.editor))) {
            aceSetValue(this.editor, value);
        }
    }

    /**
     * Called from JSNI when the editor content changes. Mirrors the new value
     * into the backing textarea so validators / dirty-state see it.
     */
    @SuppressWarnings("unused") // Invoked from JSNI in aceInit
    private void onEditorChange(final String newValue) {
        if (this.syncingFromEditor) {
            return;
        }
        this.syncingFromEditor = true;
        try {
            // fireEvents=true so addValueChangeHandler subscribers (dirty
            // tracking, validators) see the edit.
            setValue(newValue, true);
        } finally {
            this.syncingFromEditor = false;
        }
    }

    // ---------------------------------------------------------------- JSNI ---

    private static native boolean isAceLoaded() /*-{
        return !!$wnd.ace;
    }-*/;

    private native JavaScriptObject aceInit(Element div, String mode, String initialValue) /*-{
        var self = this;
        var editor = $wnd.ace.edit(div);
        editor.getSession().setMode("ace/mode/" + mode);
        editor.setTheme("ace/theme/chrome");
        editor.getSession().setUseSoftTabs(true);
        editor.getSession().setTabSize(4);
        editor.setValue(initialValue, -1);
        editor.on("change", function () {
            // Bridge back to Java; method name is mangled, use the JSNI ref.
            self.@org.eclipse.kura.web.client.ui.CodeEditorTextArea::onEditorChange(Ljava/lang/String;)(editor.getValue());
        });
        return editor;
    }-*/;

    private static native String aceGetValue(JavaScriptObject editor) /*-{
        return editor.getValue();
    }-*/;

    private static native void aceSetValue(JavaScriptObject editor, String value) /*-{
        editor.setValue(value, -1);
    }-*/;

    private static native void aceSetReadOnly(JavaScriptObject editor, boolean readOnly) /*-{
        editor.setReadOnly(readOnly);
    }-*/;

    private static native void aceDestroy(JavaScriptObject editor) /*-{
        editor.destroy();
    }-*/;

    private static native JavaScriptObject makeAnnotation(int row, int column, String text, String type) /*-{
        return { row: row, column: column, text: text, type: type };
    }-*/;

    private static native void aceSetAnnotations(JavaScriptObject editor, JavaScriptObject annotations) /*-{
        editor.getSession().setAnnotations(annotations);
    }-*/;

    private static native void aceClearAnnotations(JavaScriptObject editor) /*-{
        editor.getSession().clearAnnotations();
    }-*/;

    private static native void aceSetMode(JavaScriptObject editor, String mode) /*-{
        editor.getSession().setMode("ace/mode/" + mode);
    }-*/;
}
