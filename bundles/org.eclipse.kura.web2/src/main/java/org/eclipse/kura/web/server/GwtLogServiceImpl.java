/*******************************************************************************
 * Copyright (c) 2021, 2024 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.web.server;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.log.LogEntry;
import org.eclipse.kura.log.LogProvider;
import org.eclipse.kura.system.SystemService;
import org.eclipse.kura.web.server.util.ServiceLocator;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtLogEntry;
import org.eclipse.kura.web.shared.model.GwtLogTail;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtLogService;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GwtLogServiceImpl extends OsgiRemoteServiceServlet implements GwtLogService {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(GwtLogServiceImpl.class);

    private static final LogEntriesCache cache = new LogEntriesCache();
    private static final List<String> registeredLogProviders = new LinkedList<>();

    @Override
    public List<String> initLogProviders(GwtXSRFToken xsrfToken) throws GwtKuraException {
        checkXSRFToken(xsrfToken);

        loadLogProviders();

        return registeredLogProviders;
    }

    @Override
    public List<GwtLogEntry> readLogs(int fromId) throws GwtKuraException {
        return cache.getLogs(fromId);
    }

    private static final String FS_LOG_PROVIDER_PID = "org.eclipse.kura.log.filesystem.provider.FilesystemLogProvider";
    private static final String LOG_FILE_PATH_PROP = "logFilePath";
    private static final String DEFAULT_LOG_FILE = "/var/log/kura.log";
    // Caps so a huge/rotated file or a burst never produces an unbounded payload; the byte cursor just continues.
    private static final long MAX_TAIL_BYTES = 2L * 1024 * 1024;
    private static final long MAX_READ_BYTES = 1L * 1024 * 1024;

    @Override
    public GwtLogTail readLogFileTail(int maxLines) throws GwtKuraException {
        final GwtLogTail tail = new GwtLogTail();
        final File file = new File(resolveLogFilePath());

        if (!file.isFile() || !file.canRead()) {
            tail.setFileAvailable(false);
            return tail;
        }

        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            tail.setFileAvailable(true);
            final long length = raf.length();
            tail.setLines(maxLines > 0 ? readLastLines(raf, length, maxLines) : Collections.emptyList());
            tail.setPosition(length);
        } catch (final IOException e) {
            logger.warn("Failed to read log file tail.", e);
            tail.setFileAvailable(false);
        }
        return tail;
    }

    @Override
    public GwtLogTail readLogFileSince(long position, int maxLines) throws GwtKuraException {
        final GwtLogTail tail = new GwtLogTail();
        final File file = new File(resolveLogFilePath());

        if (!file.isFile() || !file.canRead()) {
            tail.setFileAvailable(false);
            return tail;
        }

        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            tail.setFileAvailable(true);
            final long length = raf.length();

            if (position > length) {
                // file rotated or truncated: restart from a fresh tail
                tail.setReset(true);
                tail.setLines(maxLines > 0 ? readLastLines(raf, length, maxLines) : Collections.emptyList());
                tail.setPosition(length);
                return tail;
            }

            if (position >= length) {
                tail.setPosition(length);
                return tail;
            }

            raf.seek(position);
            final int toRead = (int) Math.min(length - position, MAX_READ_BYTES);
            final byte[] buffer = new byte[toRead];
            raf.readFully(buffer);

            int lastNewline = -1;
            for (int i = buffer.length - 1; i >= 0; i--) {
                if (buffer[i] == '\n') {
                    lastNewline = i;
                    break;
                }
            }
            if (lastNewline < 0) {
                // no complete line yet; wait for more without advancing
                tail.setPosition(position);
                return tail;
            }

            final String text = new String(buffer, 0, lastNewline, StandardCharsets.UTF_8);
            tail.setLines(stripCarriageReturns(new ArrayList<>(Arrays.asList(text.split("\n", -1)))));
            tail.setPosition(position + lastNewline + 1);
        } catch (final IOException e) {
            logger.warn("Failed to read log file.", e);
            tail.setFileAvailable(false);
        }
        return tail;
    }

    private static List<String> readLastLines(final RandomAccessFile raf, final long length, final int maxLines)
            throws IOException {
        if (length == 0) {
            return Collections.emptyList();
        }
        final long window = Math.min(length, MAX_TAIL_BYTES);
        final long start = length - window;
        raf.seek(start);
        final byte[] buffer = new byte[(int) window];
        raf.readFully(buffer);

        String text = new String(buffer, StandardCharsets.UTF_8);
        // if the window doesn't start at the file beginning, drop the leading (likely partial) line
        if (start > 0) {
            final int firstNewline = text.indexOf('\n');
            text = firstNewline >= 0 ? text.substring(firstNewline + 1) : "";
        }

        final List<String> all = new ArrayList<>(Arrays.asList(text.split("\n", -1)));
        if (!all.isEmpty() && all.get(all.size() - 1).isEmpty()) {
            all.remove(all.size() - 1); // trailing newline
        }
        final List<String> last = all.size() > maxLines ? all.subList(all.size() - maxLines, all.size()) : all;
        return stripCarriageReturns(new ArrayList<>(last));
    }

    private static List<String> stripCarriageReturns(final List<String> lines) {
        for (int i = 0; i < lines.size(); i++) {
            final String line = lines.get(i);
            if (line.endsWith("\r")) {
                lines.set(i, line.substring(0, line.length() - 1));
            }
        }
        return lines;
    }

    private String resolveLogFilePath() {
        try {
            final ConfigurationService cs = ServiceLocator.getInstance().getService(ConfigurationService.class);
            if (cs != null) {
                final ComponentConfiguration cc = cs.getComponentConfiguration(FS_LOG_PROVIDER_PID);
                if (cc != null && cc.getConfigurationProperties() != null) {
                    final Object path = cc.getConfigurationProperties().get(LOG_FILE_PATH_PROP);
                    if (path instanceof String && !((String) path).trim().isEmpty()) {
                        return ((String) path).trim();
                    }
                }
            }
        } catch (final Exception e) {
            logger.debug("Could not resolve FilesystemLogProvider logFilePath, using default.", e);
        }
        return DEFAULT_LOG_FILE;
    }

    private void loadLogProviders() {
        try {
            List<String> availableLogProviders = new ArrayList<>();

            final String MATCH_EVERYTHING = "(objectClass=*)";
            List<ServiceReference<LogProvider>> logProviderRefs = (List<ServiceReference<LogProvider>>) ServiceLocator
                    .getInstance().getServiceReferences(LogProvider.class, MATCH_EVERYTHING);

            for (ServiceReference<LogProvider> logProviderRef : logProviderRefs) {
                String pid = (String) logProviderRef.getProperty(ConfigurationService.KURA_SERVICE_PID);
                LogProvider service = FrameworkUtil.getBundle(LogProvider.class).getBundleContext()
                        .getService(logProviderRef);

                availableLogProviders.add(pid);

                if (pid != null && service != null && !registeredLogProviders.contains(pid)) {

                    service.registerLogListener((LogEntry entry) -> {
                        GwtLogEntry gwtEntry = new GwtLogEntry();
                        gwtEntry.setProperties(entry.getProperties());
                        gwtEntry.setSourceLogProviderPid(pid);
                        gwtEntry.setTimestamp(getFormattedTimestamp(entry));

                        GwtLogServiceImpl.cache.add(gwtEntry);
                    });

                    registeredLogProviders.add(pid);
                    logger.info("LogProvider {} loaded.", pid);
                }
            }

            // Collect-then-remove: mutating registeredLogProviders inside the for-each previously risked a
            // ConcurrentModificationException.
            final List<String> noLongerAvailable = new ArrayList<>();
            for (String pid : registeredLogProviders) {
                if (!availableLogProviders.contains(pid)) {
                    noLongerAvailable.add(pid);
                }
            }
            for (String pid : noLongerAvailable) {
                registeredLogProviders.remove(pid);
                logger.info("LogProvider {} no more available.", pid);
            }

            Optional<String> defaultLogManager = getDefaultLogManager();
            if (defaultLogManager.isPresent() && !registeredLogProviders.isEmpty()
                    && !registeredLogProviders.get(0).equals(defaultLogManager.get())) {
                String logManager = defaultLogManager.get();
                if (registeredLogProviders.contains(logManager)) {
                    registeredLogProviders.remove(logManager);
                    registeredLogProviders.add(0, logManager);
                }
            }
        } catch (GwtKuraException e) {
            logger.error("Error loading log providers.");
        }
    }

    private String getFormattedTimestamp(LogEntry entry) {
        String time = "UNDEFINED";
        try {
            return Instant.ofEpochSecond(entry.getTimestamp()).atZone(ZoneId.systemDefault()).toLocalDateTime()
                    .toString();
        } catch (Exception ex) {
            return time;
        }
    }

    private Optional<String> getDefaultLogManager() {
        Optional<String> defaultLogManager = Optional.empty();
        try {
            SystemService systemService = ServiceLocator.getInstance().getService(SystemService.class);
            if (systemService != null) {
                defaultLogManager = systemService.getDefaultLogManager();
            }
        } catch (GwtKuraException e) {
            logger.error("Error retrieving default LogManager name", e);
        }
        return defaultLogManager;
    }

    private static final class LogEntriesCache {

        private static final LinkedList<GwtLogEntry> cache = new LinkedList<>();
        private static final int MAX_CACHE_SIZE = 1000;
        private static int nextEntryId = 0;

        public void add(GwtLogEntry newEntry) {
            synchronized (cache) {
                if (cache.size() >= MAX_CACHE_SIZE) {
                    cache.removeFirst();
                }
                manageIdIntOverflow();
                newEntry.setId(nextEntryId++);
                cache.add(newEntry);
            }
        }

        public List<GwtLogEntry> getLogs(int fromId) {
            List<GwtLogEntry> result = new LinkedList<>();
            synchronized (cache) {
                cache.forEach(entry -> {
                    if (entry.getId() > fromId) {
                        result.add(entry);
                    }
                });
            }
            return result;
        }

        /*
         * Very unlikely to happen, but if it will then entries are reindexed
         */
        private static void manageIdIntOverflow() {
            if (nextEntryId >= Integer.MAX_VALUE) {
                logger.info("ID overflow for cached UI log entries. Reindexing.");

                for (int i = 0; i < cache.size(); i++) {
                    cache.get(i).setId(i);
                }
                nextEntryId = cache.size();
            }
        }
    }
}
