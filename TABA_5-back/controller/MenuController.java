package taba.menutranslator.controller;


import lombok.Getter;
import lombok.Setter;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import taba.menutranslator.dto.RecordDTO;
import taba.menutranslator.entity.RecordEntity;
import taba.menutranslator.service.MenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

@Getter
@Setter
@RestController
@RequestMapping("/api/images")
public class MenuController {

    @Autowired
    MenuService menuService;

    //S3에 이미지 저장
    @PostMapping("/upload/save")
    public ResponseEntity<String> uploadImage(@RequestParam("image") MultipartFile file) {
        try {
            String imageUrl = menuService.uploadImage(file);
            return ResponseEntity.status(HttpStatus.OK).body("Image uploaded successfully");
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to upload image");
        }
    }

    //음식 사진 플라스크와 연동
    @PostMapping("/upload/flask")
    public ResponseEntity<String> sendImageToFlask(@RequestParam("image") MultipartFile file) {
        try {
            String response = menuService.sendImageToFlask(file);
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to upload image");
        }
    }

    // 메뉴판 플라스크와 연동
    @PostMapping("/upload/ocr_flask")
    public ResponseEntity<String> saveAndSendImage(@RequestParam("image") MultipartFile file) {
        try {
            String imageUrl = menuService.saveAndSendImage(file);
            return ResponseEntity.ok(imageUrl);
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Failed to upload image: " + e.getMessage());
        }
    }

    //메뉴판 음식 설명_GPT
    @PostMapping("/upload/gpt_flask")
    public ResponseEntity<String> gptString(@RequestBody String input) {
        try {
            String response = menuService.gptString(input);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing request: " + e.getMessage());
        }
    }

    // 음식 기록 저장
    @PostMapping("/upload/post")
    public ResponseEntity<RecordEntity> recordPost (@RequestParam("category") String category,
                                                     @RequestParam("image") MultipartFile file,
                                                     @RequestParam("comment") String comment) throws IOException {

        RecordEntity record = menuService.recordPost(category, file, comment);
        return ResponseEntity.ok(record);
    }

    //카테고리별 음식 기록 조회
    @GetMapping("/category/{category}")
    public ResponseEntity<List<RecordDTO>> getRecordsByCategory(@PathVariable String category) {
        List<RecordDTO> records = menuService.getRecordsByCategory(category);
        return ResponseEntity.ok(records);
    }

}
