package com.opicer.api.user.infrastructure;

import com.opicer.api.user.domain.AuthProvider;
import com.opicer.api.user.domain.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {
	Optional<User> findByProviderAndProviderId(AuthProvider provider, String providerId);
}
