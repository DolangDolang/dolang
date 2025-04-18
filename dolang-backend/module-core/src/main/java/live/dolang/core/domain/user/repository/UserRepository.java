package live.dolang.core.domain.user.repository;

import live.dolang.core.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer>, QuerydslPredicateExecutor<User> {

    Optional<User> findByGoogleId(String googleId);
    boolean existsById(Integer id);
}
