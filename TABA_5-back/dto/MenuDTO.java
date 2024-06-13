package taba.menutranslator.dto;

import lombok.Getter;
import org.springframework.web.multipart.MultipartFile;

@Getter
public class MenuDTO {
    // Getters and setters
    private String ename;

    public void setEname(String e_name) {
        this.ename = ename;
    }

    private MultipartFile file;
    private String description;

    // Getters and Setters
    public MultipartFile getFile() {
        return file;
    }

    public void setFile(MultipartFile file) {
        this.file = file;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }


    
}
