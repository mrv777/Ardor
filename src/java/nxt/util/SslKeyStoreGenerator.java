/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2020 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of this software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */
package nxt.util;

import nxt.crypto.Crypto;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.bc.BcRSAContentSignerBuilder;

import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Generates a local JKS keystore for secured server. A local certificate authority is generated and its certificate is
 * additionally stored in DER format for easier import as trusted CA. The CA private key is thrown away immediately
 * after signing the server certificate so that it cannot be used again
 */
public class SslKeyStoreGenerator {

    public static final String IP_PREFIX = "ip:";

    @SuppressWarnings("unused")
    public static class Builder {
        private Path keyStorePath = Paths.get("cert","local_ssl.jks");
        private String password = "123456";
        private String keyStoreType = "JKS";
        private final List<String> domainNames = new ArrayList<>();
        private int validityDays = 36500;
        private int keyLength = 2048;
        private String keyPairAlgorithm = "RSA";
        private String signatureAlgorithm = "SHA256WithRSAEncryption";

        public Builder() {
            domainNames.add("localhost");
            domainNames.add(IP_PREFIX + "127.0.0.1");
        }

        public Builder setKeyStorePath(Path keyStorePath) {
            this.keyStorePath = keyStorePath;
            return this;
        }

        public Builder setPassword(String password) {
            this.password = password;
            return this;
        }

        public Builder setKeyStoreType(String keyStoreType) {
            this.keyStoreType = keyStoreType;
            return this;
        }

        public Builder setDomainNames(Collection<String> domainNames) {
            this.domainNames.clear();
            domainNames.forEach(this::addDomainName);
            return this;
        }

        public Builder addDomainName(String domainName) {
            if (!domainNames.contains(domainName)) {
                domainNames.add(domainName);
            }
            return this;
        }

        public Builder setValidityDays(int validityDays) {
            this.validityDays = validityDays;
            return this;
        }

        public Builder setKeyLength(int keyLength) {
            this.keyLength = keyLength;
            return this;
        }

        public Builder setKeyPairAlgorithm(String keyPairAlgorithm) {
            this.keyPairAlgorithm = keyPairAlgorithm;
            return this;
        }

        public Builder setSignatureAlgorithm(String signatureAlgorithm) {
            this.signatureAlgorithm = signatureAlgorithm;
            return this;
        }

        public SslKeyStoreGenerator build() {
            return new SslKeyStoreGenerator(this);
        }
    }

    public static class GeneratorException extends Exception {
        private GeneratorException(Throwable cause) {
            super(cause);
        }
    }

    private final Path keyStorePath;
    private final String password;
    private final String keyStoreType;
    private final List<String> domainNames;
    private final int validityDays;
    private final int keyLength;
    private final String keyPairAlgorithm;
    private final String signatureAlgorithm;

    private SslKeyStoreGenerator(Builder builder) {
        this.keyStorePath = builder.keyStorePath;
        this.password = builder.password;
        this.keyStoreType = builder.keyStoreType;
        this.domainNames = Collections.unmodifiableList(builder.domainNames);
        this.validityDays = builder.validityDays;
        this.keyLength = builder.keyLength;
        this.keyPairAlgorithm = builder.keyPairAlgorithm;
        this.signatureAlgorithm = builder.signatureAlgorithm;
    }

