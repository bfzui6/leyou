package com.leyou.upload.service;

import com.github.tobato.fastdfs.FdfsClientConfig;
import com.github.tobato.fastdfs.domain.StorePath;
import com.github.tobato.fastdfs.service.FastFileStorageClient;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;


@Service
public class UploadService {
    private static final List<String> CONTENT_TYPE = Arrays.asList("image/gif","image/jpeg");
    @Autowired
    private FastFileStorageClient storageClient;
    private static final Logger LOGGER = LoggerFactory.getLogger(UploadService.class);
    public String upload(MultipartFile file){
        String originalFilename = file.getOriginalFilename();
        String contentType = file.getContentType();
        try {
            if (!CONTENT_TYPE.contains(contentType)){
                LOGGER.info("文件类型不合法"+originalFilename);
                return null;
            }
            BufferedImage read = ImageIO.read(file.getInputStream());
            if (read == null){
                LOGGER.info("文件内容不合法"+originalFilename);
                return null;
            }
//            file.transferTo(new File("E:\\乐友商城\\image\\"+originalFilename));
            String s = StringUtils.substringAfterLast(originalFilename,".");
            StorePath storePath = storageClient.uploadFile(file.getInputStream(), file.getSize(), s, null);
            return "http://image.leyou.com/"+storePath.getFullPath();
        } catch (IOException e) {
            LOGGER.info("服务器内部问题"+originalFilename);
            e.printStackTrace();
        }
        return null;
    }

}
