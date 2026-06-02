package io.github.spojchil.infopilot.server.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/** 实体基类。提供 UUID 主键、创建/更新时间、软删除标识。子类继承后自动获得这些字段，无需重复定义。 */
@Data
public abstract class BaseEntity implements Serializable {

    @Serial private static final long serialVersionUID = 1L;

    /** UUID 主键，由 MyBatis-Plus 自动生成。 */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 软删除时间戳（毫秒），0 表示未删除。配合 {@code logic-delete-field} 实现逻辑删除。 */
    @TableField(fill = FieldFill.INSERT)
    private Long deletedAt;
}
