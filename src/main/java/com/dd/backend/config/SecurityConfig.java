package com.dd.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuração base de segurança (stateless).
 *
 * <p>A autenticação via JWT será plugada aqui em seguida: basta adicionar o
 * filtro de JWT antes do {@code UsernamePasswordAuthenticationFilter} e
 * remover o {@code httpBasic} quando o fluxo de login estiver pronto.</p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // API stateless: não guarda sessão no servidor
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Documentação da API liberada
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()
                        // Todo o resto exige autenticação
                        .anyRequest().authenticated()
                )
                // Provisional: login com o usuário gerado no console (user/<senha>)
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}
