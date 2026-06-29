package org.traducao.projeto.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.traducao.projeto.legendasExtracao.infrastructure.config.ExtratorProperties;
import org.traducao.projeto.remuxer.infrastructure.config.RemuxerProperties;
import org.traducao.projeto.traducao.infrastructure.config.LlmProperties;
import org.traducao.projeto.traducao.infrastructure.config.TradutorProperties;

@Configuration
@EnableConfigurationProperties({
    LlmProperties.class,
    TradutorProperties.class,
    RemuxerProperties.class,
    ExtratorProperties.class
})
public class AppConfig {
}
