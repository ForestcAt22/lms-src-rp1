package jp.co.sss.lms.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint; // ここは「jakarta」であることを再確認！
import jakarta.validation.Payload;     // ここも「jakarta」であることを再確認！

@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = TimeValidator.class)
public @interface ValidTime {

    // メッセージキーをシンプルにする（messages.propertiesにこのキーを追加してください）
    String message() default "{attendance.timeValidationError}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    @Target({ ElementType.TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @interface List {
        ValidTime[] value();
    }
}