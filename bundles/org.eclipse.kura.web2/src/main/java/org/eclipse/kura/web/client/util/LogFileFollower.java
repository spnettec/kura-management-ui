/*******************************************************************************
 * Copyright (c) 2026 YOFC
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.kura.web.client.util;

import java.util.List;

import org.eclipse.kura.web.shared.model.GwtLogTail;
import org.eclipse.kura.web.shared.service.GwtLogService;
import org.eclipse.kura.web.shared.service.GwtLogServiceAsync;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * Follows the gateway log file by byte offset. Each instance keeps its OWN cursor, so multiple consumers (the Logs
 * view, a per-Apply popup, ...) never interfere — unlike the shared-cursor {@link LogPollService}. Use
 * {@link #startFromTail(int, Listener)} to show the last N lines and follow, or {@link #startFromEnd(int, Listener)}
 * to follow only what is logged from now on (e.g. right after clicking Apply).
 */
public class LogFileFollower {

    private static final int POLL_DELAY = 1000;
    private static final int POLL_DELAY_IDLE = 2000;
    private static final int POLL_DELAY_ON_FAILURE = 3000;

    private final GwtLogServiceAsync gwtLogService = GWT.create(GwtLogService.class);

    private Timer timer;
    private long position;
    private int maxLines = 1000;
    private Listener listener;
    private boolean running;

    public interface Listener {

        /** New log lines. {@code reset} is {@code true} when they are a fresh tail (initial load or file rotation). */
        void onLines(List<String> lines, boolean reset);

        /** No readable log file (e.g. a console-only emulator); following stops. */
        void onUnavailable();
    }

    /** Shows the last {@code maxLines} lines, then follows. */
    public void startFromTail(final int maxLines, final Listener listener) {
        start(maxLines, listener, false);
    }

    /** Follows only lines logged after this call (no history) — for "watch what my Apply does". */
    public void startFromEnd(final int maxLines, final Listener listener) {
        start(maxLines, listener, true);
    }

    public void stop() {
        this.running = false;
        if (this.timer != null) {
            this.timer.cancel();
            this.timer = null;
        }
    }

    private void start(final int maxLines, final Listener listener, final boolean fromEnd) {
        this.maxLines = maxLines;
        this.listener = listener;
        this.running = true;

        this.gwtLogService.readLogFileTail(fromEnd ? 0 : maxLines, new AsyncCallback<GwtLogTail>() {

            @Override
            public void onSuccess(final GwtLogTail tail) {
                if (!LogFileFollower.this.running) {
                    return;
                }
                if (!tail.isFileAvailable()) {
                    listener.onUnavailable();
                    return;
                }
                LogFileFollower.this.position = tail.getPosition();
                if (!tail.getLines().isEmpty()) {
                    listener.onLines(tail.getLines(), true);
                }
                scheduleNext(POLL_DELAY);
            }

            @Override
            public void onFailure(final Throwable caught) {
                if (LogFileFollower.this.running) {
                    scheduleNext(POLL_DELAY_ON_FAILURE);
                }
            }
        });
    }

    private void scheduleNext(final int delay) {
        if (!this.running) {
            return;
        }
        if (this.timer != null) {
            this.timer.cancel();
        }
        this.timer = new Timer() {

            @Override
            public void run() {
                poll();
            }
        };
        this.timer.schedule(delay);
    }

    private void poll() {
        if (!this.running) {
            return;
        }
        this.gwtLogService.readLogFileSince(this.position, this.maxLines, new AsyncCallback<GwtLogTail>() {

            @Override
            public void onSuccess(final GwtLogTail tail) {
                if (!LogFileFollower.this.running) {
                    return;
                }
                if (!tail.isFileAvailable()) {
                    listener.onUnavailable();
                    stop();
                    return;
                }
                LogFileFollower.this.position = tail.getPosition();
                if (!tail.getLines().isEmpty()) {
                    listener.onLines(tail.getLines(), tail.isReset());
                    scheduleNext(POLL_DELAY);
                } else {
                    scheduleNext(POLL_DELAY_IDLE);
                }
            }

            @Override
            public void onFailure(final Throwable caught) {
                if (LogFileFollower.this.running) {
                    scheduleNext(POLL_DELAY_ON_FAILURE);
                }
            }
        });
    }
}
