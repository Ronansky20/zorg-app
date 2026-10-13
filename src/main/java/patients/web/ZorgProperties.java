package patients.web;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("zorg")
public record ZorgProperties(String dataDir) {}
