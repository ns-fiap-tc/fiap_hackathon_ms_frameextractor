package br.com.fiap.hacka.frameextractorservice.infra.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;

@Configuration
public class WebConfig {
    private static final String CHARSET_ENCODING = "UTF-8";

    @Bean
    public LocaleResolver localeResolver(){
        CookieLocaleResolver cookieLocaleResolver = new CookieLocaleResolver();
        cookieLocaleResolver.setDefaultLocale(StringUtils.parseLocaleString("pt_BR"));
        return cookieLocaleResolver;
    }

    @Bean
    public MessageSource messageSource(){
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasenames("classpath:msgs/mensagens");
        messageSource.setUseCodeAsDefaultMessage(true);
        messageSource.setDefaultEncoding(CHARSET_ENCODING);
        messageSource.setCacheSeconds(0);
        return messageSource;
    }

    @Bean(destroyMethod = "shutdown")
    public ExecutorService fileProcessingExecutor(
            MeterRegistry meterRegistry,
            @Value("${file.processing.executor.threads}") int poolSize) {

        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(poolSize);

        // Wrap with Micrometer metrics
        return ExecutorServiceMetrics.monitor(
                meterRegistry,
                executor,
                "fileProcessingExecutor"
        );
    }
}