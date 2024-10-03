package com.abdecd.moebackend.business.tokenLogin.common.util;

import com.password4j.BcryptFunction;
import com.password4j.types.Bcrypt;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.MGF1ParameterSpec;
import java.util.Base64;

public class PwdUtil {
    private static final KeyPair keyPair = generateKeyPair();
    public static final PublicKey publicKey = keyPair.getPublic();
    private static final PrivateKey privateKey = keyPair.getPrivate();
    private static final BcryptFunction hashFunction = BcryptFunction.getInstance(Bcrypt.B, 11);

    private static KeyPair generateKeyPair() {
        // 获取指定算法的密钥对生成器
        KeyPairGenerator gen;
        try {
            gen = KeyPairGenerator.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        // 初始化密钥对生成器（密钥长度要适中, 太短不安全, 太长加密/解密速度慢）
        gen.initialize(2048);

        return gen.generateKeyPair();
    }

    /**
     * 获取前端传过来的解密后的密码
     * @param encryptedPwd :
     * @return :
     * @throws RuntimeException :
     */
    public static String decryptPwd(String encryptedPwd) throws RuntimeException {
        Cipher cipher;
        try {
            byte[] pwdBytes = Base64.getDecoder().decode(encryptedPwd);
            cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding"); // 使用RSA-OAEP算法
            OAEPParameterSpec oaepParams = new OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT);
            cipher.init(Cipher.DECRYPT_MODE, privateKey, oaepParams);
            return new String(cipher.doFinal(pwdBytes), StandardCharsets.UTF_8);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException |
                 BadPaddingException | InvalidAlgorithmParameterException e) {
            throw new RuntimeException("decrypt error");
        }
    }

    /**
     * 密码 hash
     * @param pwd 密码
     * @return hash
     */
    public static String encodePwd(String pwd) {
        return hashFunction.hash(pwd).getResult();
    }

    /**
     * 校验密码
     * @param pwd :
     * @param hashPwd :
     * @return :
     */
    public static boolean verifyPwd(String pwd, String hashPwd) {
        try {
            return BcryptFunction.getInstanceFromHash(hashPwd).check(pwd, hashPwd);
        } catch (Exception e) {
            return false;
        }
    }
}

/*
async function loadPublicKeyFromBase64(base64String) {
    // 解码Base64字符串为Uint8Array
    const binaryDer = Uint8Array.from(atob(base64String), c => c.charCodeAt(0));

    // 将DER编码的密钥导入为CryptoKey
    const importedKey = await crypto.subtle.importKey(
        "spki", // SPKI for public keys
        binaryDer,
        {   // Algorithm configuration
            name: "RSA-OAEP",
            hash: {name: "SHA-256"}, // Or other supported hash algorithm
        },
        false, // Whether the key is extractable (usually you want this to be false for public keys)
        ["encrypt"] // Only allow encrypt operation for a public key
    );

    return importedKey;
}

async function encryptData(dataToEncrypt) {

  // 将JWK格式的公钥转换成CryptoKey

  const importedPublicKey = await loadPublicKeyFromBase64("MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAkxJl7WTrNodLLuPwKGd3J60B1unFy01xzScrIBj5SqxLr0RO+yN9Xgoe6fhT2I13zEuAEk5D2BmCNdrtNHDlfrDFdbresBTPLtQsirmpJU3QIoIz8HxSwVRfTMqu3sftgsAJQnD5YvWxk43t33f8nFSrwf4bAPA9hr4ZPIwiOxHLre0PK8CdJlwdsvsMWSA9E1r0Xlxw00uli4lXqHnk76iSy1s8bjlQN6+iwd9v39YMoyAXfwjg0ESaL8U11plW0BR/isBy096L7YzEXpNqwRZRfCKjuEOD/F2ilgU2f2wmOpX+M9/hBm01TRK7KPf4IoPvVcYSwDdxZQyUDrb3uQIDAQAB");

  // 对字符串进行编码
  let dataUint8Array = new TextEncoder().encode(dataToEncrypt);


  // 使用公钥进行加密
  let encryptedBuffer = await window.crypto.subtle.encrypt(
    {
      name: "RSA-OAEP",
    },
    importedPublicKey,
    dataUint8Array
  );

  // 将加密后的缓冲区转换为Base64格式以便传输或存储
  let encryptedBase64 = btoa(String.fromCharCode.apply(null, new Uint8Array(encryptedBuffer)));

  return encryptedBase64;
}

encryptData('Hello World')
  .then(encrypted => console.log('Encrypted:', encrypted))
  .catch(error => console.error('Error:', error));
 */