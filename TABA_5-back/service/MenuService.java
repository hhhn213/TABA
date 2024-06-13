package taba.menutranslator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import taba.menutranslator.dto.RecordDTO;
import taba.menutranslator.entity.RecordEntity;
import taba.menutranslator.repository.MenuRepository;
import taba.menutranslator.repository.RecordRepository;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MenuService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private S3Client s3Client;

    @Value("${cloud.aws.region.static}")
    private String region;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    @Value("${flask.server.url}")
    private String flaskServerUrl;

    @Value("http://192.168.0.41:9999")
    private String flaskServerUrl2;

    @Value("${flask.auth.token}")
    private String flaskAuthToken;

    @Autowired
    private RecordRepository recordRepository;


    //S3에 업로드 및 url 반환
    public String uploadImage(MultipartFile file) throws IOException {
        long maxFileSize = 5242880; // 5MB
        byte[] imageBytes = file.getBytes();

        if (file.getSize() > maxFileSize) {
            imageBytes = resizeImage(file, maxFileSize);
        }

        String fileName = generateFileName(file);

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .contentType(file.getContentType())
                .contentDisposition("inline")
                .build();

        PutObjectResponse response = s3Client.putObject(putObjectRequest,
                RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

        // 파일 업로드 성공 여부 확인 (선택사항)
        if (response.sdkHttpResponse().isSuccessful()) {
            System.out.println("File uploaded successfully.");
        } else {
            System.out.println("File upload failed.");
        }

        String fileUrl = s3Client.utilities().getUrl(b -> b.bucket(bucketName).key(fileName)).toExternalForm();
        System.out.println("Generated File URL: " + fileUrl); // 로그에 URL 출력
        return fileUrl;
    }

    private byte[] resizeImage(MultipartFile file, long maxFileSize) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        Thumbnails.of(file.getInputStream())
                .size(800, 600) // 원하는 크기로 변경
                .outputFormat("jpg")
                .toOutputStream(outputStream);

        byte[] resizedImage = outputStream.toByteArray();
        outputStream.close();

        while (resizedImage.length > maxFileSize) {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(resizedImage);
            outputStream = new ByteArrayOutputStream();

            Thumbnails.of(inputStream)
                    .scale(0.8) // 크기를 줄이는 비율
                    .outputFormat("jpg")
                    .toOutputStream(outputStream);

            resizedImage = outputStream.toByteArray();
            inputStream.close();
            outputStream.close();
        }

        return resizedImage;
    }

    private String generateFileName(MultipartFile multiPart) {
        return new Date().getTime() + "-" + multiPart.getOriginalFilename().replace(" ", "_");
    }


    //음식 플라스크 연동 메서드
    public String sendImageToFlask(MultipartFile file) throws IOException {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultipartBodyBuilder builder = new MultipartBodyBuilder();

        // 파일 데이터를 MultipartBodyBuilder에 추가
        builder.part("image", new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename();
            }
        });

        HttpEntity<?> entity = new HttpEntity<>(builder.build(), headers);

        String flaskEndpoint = flaskServerUrl + "/detect";

        return restTemplate.postForObject(flaskEndpoint, entity, String.class);
    }


    //ocr 플라스크 연동 메서드
    public String saveAndSendImage(MultipartFile file) throws IOException {
        // Save the image locally
        String imageUrl = uploadImage(file);

        // Send the image URL to Flask server
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", flaskAuthToken);

        String flaskEndpoint = flaskServerUrl2 + "/API_GPT_OCR";

        // JSON 형식으로 URL 전송
        Map<String, String> requestMap = new HashMap<>();
        requestMap.put("query", imageUrl);

        ObjectMapper objectMapper = new ObjectMapper();
        String jsonRequest = objectMapper.writeValueAsString(requestMap);

        HttpEntity<String> entity = new HttpEntity<>(jsonRequest, headers);

        String response = restTemplate.postForObject(flaskEndpoint, entity, String.class);

        return response;
    }

    //메뉴판 음식 설명_GPT
    public String gptString(String input) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", flaskAuthToken);

        String flaskEndpoint2 = flaskServerUrl2 + "/API_GPT_CHAT";

        Map<String, String> requestMap = new HashMap<>();
        requestMap.put("query", input);
        ObjectMapper objectMapper = new ObjectMapper();
        //String jsonRequest = objectMapper.writeValueAsString(requestMap);
        String jsonRequest;
        try {
            jsonRequest = objectMapper.writeValueAsString(requestMap);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting request to JSON", e);
        }

        HttpEntity<String> entity = new HttpEntity<>(jsonRequest, headers);

//        String response = restTemplate.postForObject(flaskEndpoint2, entity, String.class);
//
//        return response;

        try {
            ResponseEntity<String> response = restTemplate.exchange(flaskEndpoint2, HttpMethod.POST, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            } else {
                throw new RuntimeException("Error processing request: " + response.getStatusCode().toString());
            }
        } catch (HttpClientErrorException e) {
            throw new RuntimeException("Error processing request: " + e.getMessage());
        }
    }



    //저장기능
    public RecordEntity recordPost(String category, MultipartFile file, String comment) throws IOException {
        String imageUrl = uploadImage(file);

        RecordEntity record = new RecordEntity();
        record.setFcate(category);
        record.setFimage(imageUrl);
        record.setFpost(comment);
        return recordRepository.save(record);
    }

    //조회기능
    public List<RecordDTO> getRecordsByCategory(String category) {
        System.out.println(recordRepository.findRecordsByCategory(category));
        return recordRepository.findRecordsByCategory(category);
    }
}
