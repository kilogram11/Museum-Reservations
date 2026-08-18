package com.museum.ai.rag.config;

import com.museum.ai.rag.support.LocalHashingEmbeddingModel;
import com.museum.ai.rag.store.InMemoryRagStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RagProperties.class)
@ConditionalOnProperty(prefix = "museum.ai.rag", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RagAutoConfiguration {

    @Bean
    public LocalHashingEmbeddingModel localHashingEmbeddingModel() {
        return new LocalHashingEmbeddingModel();
    }

    @Bean
    public InMemoryRagStore inMemoryRagStore(LocalHashingEmbeddingModel embeddingModel) {
        return new InMemoryRagStore(embeddingModel);
    }
}
