/*******************************************************************************
 * Copyright (c) 2021 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.web.shared.service;

import java.util.List;

import org.eclipse.kura.web.server.RequiredPermissions;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.KuraPermission;
import org.eclipse.kura.web.shared.model.GwtLogEntry;
import org.eclipse.kura.web.shared.model.GwtLogTail;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("logservice")
@RequiredPermissions(KuraPermission.DEVICE)
public interface GwtLogService extends RemoteService {

    public List<String> initLogProviders(GwtXSRFToken xsrfToken) throws GwtKuraException;

    public List<GwtLogEntry> readLogs(int fromId) throws GwtKuraException;

    /**
     * Reads the last {@code maxLines} lines of the gateway log file (auto-following the FilesystemLogProvider's
     * configured path, falling back to {@code /var/log/kura.log}). Pass {@code maxLines <= 0} to fetch no history but
     * still get the current end-of-file {@code position} — used to follow "from now" right after clicking Apply.
     * <p>
     * No XSRF token (matching {@link #readLogs(int)}): this is a high-frequency polling path and the session is
     * already authenticated.
     */
    public GwtLogTail readLogFileTail(int maxLines) throws GwtKuraException;

    /**
     * Reads the log file lines appended after byte offset {@code position} and returns the new {@code position}. If the
     * file shrank/rotated, returns a fresh tail of {@code maxLines} with {@code reset = true}.
     */
    public GwtLogTail readLogFileSince(long position, int maxLines) throws GwtKuraException;

}
