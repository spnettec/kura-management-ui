/*******************************************************************************
 * Copyright (c) 2026 YOFC
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.kura.web.shared.model;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * The outcome of validating (compiling) a script through the backend: either it compiled cleanly or it carries the
 * list of {@link GwtScriptValidationError}s describing why it did not.
 */
public class GwtScriptValidationResult implements IsSerializable {

    private boolean valid;
    private List<GwtScriptValidationError> errors;

    public GwtScriptValidationResult() {
        this.errors = new ArrayList<>();
    }

    public boolean isValid() {
        return this.valid;
    }

    public void setValid(final boolean valid) {
        this.valid = valid;
    }

    public List<GwtScriptValidationError> getErrors() {
        return this.errors;
    }

    public void setErrors(final List<GwtScriptValidationError> errors) {
        this.errors = errors;
    }
}
