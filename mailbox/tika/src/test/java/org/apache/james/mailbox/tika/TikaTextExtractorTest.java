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

package org.apache.james.mailbox.tika;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.james.mailbox.extractor.TextExtractor;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class TikaTextExtractorTest {

    private TextExtractor textExtractor;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    
    @Rule
    public TikaContainer tika = new TikaContainer();

    @Before
    public void setUp() throws Exception {
        tika.start();
        textExtractor = new TikaTextExtractor(new TikaHttpClientImpl(TikaConfiguration.builder()
                .host(tika.getIp())
                .port(tika.getPort())
                .timeoutInMillis(tika.getTimeoutInMillis())
                .build()));
    }

    @After
    public void tearDown() {
        tika.stop();
    }

    @Test
    public void textualContentShouldReturnNullWhenInputStreamIsEmpty() throws Exception {
        assertThat(textExtractor.extractContent(IOUtils.toInputStream(""), "text/plain", "Text.txt").getTextualContent())
            .isNull();
    }

    @Test
    public void textTest() throws Exception {
        InputStream inputStream = ClassLoader.getSystemResourceAsStream("documents/Text.txt");
        assertThat(inputStream).isNotNull();
        assertThat(textExtractor.extractContent(inputStream, "text/plain", "Text.txt").getTextualContent())
            .isEqualTo("This is some awesome text text.\n\n\n");
    }

    @Test
    public void textMicrosoftWorldTest() throws Exception {
        InputStream inputStream = ClassLoader.getSystemResourceAsStream("documents/writter.docx");
        assertThat(inputStream).isNotNull();
        assertThat(textExtractor.extractContent(inputStream, "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "writter.docx").getTextualContent())
            .isEqualTo("This is an awesome document on libroffice writter !\n");
    }

    @Test
    public void textOdtTest() throws Exception {
        InputStream inputStream = ClassLoader.getSystemResourceAsStream("documents/writter.odt");
        assertThat(inputStream).isNotNull();
        assertThat(textExtractor.extractContent(inputStream, "application/vnd.oasis.opendocument.text", "writter.odt").getTextualContent())
            .isEqualTo("This is an awesome document on libroffice writter !\n");
    }

    @Test
    public void documentWithBadDeclaredMetadataShouldBeWellHandled() throws Exception {
        InputStream inputStream = ClassLoader.getSystemResourceAsStream("documents/fake.txt");
        assertThat(inputStream).isNotNull();
        assertThat(textExtractor.extractContent(inputStream, "application/vnd.oasis.opendocument.text", "writter.odt").getTextualContent())
            .isEqualTo("This is an awesome document on libroffice writter !\n");
    }
    
    @Test
    public void slidePowerPointTest() throws Exception {
        InputStream inputStream = ClassLoader.getSystemResourceAsStream("documents/slides.pptx");
        assertThat(inputStream).isNotNull();
        assertThat(textExtractor.extractContent(inputStream, "application/vnd.openxmlformats-officedocument.presentationml.presentation", "slides.pptx").getTextualContent())
            .isEqualTo("James is awesome\nIt manages attachments so well !\n\n\n");
    }

    @Test
    public void slideOdpTest() throws Exception {
        InputStream inputStream = ClassLoader.getSystemResourceAsStream("documents/slides.odp");
        assertThat(inputStream).isNotNull();
        assertThat(textExtractor.extractContent(inputStream, "application/vnd.oasis.opendocument.presentation", "slides.odp").getTextualContent())
            .isEqualTo("James is awesome\n\nIt manages attachments so well !\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");
    }
    
    @Test
    public void pdfTest() throws Exception {
        InputStream inputStream = ClassLoader.getSystemResourceAsStream("documents/PDF.pdf");
        assertThat(inputStream).isNotNull();
        assertThat(textExtractor.extractContent(inputStream, "application/pdf", "PDF.pdf").getTextualContent())
            .isEqualTo("This is an awesome document on libroffice writter !\n\n\n");
    }
    
    @Test
    public void odsTest() throws Exception {
        InputStream inputStream = ClassLoader.getSystemResourceAsStream("documents/calc.ods");
        assertThat(inputStream).isNotNull();
        assertThat(textExtractor.extractContent(inputStream, "application/vnd.oasis.opendocument.spreadsheet", "calc.ods").getTextualContent())
            .isEqualTo("This is an aesome LibreOffice document !\n" +
                "\n" +
                "\n" +
                "???\n" +
                "Page \n" +
                "??? (???)\n" +
                "00/00/0000, 00:00:00\n" +
                "Page  / \n");
    }
    
    @Test
    public void excelTest() throws Exception {
        InputStream inputStream = ClassLoader.getSystemResourceAsStream("documents/calc.xlsx");
        assertThat(inputStream).isNotNull();
        assertThat(textExtractor.extractContent(inputStream, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "calc.xlsx").getTextualContent())
            .isEqualTo("Feuille1\n" +
                "\tThis is an aesome LibreOffice document !\n" +
                "\n" +
                "&A\t\n" +
                "\n" +
                "Page &P\t\n" +
                "\n" +
                "\n");
    }

    @Test
    public void deserializerShouldThrowWhenMoreThanOneNode() throws Exception {
        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("The response should have only one element");

        TikaTextExtractor textExtractor = new TikaTextExtractor(new TikaHttpClient() {
            
            @Override
            public String metadataAndContent(InputStream inputStream, String contentType) throws TikaException {
                return "[{\"key1\":\"value1\"},{\"key2\":\"value2\"}]";
            }
        });

        InputStream inputStream = null;
        textExtractor.extractContent(inputStream, "text/plain", "fake.txt");
    }

    @Test
    public void deserializerShouldThrowWhenNodeIsNotAnObject() throws Exception {
        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("The element should be a Json object");

        TikaTextExtractor textExtractor = new TikaTextExtractor(new TikaHttpClient() {
            
            @Override
            public String metadataAndContent(InputStream inputStream, String contentType) throws TikaException {
                return "[\"value1\"]";
            }
        });

        InputStream inputStream = null;
        textExtractor.extractContent(inputStream, "text/plain", "fake.txt");
    }
}
