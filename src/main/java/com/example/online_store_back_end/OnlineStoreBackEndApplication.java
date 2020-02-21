package com.example.online_store_back_end;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.GlobalAuthenticationConfigurerAdapter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@SpringBootApplication
@RestController
public class OnlineStoreBackEndApplication {

	public static void main(String[] args) {
		SpringApplication.run(OnlineStoreBackEndApplication.class, args);
	}
	@Bean
	public PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}



	@Bean
	public CommandLineRunner initData(UserRepository userRepository, ProductRepository productRepository, PurchaseRepository purchaseRepository) {
		return (args) -> {
			// save a couple of users
			com.example.online_store_back_end.User u1 = new com.example.online_store_back_end.User("Jerome","j.com", "seller",passwordEncoder().encode("123"));
			com.example.online_store_back_end.User u2 = new com.example.online_store_back_end.User("Bob","b.com", "customer",passwordEncoder().encode("123"));

			userRepository.save(u1);userRepository.save(u2);

			Product p1 = new Product("Banana", 4, "Very nice Banana", "fruit", 34,u1);
			Product p2 = new Product("Apple", 7, "Very nice Apple", "fruit", 22,u1);
			Product p3 = new Product("Strawberry", 4, "Very nice Strawberry", "fruit", 4,u1);
			Product p4 = new Product("Pineaple", 2, "Very nice Pineaple", "fruit", 45,u1);
			Product p5 = new Product("Melon", 4, "Very nice Melon", "fruit", 34,u1);
			productRepository.save(p1);productRepository.save(p2);productRepository.save(p3);productRepository.save(p4);productRepository.save(p5);


			Date date = new Date();
			Set<Product> s1 = new HashSet<>();
			Set<Product> s2 = new HashSet<>();
			s1.add(p1);s1.add(p3);s1.add(p4);
			s2.add(p1);s2.add(p5);s2.add(p2);
//			Purchase pu1 = new Purchase(s1,u2,date);
//			Purchase pu2 = new Purchase(s2,u2,date);
//			purchaseRepository.save(pu1);purchaseRepository.save(pu2);

		};
	}
}



@Configuration
class WebSecurityConfiguration extends GlobalAuthenticationConfigurerAdapter {
	@Autowired
UserRepository userRepository;

	@Override
	public void init(AuthenticationManagerBuilder auth) throws Exception {
		auth.userDetailsService(inputName-> {

			com.example.online_store_back_end.User user = userRepository.findByEmail(inputName);
			if (user != null) {
				return new User(user.getEmail(), user.getPassword(),
						AuthorityUtils.createAuthorityList(user.getRole()));
			}
			else {
				throw new UsernameNotFoundException("Unknown user: " + inputName);
			}
		});
	}

}

@Configuration
@EnableWebSecurity
class WebSecurityConfig extends WebSecurityConfigurerAdapter {
	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http.authorizeRequests()
				.antMatchers("/api/**").permitAll()
				.antMatchers("/api/signup/**").permitAll()
				.antMatchers("/api/login").permitAll()
				.antMatchers("/h2-console/**").permitAll()
				.antMatchers("/login.html").permitAll()
				.antMatchers("/login.js").permitAll()
				.antMatchers("/addProduct").hasAnyAuthority("seller")
				.antMatchers("/purchase").permitAll()
				.anyRequest()
				.fullyAuthenticated();
		http.formLogin()
				.usernameParameter("email")
				.passwordParameter("pwd")
				.loginPage("/api/login");
		http.logout().logoutUrl("/api/logout");
		// turn off checking for CSRF tokens
		http.csrf().disable();
//		System.out.println("login request received");
		// if user is not authenticated, just send an authentication failure response
		http.exceptionHandling().authenticationEntryPoint((req, res, exc) -> res.sendError(HttpServletResponse.SC_UNAUTHORIZED));
		// if login is successful, just clear the flags asking for authentication
		http.formLogin().successHandler((req, res, auth) -> clearAuthenticationAttributes(req));
		// if login fails, just send an authentication failure response
		http.formLogin().failureHandler((req, res, exc) -> res.sendError(HttpServletResponse.SC_UNAUTHORIZED));
		// if logout is successful, just send a success response
		http.logout().logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler());
		http.headers().frameOptions().disable();
	}


	private void clearAuthenticationAttributes(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session != null) {
			session.removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
		}
	}

}