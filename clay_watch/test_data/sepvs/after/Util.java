package cau.agse.sepvs.aestask.beckervoigt;

import com.google.common.hash.Hashing;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

public class Util {

    public static long getFileSize(String path) {
        File file = new File(path); //todo does this twice (also in read file to string in FileTools
        if (!file.exists() || !file.isFile()) return -1;
        return file.length();
    }

    public static final int iterationsEncrypt = 10000;
    public static final String salt = "Insecure AES operation for SEPVS class, do not *ever* use this for real projects.";

    public static Cipher getAESCipher(char[] password, int opmode)
            throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidKeyException {

        SecretKey key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
                .generateSecret(new PBEKeySpec(password, salt.getBytes(), iterationsEncrypt, 128));
        SecretKeySpec encryptKey = new SecretKeySpec(key.getEncoded(), "AES");
        Cipher aesCipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        aesCipher.init(opmode, encryptKey);
        return aesCipher;

    }


    public static boolean checksum_correct(byte[] text, String sha256_checksum) {
        return Hashing.sha256().hashBytes(text).toString().equals(sha256_checksum);
    }

    public static void writeBytesToFile(Path outputFileName, byte[] content) throws IOException {
        File inputFile = outputFileName.toFile();
        FileOutputStream outputStream = new FileOutputStream(inputFile);
        outputStream.write(content);
        outputStream.close();
    }
}
