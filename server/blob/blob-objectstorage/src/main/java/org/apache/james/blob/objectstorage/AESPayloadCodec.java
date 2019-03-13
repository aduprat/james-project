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

package org.apache.james.blob.objectstorage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Optional;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.apache.commons.io.IOUtils;
import org.apache.james.blob.objectstorage.crypto.CryptoConfig;
import org.jclouds.io.Payloads;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.crypto.tink.Aead;
import com.google.crypto.tink.aead.AeadConfig;
import com.google.crypto.tink.subtle.AesGcmJce;

public class AESPayloadCodec implements PayloadCodec {

    private static final Logger LOGGER = LoggerFactory.getLogger(AESPayloadCodec.class);
    private static final byte[] EMPTY_ASSOCIATED_DATA = new byte[0];
    private static final int PBKDF2_ITERATIONS = 65536;
    private static final int KEY_SIZE = 256;
    private static final String SECRET_KEY_FACTORY_ALGORITHM = "PBKDF2WithHmacSHA256";

    private final Aead aead;

    public AESPayloadCodec(CryptoConfig cryptoConfig) {
        try {
            AeadConfig.register();

            SecretKey secretKey = deriveKey(cryptoConfig);
            aead = new AesGcmJce(secretKey.getEncoded());
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Error while starting AESPayloadCodec", e);
        }
    }

    private static SecretKey deriveKey(CryptoConfig cryptoConfig) throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] saltBytes = cryptoConfig.salt();
        SecretKeyFactory skf = SecretKeyFactory.getInstance(SECRET_KEY_FACTORY_ALGORITHM);
        PBEKeySpec spec = new PBEKeySpec(cryptoConfig.password(), saltBytes, PBKDF2_ITERATIONS, KEY_SIZE);
        return skf.generateSecret(spec);
    }

    @Override
    public Payload write(InputStream inputStream) {
        try {
            byte[] ciphertext = aead.encrypt(IOUtils.toByteArray(inputStream), EMPTY_ASSOCIATED_DATA);
            return new Payload(Payloads.newByteArrayPayload(ciphertext), Optional.of(Long.valueOf(ciphertext.length)));
        } catch (IOException | GeneralSecurityException e) {
            throw new RuntimeException("Unable to build payload for object storage, failed to " +
                "encrypt", e);
        }
    }

    @Override
    public InputStream read(Payload payload) throws IOException {
        try {
            byte[] ciphertext = IOUtils.toByteArray(payload.getPayload().openStream());
            byte[] decrypt = aead.decrypt(ciphertext, EMPTY_ASSOCIATED_DATA);
            return new ByteArrayInputStream(decrypt);
        } catch (GeneralSecurityException e) {
            throw new IOException("Incorrect crypto setup", e);
        }
    }

}
