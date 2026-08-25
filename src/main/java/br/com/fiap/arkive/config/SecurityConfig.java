package br.com.fiap.arkive.config;

import br.com.fiap.arkive.security.ArkiveUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;

@Configuration
public class SecurityConfig {

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public BasicAuthenticationEntryPoint basicAuthenticationEntryPoint() {
		BasicAuthenticationEntryPoint entryPoint = new BasicAuthenticationEntryPoint();
		entryPoint.setRealmName("ArkIve API");
		return entryPoint;
	}

	@Bean
	@Profile("!local-nodb")
	public DaoAuthenticationProvider authenticationProvider(ArkiveUserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider((UserDetailsService) userDetailsService);
		provider.setPasswordEncoder(passwordEncoder);
		return provider;
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, BasicAuthenticationEntryPoint basicAuthenticationEntryPoint) throws Exception {
		return http
				.csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
				.exceptionHandling(exceptionHandling -> exceptionHandling
						.defaultAuthenticationEntryPointFor(basicAuthenticationEntryPoint, request -> request.getRequestURI().startsWith(request.getContextPath() + "/api/"))
						.defaultAuthenticationEntryPointFor(new LoginUrlAuthenticationEntryPoint("/login"), AnyRequestMatcher.INSTANCE)
				)
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers(
								"/api/health",
								"/swagger-ui/**",
								"/swagger-ui.html",
								"/v3/api-docs/**",
								"/css/**",
								"/js/**",
								"/images/**",
								"/login"
						).permitAll()
						.requestMatchers("/sysadmin/**").hasRole("SYSADMIN")
						.requestMatchers("/admin/**").hasAnyRole("SYSADMIN", "ADMIN_CLINICA")
						.requestMatchers("/api/**").authenticated()
						.anyRequest().authenticated()
				)
				.formLogin(formLogin -> formLogin
						.loginPage("/login")
						.loginProcessingUrl("/login")
						.failureUrl("/login?error")
						.permitAll()
				)
				.logout(logout -> logout
						.logoutSuccessUrl("/login?logout")
						.permitAll()
				)
				.httpBasic(Customizer.withDefaults())
				.build();
	}

}
