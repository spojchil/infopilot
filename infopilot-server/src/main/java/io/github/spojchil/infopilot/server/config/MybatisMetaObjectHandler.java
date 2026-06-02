package io.github.spojchil.infopilot.server.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import java.time.LocalDateTime;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

@Component
public class MybatisMetaObjectHandler implements MetaObjectHandler {

  @Override
  public void insertFill(MetaObject metaObject) {
    LocalDateTime now = LocalDateTime.now();
    this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
    this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
    this.strictInsertFill(metaObject, "deletedAt", Long.class, 0L);
  }

  @Override
  public void updateFill(MetaObject metaObject) {
    this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
  }
}
