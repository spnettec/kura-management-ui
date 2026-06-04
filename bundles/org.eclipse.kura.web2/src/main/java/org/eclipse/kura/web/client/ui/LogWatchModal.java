/*******************************************************************************
 * Copyright (c) 2026 YOFC
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.kura.web.client.ui;

import java.util.List;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.util.LogFileFollower;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.ModalBody;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.StyleInjector;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.ScrollPanel;

/**
 * A modal that scrolls the gateway log file in real time. Opened right after clicking Apply (via {@link #showFromNow()})
 * it follows only the lines produced from that moment on, so the user can immediately see what the just-applied
 * configuration did — without leaving the page or grepping the logs. Generic: works for any service, not just scripts.
 * <p>
 * Backed by {@link LogFileFollower} (its own byte-offset cursor), so it never interferes with the Logs page.
 */
public class LogWatchModal {

    private static final Messages MSGS = GWT.create(Messages.class);

    private static final int TAIL_LINES = 1000;
    private static final int MAX_DISPLAY_LINES = 3000;

    private static final String MODAL_STYLE = "kura-log-watch-modal";
    private static final String SCROLL_STYLE = "kura-log-watch-scroll";
    private static boolean cssInjected = false;

    // Make the popup user-resizable: drag the bottom-right corner of the modal box to change width AND height; the
    // log area flexes to fill it. Injected once.
    private static void injectResizableModalCss() {
        if (cssInjected) {
            return;
        }
        cssInjected = true;
        StyleInjector.inject("." + MODAL_STYLE + " .modal-dialog{width:70%;max-width:96vw;}"
                + "." + MODAL_STYLE + " .modal-content{resize:both;overflow:auto;display:flex;flex-direction:column;"
                + "height:60vh;min-width:420px;min-height:260px;max-width:96vw;max-height:92vh;}"
                + "." + MODAL_STYLE + " .modal-body{flex:1 1 auto;min-height:0;display:flex;padding:8px;}"
                + "." + SCROLL_STYLE + "{flex:1 1 auto;min-height:0;width:100%;}");
    }

    private final Modal modal = new Modal();
    private final FlowPanel logPanel = new FlowPanel();
    private final ScrollPanel scrollPanel = new ScrollPanel(this.logPanel);
    private final LogFileFollower follower = new LogFileFollower();

    public LogWatchModal() {
        injectResizableModalCss();

        this.modal.setTitle(MSGS.logWatchTitle());
        this.modal.setClosable(true);
        this.modal.addStyleName(MODAL_STYLE);

        this.scrollPanel.addStyleName(SCROLL_STYLE);
        this.scrollPanel.getElement().getStyle().setProperty("backgroundColor", "#1e1e1e");
        this.scrollPanel.getElement().getStyle().setProperty("padding", "8px");
        this.scrollPanel.getElement().getStyle().setProperty("borderRadius", "3px");
        this.scrollPanel.getElement().getStyle().setProperty("overflow", "auto");

        this.logPanel.getElement().getStyle().setProperty("fontFamily", "monospace");
        this.logPanel.getElement().getStyle().setProperty("fontSize", "12px");
        this.logPanel.getElement().getStyle().setProperty("color", "#d4d4d4");
        this.logPanel.getElement().getStyle().setProperty("whiteSpace", "pre-wrap");
        this.logPanel.getElement().getStyle().setProperty("wordBreak", "break-all");

        final ModalBody body = new ModalBody();
        body.add(this.scrollPanel);
        this.modal.add(body);

        this.modal.addHideHandler(event -> this.follower.stop());
    }

    /** Opens the popup and follows only the log lines produced from now on. */
    public void showFromNow() {
        this.logPanel.clear();
        this.modal.show();
        this.follower.startFromEnd(TAIL_LINES, this.listener);
    }

    /** Opens the popup showing the last {@value #TAIL_LINES} lines, then follows. */
    public void showTail() {
        this.logPanel.clear();
        this.modal.show();
        this.follower.startFromTail(TAIL_LINES, this.listener);
    }

    private final LogFileFollower.Listener listener = new LogFileFollower.Listener() {

        @Override
        public void onLines(final List<String> lines, final boolean reset) {
            if (reset) {
                LogWatchModal.this.logPanel.clear();
            }
            for (final String line : lines) {
                appendLine(line);
            }
            trimToMaxLines();
            LogWatchModal.this.scrollPanel.scrollToBottom();
        }

        @Override
        public void onUnavailable() {
            final HTML message = new HTML(SafeHtmlUtils.htmlEscape(MSGS.logWatchNoFile()));
            message.getElement().getStyle().setProperty("color", "#c0c0c0");
            LogWatchModal.this.logPanel.add(message);
        }
    };

    private void appendLine(final String line) {
        final HTML row = new HTML(SafeHtmlUtils.htmlEscape(line));
        final String upper = line.toUpperCase();
        if (upper.contains("ERROR")) {
            row.getElement().getStyle().setProperty("color", "#f48771");
        } else if (upper.contains("WARN")) {
            row.getElement().getStyle().setProperty("color", "#e0b341");
        }
        this.logPanel.add(row);
    }

    private void trimToMaxLines() {
        while (this.logPanel.getWidgetCount() > MAX_DISPLAY_LINES) {
            this.logPanel.remove(0);
        }
    }
}
