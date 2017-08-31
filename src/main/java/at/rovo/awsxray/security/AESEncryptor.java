package at.rovo.awsxray.security;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.encrypt.BytesEncryptor;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.security.crypto.keygen.KeyGenerators;
import org.springframework.security.crypto.keygen.StringKeyGenerator;

public class AESEncryptor {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static StringKeyGenerator generator = KeyGenerators.string();

    private static String password;

    private static final Object LOCK = new Object();

    // dummy static method that can be called on startup to ensure
    // that the application is not started if the PW file cannot be read
    public static void init() {
        getPassword();
    }

    private static String getPassword() {
        if (password == null) {
            LOG.info("Symmetric password is null. Trying to read it.");
            synchronized (LOCK) {
                // If in dev, stage, or prod - read the password from a file
                final String profile = System.getenv("ENVIRONMENT");

                if (Strings.isNullOrEmpty(profile)) {
                    LOG.error("Could not determine current environment - System variable ENVIRONMENT is null. Aborting application.");
                    System.exit(1);
                }

                LOG.info("Current profile is {}", profile);
                List<String> remoteProfiles = Lists.newArrayList("dev", "test", "prod");
                List<String> localProfiles = Lists.newArrayList("local", "ci", "vagrant", "docker");

                if (remoteProfiles.contains(profile)) {

                    try {
                        File passwordFile = new File("/opt/password");
                        LOG.info("Trying to read password file from {}", passwordFile.toPath());
                        password = IOUtils.toString(passwordFile.toURI(), StandardCharsets.UTF_8).trim();
                        LOG.info("Read {} bytes from {}", password.length(), passwordFile.toPath());
                    } catch (IOException e) {
                        LOG.info("Could not find file under /opt/password due to {}", e.getMessage());
                        String alternativeUrl = AESEncryptor.class.getResource("/").toString();
                        LOG.info("Checking now also in directory {}", alternativeUrl);
                        File alternativeFile = new File(alternativeUrl, "password");
                        try {
                            password = IOUtils.toString(alternativeFile.toURI(), StandardCharsets.UTF_8).trim();
                            LOG.info("Read {} bytes from {}", password.length(), alternativeUrl);
                        } catch (IOException e1) {
                            LOG.error(
                                    "No password set in dev, test, or prod environment - this is required. Aborting application. The exception was ",
                                    e);
                            System.exit(1);
                        }


                    }
                    // For local, test, or CI use a default password
                } else if (localProfiles.contains(profile)) {
            /*
            If the profile is on of the local ones we check in the local user.dir for the password file
            this allows to generate the test database for dev locally and upload it to dev
             */
                    File
                            localPasswordFile =
                            new File(System.getProperty("user.home"), "local_dev_password");
                    LOG.info("Trying to read password from {}", localPasswordFile.toPath());
                    try {
                        password = IOUtils.toString(localPasswordFile.toURI(), StandardCharsets.UTF_8).trim();
                        LOG.info("Read {} bytes from {}", password.length(), localPasswordFile.toPath());
                    } catch (IOException e) {
                        LOG.info(
                                "Could not read local password file, since there is not one. Fallback to default password");
                        password = "KrAjuFc,K8H/K8kYxbvqhuQ"; // Default password for unit tests,...
                        LOG.warn("Using the default password for symmetric encryption");
                    }
                    // no valid profile found, escalate
                } else {
                    LOG.error("Error loading password, the ENVIRONMENT profile " + profile + " is invalid. Aborting application.");
                    System.exit(1);
                }
            }
        }

        if (Strings.isNullOrEmpty(password)) {
            LOG.error("Error loading password for symmetric encryption, password is null");
            System.exit(1);
        }

        return password;
    }


    /**
     * Get a random salt for encryption.
     *
     * @return 8 byte random salt as a string.
     */
    public static String getSalt() {
        long start = System.nanoTime();
        String salt = generator.generateKey();
        long end = System.nanoTime();
        LOG.trace("Generating salt took: " + timeDiff(start, end));
        return salt;

    }

    /**
     * Encrypt the input with AES.
     *
     * @param input The string to be encrypted.
     * @param salt  The input specific salt.
     * @return The encrypted string - SHA256 with 1024 iterations.
     */
    public static String encrypt(String input, String salt) {
        long start = System.nanoTime();
        TextEncryptor encryptor = Encryptors.text(getPassword(), salt);
        String
                cipher =
                encryptor.encrypt(
                        input); // This will break intentionally if something goes wrong (bad characters in the password?)
        long end = System.nanoTime();
        LOG.trace("Encryption took: " + timeDiff(start, end));
        return cipher;
    }

    /**
     * Encrypt the input with AES.
     *
     * @param input The byte array to be encrypted.
     * @param salt  The input specific salt.
     * @return The encrypted string - SHA256 with 1024 iterations.
     */
    public static byte[] encrypt(byte[] input, String salt) {
        long start = System.nanoTime();
        BytesEncryptor encryptor = Encryptors.standard(getPassword(), salt);
        byte[]
                cipher =
                encryptor.encrypt(
                        input); // This will break intentionally if something goes wrong (bad characters in the password?)
        long end = System.nanoTime();
        LOG.trace("Encryption took: " + timeDiff(start, end));
        return cipher;
    }

    /**
     * Decrypt the cipher with AES.
     *
     * @param cipher The encrypted string.
     * @param salt   The cipher specific salt.
     * @return The decrypted cipher.
     */
    public static String decrypt(String cipher, String salt) {
        long start = System.nanoTime();
        TextEncryptor encryptor = Encryptors.text(getPassword(), salt);
        String
                output =
                encryptor.decrypt(
                        cipher); // This will break intentionally if something goes wrong (bad characters in the password?)
        long end = System.nanoTime();
        LOG.trace("Decryption took: " + timeDiff(start, end));
        return output;
    }

    /**
     * Decrypt the cipher with AES.
     *
     * @param cipher The encrypted byte array.
     * @param salt   The cipher specific salt.
     * @return The decrypted cipher.
     */
    public static byte[] decrypt(byte[] cipher, String salt) {
        long start = System.nanoTime();
        BytesEncryptor encryptor = Encryptors.standard(getPassword(), salt);
        byte[]
                output =
                encryptor.decrypt(
                        cipher); // This will break intentionally if something goes wrong (bad characters in the password?)
        long end = System.nanoTime();
        LOG.trace("Decryption took: " + timeDiff(start, end));
        return output;
    }

    /**
     * Internal method to calculate time differences.
     *
     * @param start Relative start time in ns.
     * @param end   Relative end time in ns.
     * @return Time difference in ms including the unit.
     */
    private static String timeDiff(long start, long end) {
        long diff = (end - start) / 1000;
        return String.format("%,d", diff) + "µs";
    }

}
