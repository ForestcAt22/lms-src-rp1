package jp.co.sss.lms; // または jp.co.sss.lms; など、Applicationクラスと同じか、そのサブパッケージ

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration; // ★追加★
import org.springframework.web.context.annotation.SessionScope;

import jp.co.sss.lms.dto.LoginUserDto;

@Configuration // ★このアノテーションが重要★
public class AppConfig {

    @Bean
    @SessionScope
    public LoginUserDto loginUserDto() {
        return new LoginUserDto();
    }
}