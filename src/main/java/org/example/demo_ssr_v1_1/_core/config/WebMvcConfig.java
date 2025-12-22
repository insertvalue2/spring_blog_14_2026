package org.example.demo_ssr_v1_1._core.config;

import org.example.demo_ssr_v1_1._core.interceptor.AdminInterceptor;
import org.example.demo_ssr_v1_1._core.interceptor.LoginInterceptor;
import org.example.demo_ssr_v1_1._core.interceptor.SessionInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 설정 클래스
 * * 인터셉터 등록 및 URL 패턴 설정을 담당합니다.
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final LoginInterceptor loginInterceptor;
    private final SessionInterceptor sessionInterceptor;
    private final AdminInterceptor adminInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // SessionInterceptor를 모든 요청에 등록
        registry.addInterceptor(sessionInterceptor)
                .addPathPatterns("/**");
        
        // LoginInterceptor 등록
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/board/**", "/user/**", "/reply/**", "/admin/**")
                .excludePathPatterns(
                        "/login", "/join", "/logout", "/user/kakao", 
                        "/board/list", "/", "/board/{id:\\d+}", 
                        "/css/**", "/js/**", "/images/**", "/favicon.ico", "/h2-console/**"
                );
        
        // AdminInterceptor 등록
        registry.addInterceptor(adminInterceptor)
                .addPathPatterns("/admin/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:images/");
    }

    /**
     * 비밀번호 암호화를 위한 PasswordEncoder 빈 등록
     * * BCryptPasswordEncoder는 강력한 해싱 알고리즘(BCrypt)을 사용합니다.
     * 학생들을 위한 용어 설명:
     * * 1. 해싱 (Hashing):
     * - 입력받은 데이터(비밀번호)를 고정된 길이의 난수(알 수 없는 문자열)로 변환하는 것입니다.
     * - 예: "1234" -> "$2a$10$D8... (아주 긴 문자열)"로 바뀝니다.
     * * 2. 단방향 (One-Way):
     * - "갈아버린 고기"와 같습니다. 고기를 갈아서 다짐육을 만들 순 있지만,
     * 다짐육을 다시 원래의 스테이크 모양으로 되돌릴 수는 없습니다.
     * - 즉, 암호화된 비밀번호를 봐도 원래 비밀번호가 "1234"인지 절대 역추적할 수 없습니다.
     * - 비밀번호 검사는 어떻게 하나요? 사용자가 입력한 값을 똑같이 해싱해서 결과가 같은지만 비교합니다.
     * * 3. 솔트 (Salt):
     * - "음식에 소금 치기"라고 생각하면 됩니다.
     * - 해싱하기 전에 원본 비밀번호에 랜덤한 문자열(소금)을 앞뒤로 붙여서 같이 버무립니다.
     * - 효과: 철수와 영희가 둘 다 비밀번호를 "1234"로 설정해도, 
     * 각자 뿌려진 소금(Salt)이 다르기 때문에 저장되는 암호문은 완전히 다르게 보입니다.
     * - 해커가 미리 만들어둔 정답지(Rainbow Table)로 해킹하는 것을 막아줍니다.
     * * @return BCryptPasswordEncoder 인스턴스 (싱글톤)
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}