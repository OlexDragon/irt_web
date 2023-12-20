package irt.web.config;

import java.util.Base64;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import irt.web.services.UserService;

@Configuration
public class IrtWebSecurity {

	@Autowired UserDetailsService userDetailsService;
	@Autowired UserService userService;


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

		http
			.authorizeRequests()

			.antMatchers("/editor/**").authenticated()

			.antMatchers("/**").permitAll()

				.and()
			.formLogin()
				.loginPage("/editor/login")
			.permitAll()
				.and()
			.logout()
        	.logoutSuccessUrl("/")
			.permitAll()
				.and()
//	        .csrf().disable()
//	        .headers()
//				.frameOptions().sameOrigin()
//				.httpStrictTransportSecurity().disable()
//			.and()
            	.rememberMe().userDetailsService(userService).tokenValiditySeconds((int) TimeUnit.DAYS.toSeconds(21));

		return http.build();
	}

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {

    	DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userService);
        authProvider.setPasswordEncoder(encoder());
        return authProvider;
    }
     
    @Bean
    public PasswordEncoder encoder() {
        return new PasswordEncoder() {
			
			@Override
			public boolean matches(CharSequence userEntry, String encodedPassword) {

				if(encodedPassword.equals("?"))
					return  true;

				final String dbPassword = new String(Base64.getDecoder().decode(encodedPassword));

				return dbPassword.equals(userEntry);
			}
			
			@Override
			public String encode(CharSequence rawPassword) {
		        return rawPassword==null || rawPassword.equals("?")
		        		? "?"
		        				: Base64.getEncoder().encodeToString(rawPassword.toString().getBytes());
		    }
		};
    }
}
