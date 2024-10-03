package com.abdecd.moebackend.business.service.fileservice;

import com.abdecd.moebackend.business.tokenLogin.common.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "moe.local-file-service.enable", havingValue = "true")
@Slf4j
@SuppressWarnings("all")
public class LocalFileServiceImpl implements FileService {

    @Value("${moe.local-file-service.file-base-path}")
    private String FILE_BASE_PATH;

    @Value("${moe.local-file-service.url-prefix:empty}")
    private String URL_PREFIX;

    public static final String TMP_FOLDER_BASE = "/tmp";
    public static String getTmpFolder() {
        return TMP_FOLDER_BASE + "/user" + UserContext.getUserId();
    }

    public static String getFileFolder() {
        return "/img";
    }

    private String basicUpload(MultipartFile file, String folder, String fileName) throws IOException {
        var dest = new File(FILE_BASE_PATH + folder + "/" + fileName);
        // 保存文件
        dest.getParentFile().mkdirs();
        file.transferTo(dest);
        // 适配前端
        return URL_PREFIX + folder + "/" + fileName;
    }

    private String basicUpload(InputStream inputStream, String folder, String fileName) throws IOException {
        var dest = new File(FILE_BASE_PATH + folder + "/" + fileName);
        // 保存文件
        dest.getParentFile().mkdirs();
        Files.copy(inputStream, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        // 适配前端
        return URL_PREFIX + folder + "/" + fileName;
    }

    @Override
    public String uploadTmpFile(MultipartFile file) throws IOException {
        var suffix = Objects.requireNonNull(file.getOriginalFilename()).substring(file.getOriginalFilename().lastIndexOf(".")).toLowerCase();
        return basicUpload(file, getTmpFolder(), UUID.randomUUID() + suffix);
    }

    @Override
    public String uploadFile(MultipartFile file, String fileName) throws IOException {
        return basicUpload(file, getFileFolder(), fileName);
    }

    @Override
    public String uploadFile(MultipartFile file, String folder, String fileName) throws IOException {
        return basicUpload(file, folder, fileName);
    }

    @Override
    public String uploadFile(InputStream inputStream, String fileName) throws IOException {
        return basicUpload(inputStream, getFileFolder(), fileName);
    }

    @Override
    public String uploadFile(InputStream inputStream, String folder, String fileName) throws IOException {
        return basicUpload(inputStream, folder, fileName);
    }

    @Override
    public String changeTmpFileToStatic(String fullTmpFilePath, String folder, String fileName) throws IOException {
        // getTmpFolder()/xxx  ->  folder/xxx
        if (folder == null || folder.isBlank()) folder = getFileFolder();
        if (!fullTmpFilePath.startsWith(URL_PREFIX)) return "";
        var tmpFilePath = fullTmpFilePath.substring(URL_PREFIX.length());
        if (!tmpFilePath.startsWith(getTmpFolder()+"/")) return "";
        var newFileName = (fileName == null || fileName.isBlank()) ? tmpFilePath.substring(tmpFilePath.lastIndexOf("/")) : ("/" + fileName);
        var oldPath = FILE_BASE_PATH + tmpFilePath;
        var newPath = FILE_BASE_PATH + folder + newFileName;
        var tmp = new File(oldPath);
        var newFile = new File(newPath);
        newFile.getParentFile().mkdirs();
        Files.move(tmp.toPath(), new File(newPath).toPath(), StandardCopyOption.REPLACE_EXISTING);
        return URL_PREFIX + folder + newFileName;
    }

    @Override
    public void deleteFile(String path) {
        if (!path.startsWith(URL_PREFIX)) return;
        var tmpFilePath = path.substring(URL_PREFIX.length());
        var oldPath = FILE_BASE_PATH + tmpFilePath;
        var file = new File(oldPath);
        if (file.exists() && file.isFile()) file.delete();
    }

    @Override
    public void deleteFileInSystem(String path) {
        var oldPath = FILE_BASE_PATH + path;
        var file = new File(oldPath);
        if (file.exists() && file.isFile()) file.delete();
    }

    /**
     * 清除过期的临时文件
     * @param ttl 时长，单位秒
     * @throws IOException :
     */
    public void clearTmpFile(Integer ttl) throws IOException {
        var tmpDir = new File(FILE_BASE_PATH + TMP_FOLDER_BASE);
        if (tmpDir.exists()) {
            var files = tmpDir.listFiles();
            if (files != null) {
                for (var file : files) {
                    clearTmpFileBase(file, ttl);
                }
            }
        }
    }
    private void clearTmpFileBase(File tmpDir, Integer ttl) throws IOException {
        if (tmpDir.exists() && tmpDir.isDirectory()) {
            var files = tmpDir.listFiles();
            if (files != null) {
                for (var file : files) {
                    if (file.isDirectory()) clearTmpFileBase(file, ttl);
                    else {
                        var fileTime = Files.getLastModifiedTime(file.toPath()).toMillis();
                        var now = System.currentTimeMillis();
                        if (now - fileTime > ttl * 1000) {
                            log.info("删除过期文件:{}", file.getAbsolutePath());
                            file.delete();
                        }
                    }
                }
            }
        }
    }

    @Override
    public InputStream getFileInSystem(String path) throws IOException {
        var file = new File(FILE_BASE_PATH + path);
        if (file.exists() && file.isFile()) return new FileInputStream(file);
        return null;
    }
}