    public void generate() throws GeneratorException {
        try {
            // Generate the key pairs for the CA and Server
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(keyPairAlgorithm);
            keyPairGenerator.initialize(keyLength);
            KeyPair caKeyPair = keyPairGenerator.generateKeyPair();
            KeyPair serverKeyPair = keyPairGenerator.generateKeyPair();

            // Generate and write the CA certificate
            Files.createDirectories(keyStorePath.getParent());
            X509Certificate caCert = generateCertificate(caKeyPair.getPublic(), caKeyPair.getPrivate(), true); // CA private and public key
            FileOutputStream fos = new FileOutputStream(getCaCertPath(keyStorePath).toFile());
            fos.write(caCert.getEncoded());
            fos.close();

            // Generate the server certificate signed by the CA certificate and add to the keystore
            X509Certificate serverCert = generateCertificate(serverKeyPair.getPublic(), caKeyPair.getPrivate(), false); // Server public key, CA private key
            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(null, password.toCharArray());
            keyStore.setKeyEntry("main", serverKeyPair.getPrivate(),
                    password.toCharArray(),
                    new java.security.cert.Certificate[]{serverCert, caCert});
            keyStore.store( new FileOutputStream(keyStorePath.toFile()), password.toCharArray());
        } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
            throw new GeneratorException(e);
        }
    }

    /**
     * Utility method for getting the path to the CA certificate having the keystore path
     * @param keyStorePath Keystore path
     * @return CA certification path
     */
    public static Path getCaCertPath(Path keyStorePath) {
        String fileName = keyStorePath.getFileName().toString();
        if (fileName.indexOf(".") > 0) {
            fileName = fileName.substring(0, fileName.lastIndexOf("."));
        }
        return keyStorePath.getParent().resolve(fileName + "-ca.crt");
    }

    private X509Certificate generateCertificate(PublicKey certKey, PrivateKey signingKey, boolean isCa) throws CertificateException {
        try {
            // Prepare signer object
            AlgorithmIdentifier sigAlgId = new DefaultSignatureAlgorithmIdentifierFinder().find(signatureAlgorithm);
            AlgorithmIdentifier digAlgId = new DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId);
            AsymmetricKeyParameter privateKeyAsymKeyParam = PrivateKeyFactory.createKey(signingKey.getEncoded());
            SubjectPublicKeyInfo subPubKeyInfo = SubjectPublicKeyInfo.getInstance(certKey.getEncoded());
            ContentSigner sigGen = new BcRSAContentSignerBuilder(sigAlgId, digAlgId).build(privateKeyAsymKeyParam);

            // Prepare the certificate
            X500Name name = getSubject(isCa);
            X500Name issuerName = isCa ? name : getSubject(true);
            Date from = new Date();
            int validityDays = this.validityDays + (isCa ? 1 : 0);
            Date to = new Date(from.getTime() + validityDays * 24L * 60L * 60L * 1000L);
            BigInteger sn = new BigInteger(64, Crypto.getSecureRandom());
            X509v3CertificateBuilder v3CertGen = new X509v3CertificateBuilder(issuerName, sn, from, to, name, subPubKeyInfo);

            if (isCa) {
                // For CA mark as code signing certificate
                v3CertGen.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
                v3CertGen.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.keyCertSign));
            } else {
                // For server list the supported domains
                v3CertGen.addExtension(Extension.subjectAlternativeName, false,
                        new GeneralNames(domainNames.stream()
                                .map(domainName -> {
                                    int nameType = GeneralName.dNSName;
                                    if (domainName.startsWith(IP_PREFIX)) {
                                        nameType = GeneralName.iPAddress;
                                        domainName = domainName.substring(IP_PREFIX.length());
                                    }
                                    return new GeneralName(nameType, domainName);
                                })
                                .toArray(GeneralName[]::new)));
            }

            // Build and sign the certificate
            X509CertificateHolder certificateHolder = v3CertGen.build(sigGen);
            return new JcaX509CertificateConverter().getCertificate(certificateHolder);
        } catch (CertificateException ce) {
            throw ce;
        } catch (Exception e) {
            throw new CertificateException(e);
        }
    }

    private X500Name getSubject(boolean isCa) {
        String commonName = isCa ? "Ardor Local CA" : this.domainNames.get(0);
        return new X500Name("CN=" + commonName + ", O=Jelurida, OU=Ardor");
    }

    public static void main(String[] args) {
        try {
            new Builder().build().generate();
        } catch (GeneratorException e) {
            e.printStackTrace();
        }
    }
}
