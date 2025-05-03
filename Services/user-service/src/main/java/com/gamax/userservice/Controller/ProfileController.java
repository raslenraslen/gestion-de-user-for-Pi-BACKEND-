package com.gamax.userservice.Controller;

import com.gamax.userservice.Entity.User;
import com.gamax.userservice.Service.UserService;
import com.gamax.userservice.TDO.UpdateProfilePictureRequest;
import com.gamax.userservice.TDO.UploadResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
@RestController
@RequestMapping("/api")
public class ProfileController {

    @Autowired
    private UserService userService;

    @PostMapping("/userupload")
    public ResponseEntity<UploadResponse> uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
        String fileUrl = saveFile(file);
        return ResponseEntity.ok(new UploadResponse(fileUrl));
    }

    @GetMapping("/useruploads")
    public ResponseEntity<List<String>> listUploadedFiles() throws IOException {
        Path uploadPath = Paths.get("C:/Users/user/Desktop/Pi dev/projet dev gamemax/gestion-de-user-for-Pi-BACKEND--main/Services/user-service/uploads");
        List<String> fileUrls = new ArrayList<>();
        if (Files.exists(uploadPath)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(uploadPath)) {
                for (Path path : stream) {
                    if (!Files.isDirectory(path)) {
                        fileUrls.add("http://26.223.72.183:8080/api/useruploads/" + path.getFileName().toString());
                    }
                }
            }
        }
        return ResponseEntity.ok(fileUrls);
    }

    private String saveFile(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename();
        Path uploadPath = Paths.get("C:/Users/user/Desktop/Pi dev/projet dev gamemax/gestion-de-user-for-Pi-BACKEND--main/Services/user-service/uploads");
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        assert fileName != null;
        Path filePath = uploadPath.resolve(fileName);

        try (var inputStream = file.getInputStream()) {
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
        }

        return "http://26.223.72.183:8080/api/useruploads/" + fileName;
    }

    @PutMapping("/updateProfilePicture")
    public ResponseEntity<String> updateProfilePicture(@RequestBody UpdateProfilePictureRequest request) {
        Optional<User> userOptional = userService.getUserById(request.getUserId());
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            user.setProfilePictureUrl(request.getProfilePictureUrl());
            userService.saveUser(user);
            return ResponseEntity.ok("Profile picture URL updated");
        } else {
            return ResponseEntity.status(404).body("User not found");
        }
    }


}