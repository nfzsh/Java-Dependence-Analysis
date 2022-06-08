package cn.lipg.instrument.jda;

import javassist.NotFoundException;
import javassist.expr.Expr;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author lipangeng, Email:lipg@outlook.com
 * Created on 2022/6/6 10:44
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotFountRecord {
    private Expr expr;
    /** 原始异常信息 */
    private NotFoundException exception;
    /** 调用方源文件名称 */
    private String originFileName;
    /** 调用方源文件行 */
    private Integer originLineNumber;

}
