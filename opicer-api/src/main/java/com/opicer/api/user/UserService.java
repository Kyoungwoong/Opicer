package com.opicer.api.user;

import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

	private final UserRepository userRepository;

	public UserService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Transactional
	public User upsert(AuthProvider provider, String providerId, String email, String name, UserRole role) {
		Optional<User> existing = userRepository.findByProviderAndProviderId(provider, providerId);
		if (existing.isPresent()) {
			User user = existing.get();
			user.updateProfile(email, name, role);
			return user;
		}
		User user = new User(provider, providerId, email, name, role);
		return userRepository.save(user);
	}
}
