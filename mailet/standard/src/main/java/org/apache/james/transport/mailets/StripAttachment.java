/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.transport.mailets;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.mailet.Mail;
import org.apache.mailet.MailetException;
import org.apache.mailet.base.GenericMailet;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

/**
 * <p>
 * Remove attachments from a Message. Supports simple removal, storing to file,
 * or storing to mail attributes.
 * </p>
 * <p>
 * Configuration:
 * </p>
 * <p>
 * 
 * <pre>
 *   &lt;mailet match=&quot;All&quot; class=&quot;StripAttachment&quot; &gt;
 *     &lt;pattern &gt;.*\.xls &lt;/pattern&gt;  &lt;!-- The regular expression that must be matched -- &gt;
 *     &lt;!-- notpattern &gt;.*\.xls &lt;/notpattern--&gt;  &lt;!-- The regular expression that must be matched -- &gt;
 *     &lt;directory &gt;c:\temp\james_attach &lt;/directory&gt;   &lt;!-- The directory to save to -- &gt;
 *     &lt;remove &gt;all &lt;/remove&gt;   &lt;!-- either &quot;no&quot;, &quot;matched&quot;, &quot;all&quot; -- &gt;
 *     &lt;!-- attribute&gt;my.attribute.name&lt;/attribute --&gt;
 *   &lt;/mailet &gt;
 *   
 *   At least one of pattern and notpattern is required.
 * </pre>
 * 
 * </p>
 */
public class StripAttachment extends GenericMailet {

    private static final String MULTIPART_MIME_TYPE = "multipart/*";
    public static final String PATTERN_PARAMETER_NAME = "pattern";
    public static final String NOTPATTERN_PARAMETER_NAME = "notpattern";
    public static final String ATTRIBUTE_PARAMETER_NAME = "attribute";
    public static final String DIRECTORY_PARAMETER_NAME = "directory";
    // Either "no", "matched", "all"
    public static final String REMOVE_ATTACHMENT_PARAMETER_NAME = "remove"; 
    // Either "true", "false"
    public static final String DECODE_FILENAME_PARAMETER_NAME = "decodeFilename";
    public static final String REPLACE_FILENAME_PATTERN_PARAMETER_NAME = "replaceFilenamePattern";
    public static final String REMOVE_NONE = "no";
    public static final String REMOVE_ALL = "all";
    public static final String REMOVE_MATCHED = "matched";
    public static final String REMOVED_ATTACHMENTS_ATTRIBUTE_KEY = "org.apache.james.mailet.standard.mailets.StripAttachment.removed";
    public static final String SAVED_ATTACHMENTS_ATTRIBUTE_KEY = "org.apache.james.mailet.standard.mailets.StripAttachment.saved";

    public static final boolean DECODE_FILENAME_DEFAULT_VALUE = false;

    @VisibleForTesting String removeAttachments;
    private String directoryName;
    private String attributeName;
    private Pattern regExPattern;
    private Pattern notregExPattern;
    private boolean decodeFilename;

    private List<ReplacingPattern> filenameReplacingPatterns;

