package taba.menutranslator.entity;

import jakarta.persistence.*;

@Entity
public class MenuEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String menu;
    @Column
    private String ename;
    @Column
    private String eng_menu;
    @Column
    private String info;
    @Column
    private String allergy;
    @Column
    private String spicy;

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMenu() {
        return menu;
    }

    public void setMenu(String menu) {
        this.menu = menu;
    }

    public String getEname() {
        return ename;
    }

    public void setEname(String e_name) {
        this.ename = ename;
    }

    public String getEng_menu() {
        return eng_menu;
    }

    public void setEng_menu(String eng_menu) {
        this.eng_menu = eng_menu;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public String getAllergy() {
        return allergy;
    }

    public void setAllergy(String allergy) {
        this.allergy = allergy;
    }

    public String getSpicy() {
        return spicy;
    }

    public void setSpicy(String spicy) {
        this.spicy = spicy;
    }
}
