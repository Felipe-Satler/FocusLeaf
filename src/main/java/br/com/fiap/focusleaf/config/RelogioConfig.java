package br.com.fiap.focusleaf.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Relógio da aplicação. Fica como bean para que os testes possam usar um horário fixo
 * e verificar durações e velocidades com valores exatos.
 */
@Configuration
public class RelogioConfig {

    @Bean
    public Clock relogio() {
        return Clock.systemUTC();
    }
}
