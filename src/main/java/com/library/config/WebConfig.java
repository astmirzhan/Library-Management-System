package com.library.config;

import com.library.interceptor.AuthInterceptor;
import com.library.interceptor.LoggingInterceptor;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import com.library.util.ConfigLoader;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.thymeleaf.spring5.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring5.view.ThymeleafViewResolver;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Spring MVC configuration class.
 * Configures Thymeleaf template engine, internationalization (i18n),
 * static resources, and HTTP interceptors.
 */
@Configuration
@EnableWebMvc
@org.springframework.context.annotation.EnableAspectJAutoProxy
@org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity(prePostEnabled = true)
@ComponentScan(basePackages = {
        "com.library.controller",
        "com.library.service",
        "com.library.dao",
        "com.library.interceptor",
        "com.library.aspect"
})
public class WebConfig implements WebMvcConfigurer {

    /**
     * Configures Thymeleaf template resolver to look in /WEB-INF/templates/.
     *
     * @return the template resolver
     */
    @Bean
    public SpringResourceTemplateResolver templateResolver() {
        SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
        resolver.setPrefix("/WEB-INF/templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resolver.setCacheable(false);
        return resolver;
    }

    /**
     * Configures the Thymeleaf template engine.
     *
     * @return the template engine
     */
    @Bean
    public SpringTemplateEngine templateEngine() {
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(templateResolver());
        engine.setEnableSpringELCompiler(true);
        return engine;
    }

    /**
     * Exposes the ConfigLoader Singleton as a Spring bean
     * so it can be injected into services like BorrowService.
     *
     * @return the ConfigLoader Singleton instance
     */
    @Bean
    public ConfigLoader configLoader() {
        return ConfigLoader.getInstance();
    }

    /**
     * Configures the Thymeleaf view resolver.
     *
     * @return the view resolver
     */
    @Bean
    public ThymeleafViewResolver viewResolver() {
        ThymeleafViewResolver resolver = new ThymeleafViewResolver();
        resolver.setTemplateEngine(templateEngine());
        resolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resolver.setOrder(1);
        return resolver;
    }

    /**
     * Configures message source for internationalization (i18n).
     *
     * @return the message source
     */
    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
        source.setBasename("classpath:messages");
        source.setDefaultEncoding(StandardCharsets.UTF_8.name());
        source.setUseCodeAsDefaultMessage(true);
        return source;
    }

    /**
     * Configures locale resolver (stored in session).
     *
     * @return the locale resolver
     */
    @Bean
    public LocaleResolver localeResolver() {
        SessionLocaleResolver resolver = new SessionLocaleResolver();
        resolver.setDefaultLocale(Locale.ENGLISH);
        return resolver;
    }

    /**
     * Allows language change via URL parameter ?lang=en or ?lang=ru.
     *
     * @return the locale change interceptor
     */
    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName("lang");
        return interceptor;
    }

    /**
     * Registers application interceptors.
     *
     * @param registry the interceptor registry
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
        registry.addInterceptor(new LoggingInterceptor());
        registry.addInterceptor(new AuthInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/login", "/register", "/logout",
                        "/css/**", "/js/**", "/images/**",
                        "/error"
                );
    }

    /**
     * Configures static resource handling (CSS, JS, images).
     *
     * @param registry the resource handler registry
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // no-cache: браузер обязан ревалидировать статику перед использованием кэша,
        // поэтому обновлённый style.css подхватывается без ручного hard-refresh.
        registry.addResourceHandler("/css/**").addResourceLocations("/static/css/")
                .setCacheControl(CacheControl.noCache());
        registry.addResourceHandler("/js/**").addResourceLocations("/static/js/")
                .setCacheControl(CacheControl.noCache());
        registry.addResourceHandler("/images/**").addResourceLocations("/static/images/")
                .setCacheControl(CacheControl.noCache());
    }
}
