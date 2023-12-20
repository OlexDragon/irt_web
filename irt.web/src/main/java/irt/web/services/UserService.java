package irt.web.services;


import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import irt.web.bean.jpa.user.User;
import irt.web.bean.jpa.user.UserRepository;

@Service
public class UserService implements UserDetailsService  {

	@Autowired
	private UserRepository userRepository;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		final Optional<User> findByUsername = userRepository.findByUsername(username);
		return findByUsername.map(UserPrincipal::new).orElseThrow(()->new UsernameNotFoundException("User '" + username + "' does not exists."));
	}
}
