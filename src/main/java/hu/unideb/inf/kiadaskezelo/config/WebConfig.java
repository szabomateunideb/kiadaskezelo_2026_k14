package hu.unideb.inf.kiadaskezelo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

@Configuration
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class WebConfig {
    // Stabil JSON-formátum: content és page metaadatok, a PageImpl belső mezői nélkül.
}
