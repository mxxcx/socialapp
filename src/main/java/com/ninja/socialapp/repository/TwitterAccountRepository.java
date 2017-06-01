package com.ninja.socialapp.repository;

import com.ninja.socialapp.domain.TwitterAccount;
import com.ninja.socialapp.domain.enumeration.TwitterStatus;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.*;

import java.util.List;


/**
 * Spring Data JPA repository for the TwitterAccount entity.
 */
@SuppressWarnings("unused")
@Repository
public interface TwitterAccountRepository extends JpaRepository<TwitterAccount,Long> {

    List<TwitterAccount> findByStatus(TwitterStatus status);
}
