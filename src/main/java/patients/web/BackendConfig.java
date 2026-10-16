package patients.web;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import patients.Backend;
import patients.web.session.FirebaseSignIn;
import patients.web.session.LocalSignIn;
import patients.web.session.LoginThrottle;
import patients.web.session.SignIn;

import java.nio.file.Path;

@Configuration
@EnableConfigurationProperties(ZorgProperties.class)
class BackendConfig {

    @Bean
    Backend backend(Environment environment) {
        return Backend.fromEnvironment(environment::getProperty);
    }

    @Bean
    SignIn signIn(Backend backend, ZorgProperties properties) {
        if (!backend.usesFirebase()) {
            return new LocalSignIn(Backend.json(Path.of(properties.dataDir())));
        }
        if (!backend.canSignIn()) {
            throw new IllegalStateException(Backend.SIGN_IN_SETTINGS_MISSING);
        }
        return new FirebaseSignIn(backend);
    }

    @Bean
    LoginThrottle loginThrottle() {
        return new LoginThrottle();
    }
}
