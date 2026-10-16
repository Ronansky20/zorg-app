package patients.web;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import patients.Backend;
import patients.model.Profession;
import patients.model.User;
import patients.testing.InMemoryPatientRepository;
import patients.testing.InMemoryUserRepository;
import patients.web.session.LocalSignIn;
import patients.web.session.LoginThrottle;
import patients.web.session.SignIn;

import java.util.List;

@TestConfiguration
class TestWebConfig {

    static final List<User> USERS = List.of(
            new User("jansen", "Dr. Max Jansen", Profession.DOCTOR),
            new User("hendriks", "Dr. Noor Hendriks", Profession.DOCTOR),
            new User("devries", "Pieter de Vries", Profession.PHARMACIST),
            new User("bakker", "Sanne Bakker", Profession.PHYSIOTHERAPIST),
            new User("visser", "Lotte Visser", Profession.DENTIST));

    @Bean
    InMemoryPatientRepository patientRepository() {
        return new InMemoryPatientRepository();
    }

    @Bean
    SignIn signIn(InMemoryPatientRepository patients) {
        return new LocalSignIn(new Backend.Repositories(patients, new InMemoryUserRepository(USERS)));
    }

    @Bean
    LoginThrottle loginThrottle() {
        return new LoginThrottle();
    }
}
