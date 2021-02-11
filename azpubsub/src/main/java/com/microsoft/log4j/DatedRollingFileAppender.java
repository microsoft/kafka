/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.microsoft.log4j;

import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.helpers.OptionConverter;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.helpers.CountingQuietWriter;

public class DatedRollingFileAppender extends FileAppender {
    private final static String LINE_SEP_REGEX = Layout.LINE_SEP + "$";
    private final static String LINE_SEP_REPLACE_CHAR = "|";
    private final static String EXCEPTION_SEP = "###";

    /**
     * The date pattern. By default, the pattern is set to
     * "'.'yyyy-MM-dd" meaning daily roll over.
     */
    private final static String DATE_PATTERN = "'.'yyyy-MM-dd";

    /**
     *  The default maximum file size is 100MB.
     */
    private final static long DEFAULT_MAX_FILE_SIZE = 100 * 1024 * 1024;

    private final static SimpleDateFormat SDF_PATTERN = new SimpleDateFormat(DATE_PATTERN);

    /**
     * The next time we estimate a roll over should occur.
     */
    private long nextRolloverCheck = System.currentTimeMillis() - 1;

    /**
     *  The size based rolling is not enabled when the maximum file size is 0.
     */
    protected long maxFileSize = 0;

    /**
     * No log file cleanup by default.
     */
    protected int maxBackupIndex = 0;

    /**
     * The original name of the log file.
     */
    private String originalFileName = null;

    /**
     * The default constructor simply calls its {@link
     * FileAppender#FileAppender parents constructor}.
     */
    public DatedRollingFileAppender() {
        super();
    }

    /**
     * Instantiate a RollingFileAppender and open the file designated by
     * <code>filename</code>. The opened filename will become the output
     * destination for this appender.
     * <p>If the <code>append</code> parameter is true, the file will be
     * appended to. Otherwise, the file designated by
     * <code>filename</code> will be truncated before being opened.
     */
    public DatedRollingFileAppender(Layout layout, String filename, boolean append) throws IOException {
        super(layout, filename, append);
    }

    /**
     * Instantiate a FileAppender and open the file designated by
     * <code>filename</code>. The opened filename will become the output
     * destination for this appender.
     * <p>The file will be appended to.
     */
    public DatedRollingFileAppender(Layout layout, String filename) throws IOException {
        super(layout, filename);
    }

    /**
     * Returns the value of the <b>MaxBackupIndex</b> option.
     */
    public int getMaxBackupIndex() {
        return maxBackupIndex;
    }

    /**
     * Sets and opens the file where the log output will go. The specified file must be writable.
     * If there was already an opened file, then the previous file is closed first.
     *
     * Overrides setFile in super class FileAppender
     */
    public synchronized void setFile(String fileName, boolean append, boolean bufferedIO, int bufferSize) throws IOException {
        this.originalFileName = fileName;
        if (this.maxFileSize > 0) {
            Pair<String, Long> res = getSerialNumber();
            super.setFile(this.originalFileName + SDF_PATTERN.format(new Date()) + "." + res.getLeft(), append, bufferedIO, bufferSize);
            // set the qw counter to current/latest file length
            ((CountingQuietWriter) qw).setCount(res.getRight());
        } else {
            super.setFile(this.originalFileName + SDF_PATTERN.format(new Date()), append, bufferedIO, bufferSize);
        }

        this.nextRolloverCheck = getNextCheckForTomorrow();
    }

    /**
     * Set the maximum number of backup files to keep around.
     * <p>The <b>MaxBackupIndex</b> option determines how many backup
     * files are kept before the oldest is erased. This option takes
     * a positive integer value. If set to zero, then there will be no
     * backup files and the log file will be truncated when it reaches
     * <code>MaxFileSize</code>.
     */
    public void setMaxBackupIndex(int maxBackups) {
        this.maxBackupIndex = maxBackups;
    }

    /**
     * Set the maximum size that the output file is allowed to reach
     * before being rolled over to backup files.
     *
     * <p>
     * In configuration files, the <b>MaxFileSize</b> option takes an long
     * integer in the range 0 - 2^63. You can specify the value with the
     * suffixes "KB", "MB" or "GB" so that the integer is interpreted being
     * expressed respectively in kilobytes, megabytes or gigabytes. For example,
     * the value "10KB" will be interpreted as 10240.
     */
    public synchronized void setMaxFileSize(String value) {
        this.maxFileSize = OptionConverter.toFileSize(value, DEFAULT_MAX_FILE_SIZE);
    }

    /**
     * Sets the quiet writer to counting quiet writer
     * Overrides setQWForFiles in super class FileAppender
     */
    protected void setQWForFiles(Writer writer) {
        this.qw = new CountingQuietWriter(writer, errorHandler);
    }

