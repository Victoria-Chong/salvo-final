package com.codeoftheweb.salvo;

import com.codeoftheweb.salvo.Class.*;
import com.codeoftheweb.salvo.Repository.*;
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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.Arrays;

@SpringBootApplication
public class SalvoApplication {
	@Bean
	public PasswordEncoder passwordEncoder(){
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

	public static void main(String[] args) {
		SpringApplication.run(SalvoApplication.class, args);
	}

	@Bean
	public CommandLineRunner initData(PlayerRepository playerRepository,
									  GameRepository gameRepository,
									  GamePlayerRepository gamePlayerRepository,
									  ShipRepository shipRepository,
									  SalvoRepository salvoRepository,
									  ScoreRepository scoreRepository) {
		return (args) -> {
			Player player1 = new Player("vicky@gmail.com", passwordEncoder().encode("123"));
			Player player2 = new Player("paksu@gmail.com", passwordEncoder().encode("123"));
			playerRepository.save(player1);
			playerRepository.save(player2);
			Game game1 = new Game(LocalDateTime.now());
			Game game2 = new Game(LocalDateTime.now().plusHours(1));
			Game game3 = new Game(LocalDateTime.now().plusHours(2));
			gameRepository.save(game1);
			gameRepository.save(game2);
			gameRepository.save(game3);
			GamePlayer gamePlayer1 = new GamePlayer(player1, game1, LocalDateTime.now());
			GamePlayer gamePlayer2 = new GamePlayer(player2, game1, LocalDateTime.now());
			GamePlayer gamePlayer3 = new GamePlayer(player2, game3, LocalDateTime.now());
			gamePlayerRepository.save(gamePlayer1);
			gamePlayerRepository.save(gamePlayer2);
			gamePlayerRepository.save(gamePlayer3);
//			Ship ship4 = new Ship("carrier", gamePlayer1, Arrays.asList("A2","A3","A4","A5","A6"));
//			Ship ship5 = new Ship("battleship", gamePlayer1, Arrays.asList("E4","F4","G4","H4"));
//			Ship ship2 = new Ship("submarine", gamePlayer1, Arrays.asList("E1","F1","G1"));
//			Ship ship1 = new Ship("destroyer", gamePlayer1, Arrays.asList("H1","H2","H3"));
//			Ship ship3 = new Ship("patrolboat", gamePlayer1, Arrays.asList("B4","B5"));
//			Ship ship6 = new Ship("carrier", gamePlayer2, Arrays.asList("A2","A3","A4","A5","A6"));
//			Ship ship7 = new Ship("battleship", gamePlayer2, Arrays.asList("E4","F4","G4","H4"));
//			Ship ship8 = new Ship("submarine", gamePlayer2, Arrays.asList("E1","F1","G1"));
//			Ship ship9 = new Ship("destroyer", gamePlayer2, Arrays.asList("H1","H2","H3"));
//			Ship ship10 = new Ship("patrolboat", gamePlayer2, Arrays.asList("B4","B5"));
//			shipRepository.save(ship1);
//			shipRepository.save(ship2);
//			shipRepository.save(ship3);
//			shipRepository.save(ship4);
//			shipRepository.save(ship5);
//			shipRepository.save(ship6);
//			shipRepository.save(ship7);
//			shipRepository.save(ship8);
//			shipRepository.save(ship9);
//			shipRepository.save(ship10);
//			Salvo salvo1 = new Salvo(1, gamePlayer1, Arrays.asList("H2","H3","H4"));
//			Salvo salvo2 = new Salvo(2, gamePlayer1, Arrays.asList("E1","F1","G1"));
//			Salvo salvo3 = new Salvo(1, gamePlayer2, Arrays.asList("B4","B5"));
//			Salvo salvo4 = new Salvo(2, gamePlayer2, Arrays.asList("E4","F5","G2"));
//			Salvo salvo5 = new Salvo(3, gamePlayer1, Arrays.asList("B4","B5"));
//			Salvo salvo6 = new Salvo(3, gamePlayer2, Arrays.asList("A4","A5","A2"));
//			salvoRepository.save(salvo1);
//			salvoRepository.save(salvo2);
//			salvoRepository.save(salvo3);
//			salvoRepository.save(salvo4);
//			salvoRepository.save(salvo5);
//			salvoRepository.save(salvo6);
//			Score score1 = new Score(1, LocalDateTime.now(), player1, game1);
//			Score score2 = new Score(0, LocalDateTime.now(), player2, game1);
//			scoreRepository.save(score1);
//			scoreRepository.save(score2);
		};
	}
}

@Configuration
class WebSecurityConfiguration extends GlobalAuthenticationConfigurerAdapter {

	@Autowired
	PlayerRepository playerRepository;

	@Override
	public void init(AuthenticationManagerBuilder auth) throws Exception {
		auth.userDetailsService(inputName-> {
			Player player = playerRepository.findByUserName(inputName);
			if (player != null) {
				return new User(player.getuserName(), player.getPassword(),
						AuthorityUtils.createAuthorityList("USER"));
			} else {
				throw new UsernameNotFoundException("Unknown user: " + inputName);
			}
		});
	}
}
@Configuration
@EnableWebSecurity
class WebSecurityConfig extends WebSecurityConfigurerAdapter {
	@Autowired
	PasswordEncoder passwordEncoder;

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http.authorizeRequests()
				.antMatchers("/api/players").permitAll()
				.antMatchers("/api/login").permitAll()
				.antMatchers("/web/**").permitAll()
				.antMatchers("/api/games").permitAll()
				.antMatchers("/h2-console/").permitAll()
				.antMatchers("**").hasAuthority("USER")
				.and().headers().frameOptions().disable()
				.and().csrf().ignoringAntMatchers("/h2-console/")
				.and()
				.cors().disable();
		http.formLogin()
				.usernameParameter("name")
				.passwordParameter("pwd")
				.loginPage("/api/login");;
		http.logout().logoutUrl("/api/logout");

		http.csrf().disable();

		http.exceptionHandling().authenticationEntryPoint((req, res, exc) -> res.sendError(HttpServletResponse.SC_UNAUTHORIZED));

		http.formLogin().successHandler((req, res, auth) -> clearAuthenticationAttributes(req));

		http.formLogin().failureHandler((req, res, exc) -> res.sendError(HttpServletResponse.SC_UNAUTHORIZED));

		http.logout().logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler());
	}

	private void clearAuthenticationAttributes(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session != null) {
			session.removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
		}
	}

}
