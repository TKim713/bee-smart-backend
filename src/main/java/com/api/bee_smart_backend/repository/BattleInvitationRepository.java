package com.api.bee_smart_backend.repository;

import com.api.bee_smart_backend.model.BattleInvitation;
import com.api.bee_smart_backend.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface BattleInvitationRepository extends MongoRepository<BattleInvitation, String> {

    // Find pending invitations for a user
    List<BattleInvitation> findByInviteeAndStatusOrderByCreatedAtDesc(User invitee, String status);

    // Find all invitations sent by a user
    List<BattleInvitation> findByInviterOrderByCreatedAtDesc(User inviter);

    // Find specific invitation between two users
    Optional<BattleInvitation> findByInviterAndInviteeAndStatus(User inviter, User invitee, String status);

    // Find expired invitations
    List<BattleInvitation> findByStatusAndExpiresAtBefore(String status, Instant now);

    // Check if there's already a pending invitation between users
    boolean existsByInviterAndInviteeAndStatus(User inviter, User invitee, String status);

    // Find invitation by ID and invitee (for security)
    Optional<BattleInvitation> findByInvitationIdAndInvitee(String invitationId, User invitee);

    List<BattleInvitation> findByInviterAndCreatedAtAfter(User inviter, Instant minus);
}
