/*******************************************************************************
 * Copyright (c) 2026 YOFC
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.kura.web.client.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
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

    private final String mode;
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
                this.editor = aceInit(this.editorDiv, this.mode, current == null ? "" : current);
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
}
