package com.fuzis.images.service;

import com.fuzis.images.entity.ImageData;
import com.fuzis.images.entity.ImageLink;
import com.fuzis.images.repository.ImageDataRepository;
import com.fuzis.images.repository.ImageLinkRepository;
import com.fuzis.images.transfer.ChangeDTO;
import com.fuzis.images.transfer.state.State;
import com.fuzis.images.util.TokenGenerator;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;

@Service
public class ImageService {

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp", "image/bmp"
    );

    private static final long MAX_FILE_SIZE = 15 * 1024 * 1024; // 15 MB

    private final ImageDataRepository imageDataRepository;
    private final ImageLinkRepository imageLinkRepository;
    private final TokenGenerator tokenGenerator;

    @Value("${data_path}")
    private String dataPath;

    private Path uploadDir;

    public ImageService(ImageDataRepository imageDataRepository,
                        ImageLinkRepository imageLinkRepository,
                        TokenGenerator tokenGenerator) {
        this.imageDataRepository = imageDataRepository;
        this.imageLinkRepository = imageLinkRepository;
        this.tokenGenerator = tokenGenerator;
    }

    @PostConstruct
    public void init() throws IOException {
        this.uploadDir = Paths.get(dataPath);
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }
    }

    public ChangeDTO<ImageLink> uploadImage(MultipartFile file, Integer uploaderId) {
        // Проверка на пустой файл
        if (file.isEmpty()) {
            return new ChangeDTO<>(State.Fail_BadData, "File is empty", null);
        }

        // Проверка размера файла
        if (file.getSize() > MAX_FILE_SIZE) {
            return new ChangeDTO<>(State.Fail_BadData, "File size exceeds 15 MB limit", null);
        }

        // Проверка MIME-типа
        String mimeType = file.getContentType();
        if (mimeType == null || !ALLOWED_MIME_TYPES.contains(mimeType.toLowerCase())) {
            return new ChangeDTO<>(State.Fail_BadData, "Unsupported file type. Allowed: JPEG, PNG, GIF, WebP, BMP", null);
        }

        // Получение расширения файла
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        } else {
            // Если расширение не указано, определяем по MIME-типу
            extension = getExtensionFromMimeType(mimeType);
            if (extension.isEmpty()) {
                return new ChangeDTO<>(State.Fail_BadData, "Cannot determine file extension", null);
            }
        }

        try {
            // Генерация UUID для имени файла
            String uuid = tokenGenerator.getTokenKey();
            String filename = uuid + "." + extension;
            Path filePath = uploadDir.resolve(filename);

            // Сохранение файла на диск
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Сохранение метаданных в БД
            ImageData imageData = ImageData.builder()
                    .uuid(uuid)
                    .uploaderId(uploaderId)
                    .size((int) file.getSize())
                    .mimeType(mimeType)
                    .extension(extension)
                    .build();

            imageData = imageDataRepository.save(imageData);

            // Создание записи в IMAGE_LINKS
            ImageLink imageLink = ImageLink.builder()
                    .imageData(imageData)
                    .build();

            imageLink = imageLinkRepository.save(imageLink);

            return new ChangeDTO<>(State.OK, "Image uploaded successfully", imageLink);

        } catch (IOException e) {
            return new ChangeDTO<>(State.Fail, "Failed to save image: " + e.getMessage(), null);
        } catch (Exception e) {
            return new ChangeDTO<>(State.Fail, "Unexpected error: " + e.getMessage(), null);
        }
    }

    private String getExtensionFromMimeType(String mimeType) {
        return switch (mimeType.toLowerCase()) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/gif" -> "gif";
            case "image/webp" -> "webp";
            case "image/bmp" -> "bmp";
            default -> "";
        };
    }
}