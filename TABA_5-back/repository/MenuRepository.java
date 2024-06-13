package taba.menutranslator.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import taba.menutranslator.entity.MenuEntity;

public interface MenuRepository extends JpaRepository<MenuEntity, Long> {
    MenuEntity findByEname(String ename);
}
