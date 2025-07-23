package jp.co.sss.lms.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Target({ ElementType.TYPE })
//アノテーションがいつまで表示されるのか
@Retention(RetentionPolicy.RUNTIME)
//Javadocに記述
@Documented
//アノテーションがどのValidatorクラスに検証されるかを指定
@Constraint(validatedBy = TimeValidator.class)

public @interface ValidTime {

	//バリデーション失敗時のメッセージキー
	String message()

	default "{jp.co.sss.lms.validation.ValidTime.message}";

	//バリデーショングループ定義
	Class<?>[] groups() default {};

	//ペイロード定義
	Class<? extends Payload>[] payload() default {};

	//複数の@ValidTimeを適用するためのコンテナアノテーション
	@Target({ ElementType.TYPE })
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@interface List {
		ValidTime[] value();
	}
}
