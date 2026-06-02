package io.github.spojchil.infopilot.server.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import java.time.LocalDateTime;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

/**
 * 实体字段自动填充。插入时填充 {@code createTime / updateTime / deletedAt}，更新时刷新 {@code updateTime}。配合 {@link
 * io.github.spojchil.infopilot.server.model.entity.BaseEntity} 使用。
 */
@Component
public class MybatisMetaObjectHandler implements MetaObjectHandler {

    /** 新增记录时自动填入创建时间、更新时间，软删除标记置为 0（未删除）。 */
    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "deletedAt", Long.class, 0L);
    }

    /** 更新记录时自动刷新更新时间。 */
    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
}