    /**
     * This method differentiates RollingFileAppender from its super
     * class.
     *
     * @since 0.9.0
     */
    protected synchronized void subAppend(LoggingEvent event) {
        long size = ((CountingQuietWriter) this.qw).getCount();

        if ((maxFileSize > 0 && size >= maxFileSize) || System.currentTimeMillis() >= this.nextRolloverCheck) {
            LogLog.debug("Roll over the log to next day. Current Log File name: " + this.fileName);

            try {
                // delete old file(s)
                if (this.maxBackupIndex > 0) {
                    Path logFile = Paths.get(this.originalFileName);
                    Pattern pattern = Pattern.compile("^" + logFile.getFileName() + ".(?<yy>\\d{4})-(?<mm>\\d{2})-(?<dd>\\d{2})(.(\\d+))?$");
                    // date till the log files to be retained
                    LocalDate dateTillToRetain =
                            Instant.ofEpochMilli(this.nextRolloverCheck)
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate()
                                    .minusDays(this.maxBackupIndex + 1);

                    // get the log root folder
                    Path logPath = logFile.getParent();
                    // find all files that are older than configured no. of days to retain and delete all those files
                    Files.find(logPath, 1,
                        (path, basicFileAttributes) -> {
                            if (!path.toFile().isDirectory()) {
                                Matcher matcher = pattern.matcher(path.getFileName().toString());
                                if (matcher.matches()) {
                                    int day = Integer.parseInt(matcher.group("dd"));
                                    int month = Integer.parseInt(matcher.group("mm"));
                                    int year = Integer.parseInt(matcher.group("yy"));
                                    LocalDate logFileDate = LocalDate.of(year, month, day);
                                    Duration dur = Duration.between(logFileDate.atStartOfDay(), dateTillToRetain.atStartOfDay());
                                    if (dur.toDays() > 0) {
                                        // if the duration is greater than 0, it is older than configured no. of days to retain
                                        return true;
                                    }
                                }
                            }

                            // retain the log file
                            return false;
                        })
                        .forEach(path -> {
                            // delete the log file
                            boolean deleted = path.toFile().delete();
                            if (!deleted) {
                                LogLog.error("log file deletion failed.");
                            }
                        });
                }

                // This will also close the file. This is OK since multiple
                // close operations are safe.
                this.setFile(this.originalFileName, this.fileAppend, this.bufferedIO, this.bufferSize);
            } catch (IOException ioe) {
                if (ioe instanceof InterruptedIOException) {
                    Thread.currentThread().interrupt();
                }

                LogLog.error("rollOver() failed.", ioe);
            }
        }

        // super.subAppend(event);
        // replacing super.subAppend to replace newline char with |
        String line = this.layout.format(event);
        line = line.replaceAll(DatedRollingFileAppender.LINE_SEP_REGEX, ""); // remove the end newline character added by the formatter
        line = line.replace(Layout.LINE_SEP, DatedRollingFileAppender.LINE_SEP_REPLACE_CHAR); // replace all newline with pipe
        this.qw.write(line);
        if (layout.ignoresThrowable()) {
            String[] s = event.getThrowableStrRep();
            if (s != null) {
                this.qw.write(DatedRollingFileAppender.EXCEPTION_SEP); // putting a separator for the log parser to extract the stack trace easily
                for (int i = 0; i < s.length; i++) {
                    if (s[i] != null) {
                        this.qw.write(s[i].replace(Layout.LINE_SEP, DatedRollingFileAppender.LINE_SEP_REPLACE_CHAR)); // replace all newline with pipe
                    }
                }
            }
        }

        this.qw.write(Layout.LINE_SEP);
        if (shouldFlush(event)) {
            this.qw.flush();
        }
    }

    private long getNextCheckForTomorrow() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        c.add(Calendar.DATE, 1);
        return c.getTimeInMillis();
    }

    private Pair<String, Long> getSerialNumber() throws IOException {
        // find the current/latest log file for the current date
        Path logFile = Paths.get(originalFileName);
        Pattern pattern = Pattern.compile("^" + logFile.getFileName() + new SimpleDateFormat(DATE_PATTERN).format(new Date()) + "(.(?<sno>\\d+))?$");
        Optional<String> file =
            Files.find(
                logFile.getParent(), 1,
                (path, basicFileAttributes) -> {
                    Matcher matcher = pattern.matcher(path.getFileName().toString());
                    if (matcher.matches()) {
                        return true;
                    }
                    return false;
                })
                .map(p -> p.toFile().getName())
                .max(Comparator.comparing(String::valueOf));

        // extract the serial number if the current/latest log file exist
        int sno = 1;
        long size = 0;
        if (file.isPresent()) {
            String filename = file.get();
            Matcher matcher = pattern.matcher(filename);
            if (matcher.matches()) {
                if (matcher.group("sno") != null) {
                    sno = Integer.parseInt(matcher.group("sno"));
                    if (logFile.getParent() != null && filename != null) {
                        File f = Paths.get(logFile.getParent().toString(), filename).toFile();
                        // if the current/latest file reached the max file size, move it to next
                        if (f.length() >= this.maxFileSize) {
                            sno++;
                        } else {
                            size = f.length();
                        }
                    }
                }
            }
        }

        return new ImmutablePair<String, Long>(String.format("%04d", sno), size);
    }
}