    /**
     * Checks if the mandatory parameters are present, creates the directory to
     * save the files in (if not present).
     * 
     * @throws MailetException
     */
    @Override
    public void init() throws MailetException {
        regExPattern = regExFromParameter(PATTERN_PARAMETER_NAME);
        notregExPattern = regExFromParameter(NOTPATTERN_PARAMETER_NAME);
        if (regExPattern == null && notregExPattern == null) {
            throw new MailetException("At least one of '" + PATTERN_PARAMETER_NAME + "' or '" + NOTPATTERN_PARAMETER_NAME + "' parameter should be provided.");
        }

        directoryName = getInitParameter(DIRECTORY_PARAMETER_NAME);
        attributeName = getInitParameter(ATTRIBUTE_PARAMETER_NAME);

        removeAttachments = getInitParameter(REMOVE_ATTACHMENT_PARAMETER_NAME, REMOVE_NONE).toLowerCase(Locale.US);
        if (!removeAttachments.equals(REMOVE_MATCHED) && !removeAttachments.equals(REMOVE_ALL) && !removeAttachments.equals(REMOVE_NONE)) {
            throw new MailetException(String.format("Unknown remove parameter value '%s' waiting for '%s', '%s' or '%s'.", 
                    removeAttachments, REMOVE_MATCHED, REMOVE_ALL, REMOVE_NONE));
        }

        if (directoryName != null) {
            createDirectory();
        }

        decodeFilename = getBooleanParameter(getInitParameter(DECODE_FILENAME_PARAMETER_NAME), DECODE_FILENAME_DEFAULT_VALUE);
        String replaceFilenamePattern = getInitParameter(REPLACE_FILENAME_PATTERN_PARAMETER_NAME);
        if (replaceFilenamePattern != null) {
            filenameReplacingPatterns = new PatternExtractor().getPatternsFromString(replaceFilenamePattern);
        } else {
            filenameReplacingPatterns = ImmutableList.of();
        }

        logConfiguration();
    }

    private Pattern regExFromParameter(String patternParameterName) throws MailetException {
        String patternString = getInitParameter(patternParameterName);
        try {
            if (patternString != null) {
                return Pattern.compile(patternString);
            }
            return null;
        } catch (Exception e) {
            throw new MailetException("Could not compile regex [" + patternString + "].");
        }
    }

    private void createDirectory() throws MailetException {
        try {
            FileUtils.forceMkdir(new File(directoryName));
        } catch (Exception e) {
            throw new MailetException("Could not create directory [" + directoryName + "].", e);
        }
    }

      private void logConfiguration() {
      StringBuilder logMessage = new StringBuilder();
      logMessage.append("StripAttachment is initialised with regex pattern [");
      if (regExPattern != null) {
          logMessage.append(regExPattern.pattern());
      } else {
          logMessage.append("");
      }
      logMessage.append(" / ");
      if (notregExPattern != null) {
          logMessage.append(notregExPattern.pattern());
      } else {
          logMessage.append("");
      }
      logMessage.append("]");

      if (directoryName != null) {
          logMessage.append(" and will save to directory [");
          logMessage.append(directoryName);
          logMessage.append("]");
      }
      if (attributeName != null) {
          logMessage.append(" and will store attachments to attribute [");
          logMessage.append(attributeName);
          logMessage.append("]");
      }
      log(logMessage.toString());
    }
    
    /**
     * Service the mail: scan it for attachments matching the pattern, store the
     * content of a matching attachment in the given directory.
     * 
     * @param mail
     *            The mail to service
     * @throws MailetException
     *             Thrown when an error situation is encountered.
     */
    @Override
    public void service(Mail mail) throws MailetException {
        // All MIME messages with an attachment are multipart, so we do nothing
        // if it is not mutlipart
        MimeMessage message = getMessageFromMail(mail);
        if (isMultipart(message)) {
            analyseMultipartPartMessage(message, mail);
        }
    }

    private boolean isMultipart(Part part) throws MailetException {
        try {
            return part.isMimeType(MULTIPART_MIME_TYPE);
        } catch (MessagingException e) {
            throw new MailetException("Could not retrieve contenttype of MimePart.", e);
        }
    }

    private MimeMessage getMessageFromMail(Mail mail) throws MailetException {
        try {
            return mail.getMessage();
        } catch (MessagingException e) {
            throw new MailetException("Could not retrieve message from Mail object", e);
        }
    }

    /**
     * returns a String describing this mailet.
     * 
     * @return A desciption of this mailet
     */
    @Override
    public String getMailetInfo() {
        return "StripAttachment";
    }

