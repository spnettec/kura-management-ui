/*******************************************************************************
 * Copyright (c) 2026 YOFC
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.kura.web.shared.model;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * A single syntax / compilation problem reported by the backend script validator, carried back to the code editor so
 * it can be shown inline (gutter annotation) and in the status line.
 */
public class GwtScriptValidationError implements IsSerializable {

    private int line;
    private int column;
    private String message;
    private String severity;

    public GwtScriptValidationError() {
        // required by GWT-RPC serialization
    }

    public GwtScriptValidationError(final int line, final int column, final String message, final String severity) {
        this.line = line;
        this.column = column;
        this.message = message;
        this.severity = severity;
    }

    public int getLine() {
        return this.line;
    }

    public void setLine(final int line) {
        this.line = line;
    }

    public int getColumn() {
        return this.column;
    }

    public void setColumn(final int column) {
        this.column = column;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(final String message) {
        this.message = message;
    }

    public String getSeverity() {
        return this.severity;
    }

    public void setSeverity(final String severity) {
        this.severity = severity;
    }
}
