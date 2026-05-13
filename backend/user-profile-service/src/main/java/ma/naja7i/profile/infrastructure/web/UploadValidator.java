package ma.naja7i.profile.infrastructure.web;

import ma.naja7i.profile.config.MinioProperties;
import ma.naja7i.shared.errors.BusinessException;
import ma.naja7i.shared.errors.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@Component
public class UploadValidator {

    private static final Set<String> PHOTO_MIME = Set.of("image/jpeg", "image/png", "image/webp");
    private static final Set<String> DIPLOMA_MIME = Set.of("application/pdf");
    private static final Set<String> VIDEO_MIME = Set.of("video/mp4", "video/webm");

    private final MinioProperties props;

    public UploadValidator(MinioProperties props) {
        this.props = props;
    }

    public void validatePhoto(MultipartFile file) {
        validate(file, PHOTO_MIME, props.maxPhotoBytes(), "photo");
    }

    public void validateDiploma(MultipartFile file) {
        validate(file, DIPLOMA_MIME, props.maxDiplomaBytes(), "diploma");
    }

    public void validateVideo(MultipartFile file) {
        validate(file, VIDEO_MIME, props.maxVideoBytes(), "video");
    }

    private void validate(MultipartFile file, Set<String> allowedMime, long maxBytes, String kind) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, kind + " file is required");
        }
        if (file.getSize() > maxBytes) {
            throw new BusinessException(ErrorCode.CHAT_ATTACHMENT_TOO_LARGE,
                    kind + " exceeds max size of " + maxBytes + " bytes");
        }
        String mime = file.getContentType();
        if (mime == null || !allowedMime.contains(mime.toLowerCase())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    kind + " mime '" + mime + "' is not allowed; expected: " + allowedMime);
        }
    }
}