    /**
     * Checks every part in this part (if it is a Multipart) for having a
     * filename that matches the pattern. If the name matches, the content of
     * the part is stored (using its name) in te given directory.
     * 
     * Note: this method is recursive.
     * 
     * @param part
     *            The part to analyse.
     * @param mail
     * @return True if one of the subpart was removed
     * @throws Exception
     */
    @VisibleForTesting boolean analyseMultipartPartMessage(Part part, Mail mail) throws MailetException {
        if (!isMultipart(part)) {
            return false;
        }

        try {
            Multipart multipart = (Multipart) part.getContent();
            boolean atLeastOneRemoved = false;
            boolean subpartHasBeenChanged = false;
            List<BodyPart> bodyParts = retrieveBodyParts(multipart);
            for (BodyPart bodyPart: bodyParts) {
                if (isMultipart(bodyPart)) {
                    if (analyseMultipartPartMessage(bodyPart, mail)) {
                        subpartHasBeenChanged = true;
                    }
                } else {
                    if (shouldBeRemoved(bodyPart, mail)) {
                        multipart.removeBodyPart(bodyPart);
                        atLeastOneRemoved = true;
                    }
                }
            }
            if (atLeastOneRemoved || subpartHasBeenChanged) {
                part.setContent(multipart);
                if (part instanceof Message) {
                    ((Message) part).saveChanges();
                }
            }
            return atLeastOneRemoved || subpartHasBeenChanged;
        } catch (Exception e) {
            log("Failing while analysing part for attachments (StripAttachment mailet).", e);
            return false;
        }
    }

    private List<BodyPart> retrieveBodyParts(Multipart multipart) throws MessagingException {
        ImmutableList.Builder<BodyPart> builder = ImmutableList.builder();
        for (int i = 0; i < multipart.getCount(); i++) {
            builder.add(multipart.getBodyPart(i));
        }
        return builder.build();
    }

    private boolean shouldBeRemoved(BodyPart bodyPart, Mail mail) throws MessagingException, Exception {
        String fileName = getFilename(bodyPart);
        if (fileName == null) {
            return false;
        }

        boolean shouldRemove = removeAttachments.equals(REMOVE_ALL);
        if (fileNameMatches(fileName)) {
            storeBodyPartAsFile(bodyPart, mail, fileName);
            storeBodyPartAsMailAttribute(bodyPart, mail, fileName);
            if (removeAttachments.equals(REMOVE_MATCHED)) {
                shouldRemove = true;
            }
        }
        storeFileNameAsAttribute(mail, fileName, shouldRemove);
        return shouldRemove;
    }

    private void storeBodyPartAsFile(BodyPart bodyPart, Mail mail, String fileName) throws Exception {
        if (directoryName != null) {
            Optional<String> filename = saveAttachmentToFile(bodyPart, Optional.of(fileName));
            if (filename.isPresent()) {
                addFilenameToAttribute(mail, filename.get(), SAVED_ATTACHMENTS_ATTRIBUTE_KEY);
            }
        }
    }

    private void addFilenameToAttribute(Mail mail, String filename, String attributeName) {
        @SuppressWarnings("unchecked")
        ArrayList<String> attributeValues = (ArrayList<String>) mail.getAttribute(attributeName);
        if (attributeValues == null) {
            attributeValues = new ArrayList<String>();
            mail.setAttribute(attributeName, attributeValues);
        }
        attributeValues.add(filename);
    }

    private void storeBodyPartAsMailAttribute(BodyPart bodyPart, Mail mail, String fileName) throws IOException, MessagingException {
        if (attributeName != null) {
            addPartContent(bodyPart, mail, fileName);
        }
    }

