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
 * A chunk of the gateway log file: the lines read and the byte {@code position} reached (the cursor a follower passes
 * back to keep reading). The cursor is a plain file offset, so each consumer keeps its own and there is no shared,
 * lossy cache to drift.
 */
public class GwtLogTail implements IsSerializable {

    private List<String> lines;
    private long position;
    /** {@code false} when there is no readable log file (e.g. a console-only emulator); the follower then stops. */
    private boolean fileAvailable;
    /** {@code true} when the file shrank/rotated and {@code lines} is a fresh tail rather than a continuation. */
    private boolean reset;

    public GwtLogTail() {
        this.lines = new ArrayList<>();
    }

    public List<String> getLines() {
        return this.lines;
    }

    public void setLines(final List<String> lines) {
        this.lines = lines;
    }

    public long getPosition() {
        return this.position;
    }

    public void setPosition(final long position) {
        this.position = position;
    }

    public boolean isFileAvailable() {
        return this.fileAvailable;
    }

    public void setFileAvailable(final boolean fileAvailable) {
        this.fileAvailable = fileAvailable;
    }

    public boolean isReset() {
        return this.reset;
    }

    public void setReset(final boolean reset) {
        this.reset = reset;
    }
}
