package bg.softuni.parkzone.config;

import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

            http
                    .authorizeHttpRequests(matchers -> matchers
                            .requestMatchers(PathRequest.toStaticResources().atCommonLocations())
                            .permitAll()

                            .requestMatchers("/", "/login", "/register", "/error")
                            .permitAll()

                            .requestMatchers("/admin/**")
                            .hasRole("ADMIN")

                            .anyRequest()
                            .authenticated()
                    )
                    .formLogin(form -> form
                            .loginPage("/login")
                            .loginProcessingUrl("/login")
                            .usernameParameter("email")
                            .passwordParameter("password")
                            .defaultSuccessUrl("/home", true)
                            .failureHandler(authenticationFailureHandler())
                            .permitAll()
                    )
                    .logout(logout -> logout
                            .logoutRequestMatcher(new AntPathRequestMatcher("/logout", "GET"))
                            .logoutSuccessUrl("/")
                            .invalidateHttpSession(true)
                            .deleteCookies("JSESSIONID")
                    );

            return http.build();
        }

    @Bean
    public AuthenticationFailureHandler authenticationFailureHandler() {
        return (request, response, exception) -> {

            if (exception instanceof DisabledException) {
                response.sendRedirect("/login?disabled");
                return;
            }

            response.sendRedirect("/login?error");
        };
    }
}