    private void addPartContent(BodyPart bodyPart, Mail mail, String fileName) throws IOException, MessagingException {
        @SuppressWarnings("unchecked")
        LinkedHashMap<String, byte[]> fileNamesToPartContent = (LinkedHashMap<String, byte[]>) mail.getAttribute(attributeName);
        if (fileNamesToPartContent == null) {
            fileNamesToPartContent = new LinkedHashMap<String, byte[]>();
            mail.setAttribute(attributeName, fileNamesToPartContent);
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bodyPart.writeTo(new BufferedOutputStream(byteArrayOutputStream));
        fileNamesToPartContent.put(fileName, byteArrayOutputStream.toByteArray());
    }

    private void storeFileNameAsAttribute(Mail mail, String fileName, boolean asToBeStored) {
        if (asToBeStored) {
            addFilenameToAttribute(mail, fileName, REMOVED_ATTACHMENTS_ATTRIBUTE_KEY);
        }
    }

    private String getFilename(BodyPart bodyPart) throws UnsupportedEncodingException, MessagingException {
        String fileName = bodyPart.getFileName();
        if (fileName != null) {
            return renameWithConfigurationPattern(decodeFilename(fileName));
        }
        return fileName;
    }

    private String renameWithConfigurationPattern(String fileName) {
        if (filenameReplacingPatterns != null) {
            return new ContentReplacer(false, this).applyPatterns(filenameReplacingPatterns, fileName);
        }
        return fileName;
    }

    private String decodeFilename(String fileName) throws UnsupportedEncodingException {
        if (decodeFilename) {
            return MimeUtility.decodeText(fileName);
        }
        return fileName;
    }

    /**
     * Checks if the given name matches the pattern.
     * 
     * @param name
     *            The name to check for a match.
     * @return True if a match is found, false otherwise.
     */
    @VisibleForTesting boolean fileNameMatches(String name) {
        if (patternsAreEquals()) {
            return false;
        }
        boolean result = isMatchingPattern(name, regExPattern).or(false) 
                || !isMatchingPattern(name, notregExPattern).or(true);

        log("attachment " + name + " " + ((result) ? "matches" : "does not match"));
        return result;
    }

    private boolean patternsAreEquals() {
        return regExPattern != null && notregExPattern != null
                && regExPattern.pattern().equals(notregExPattern.pattern());
    }

    private Optional<Boolean> isMatchingPattern(String name, Pattern pattern) {
        if (pattern != null) {
            return Optional.of(pattern.matcher(name).matches());
        }
        return Optional.absent();
    }

    /**
     * Saves the content of the part to a file in the given directory, using the
     * name of the part. Created files have unique names.
     * 
     * @param part
     *            The MIME part to save.
     * @return
     * @throws Exception
     */
    @VisibleForTesting Optional<String> saveAttachmentToFile(Part part, Optional<String> fileName) throws Exception {
        try {
            File outputFile = outputFile(part, fileName);

            log("saving content of " + outputFile.getName() + "...");
            IOUtils.copy(part.getInputStream(), new FileOutputStream(outputFile));

            return Optional.of(outputFile.getName());
        } catch (Exception e) {
            log("Error while saving contents of", e);
            return Optional.absent();
        }
    }

    private File outputFile(Part part, Optional<String> fileName) throws MessagingException, IOException {
        if (!fileName.isPresent()) {
            return createTempFile(part.getFileName());
        }
        return createTempFile(fileName.get());
    }

    private File createTempFile(String originalFileName) throws IOException {
        OutputFileName outputFileName = OutputFileName.from(originalFileName);
        return File.createTempFile(outputFileName.getPrefix(), outputFileName.getSuffix(), new File(directoryName));
    }

    @VisibleForTesting static class OutputFileName {

        private static final char PAD_CHAR = '_';
        private static final int MIN_LENGTH = 3;
        private static final String DEFAULT_SUFFIX = ".bin";

        public static OutputFileName from(String originalFileName) {
            if (!originalFileName.contains(".")) {
                return new OutputFileName(prependedPrefix(originalFileName), DEFAULT_SUFFIX);
            }

            int lastDotPosition = originalFileName.lastIndexOf(".");
            return new OutputFileName(prependedPrefix(originalFileName.substring(0, lastDotPosition)), 
                    originalFileName.substring(lastDotPosition));
        }

        @VisibleForTesting static String prependedPrefix(String prefix) {
            return Strings.padStart(prefix, MIN_LENGTH, PAD_CHAR);
        }

        private final String prefix;
        private final String suffix;

        private OutputFileName(String prefix, String suffix) {
            this.prefix = prefix;
            this.suffix = suffix;
        }

        public String getPrefix() {
            return prefix;
        }

        public String getSuffix() {
            return suffix;
        }
    }
}

