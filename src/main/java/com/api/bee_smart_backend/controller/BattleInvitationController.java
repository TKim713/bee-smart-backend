package com.api.bee_smart_backend.controller;

import com.api.bee_smart_backend.helper.request.BattleInvitationRequest;
import com.api.bee_smart_backend.helper.response.BattleInvitationResponse;
import com.api.bee_smart_backend.helper.response.InvitationActionResponse;
import com.api.bee_smart_backend.helper.response.ResponseObject;
import com.api.bee_smart_backend.service.BattleInvitationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/battle-invitations")
@RequiredArgsConstructor
public class BattleInvitationController {

    private final BattleInvitationService battleInvitationService;

    @PostMapping("/send")
    public ResponseEntity<ResponseObject<BattleInvitationResponse>> sendInvitation(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody BattleInvitationRequest request) {
        try {
            String token = authHeader.replace("Bearer ", "");
            BattleInvitationResponse response = battleInvitationService.sendInvitation(token, request);
            return ResponseEntity.ok(new ResponseObject<>(HttpStatus.OK.value(), "Invitation sent successfully", response));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), e.getMessage(), null));
        }
    }

    @PostMapping("/{invitationId}/accept")
    public ResponseEntity<ResponseObject<InvitationActionResponse>> acceptInvitation(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String invitationId) {
        try {
            String token = authHeader.replace("Bearer ", "");
            InvitationActionResponse response = battleInvitationService.acceptInvitation(token, invitationId);
            return ResponseEntity.ok(new ResponseObject<>(HttpStatus.OK.value(), "Invitation accepted", response));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), e.getMessage(), null));
        }
    }

    @PostMapping("/{invitationId}/decline")
    public ResponseEntity<ResponseObject<InvitationActionResponse>> declineInvitation(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String invitationId) {
        try {
            String token = authHeader.replace("Bearer ", "");
            InvitationActionResponse response = battleInvitationService.declineInvitation(token, invitationId);
            return ResponseEntity.ok(new ResponseObject<>(HttpStatus.OK.value(), "Invitation declined", response));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), e.getMessage(), null));
        }
    }

    @PostMapping("/{invitationId}/cancel")
    public ResponseEntity<ResponseObject<InvitationActionResponse>> cancelInvitation(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String invitationId) {
        try {
            String token = authHeader.replace("Bearer ", "");
            InvitationActionResponse response = battleInvitationService.cancelInvitation(token, invitationId);
            return ResponseEntity.ok(new ResponseObject<>(HttpStatus.OK.value(), "Invitation cancelled", response));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), e.getMessage(), null));
        }
    }

    @GetMapping("/pending")
    public ResponseEntity<ResponseObject<List<BattleInvitationResponse>>> getPendingInvitations(
            @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            List<BattleInvitationResponse> invitations = battleInvitationService.getPendingInvitations(token);
            return ResponseEntity.ok(new ResponseObject<>(HttpStatus.OK.value(), "Pending invitations retrieved", invitations));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), e.getMessage(), Collections.emptyList()));
        }
    }

    @GetMapping("/sent")
    public ResponseEntity<ResponseObject<List<BattleInvitationResponse>>> getSentInvitations(
            @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            List<BattleInvitationResponse> invitations = battleInvitationService.getSentInvitations(token);
            return ResponseEntity.ok(new ResponseObject<>(HttpStatus.OK.value(), "Sent invitations retrieved", invitations));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), e.getMessage(), Collections.emptyList()));
        }
    }

    @GetMapping("/{invitationId}")
    public ResponseEntity<ResponseObject<BattleInvitationResponse>> getInvitationDetails(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String invitationId) {
        try {
            String token = authHeader.replace("Bearer ", "");
            // This method needs to be added to the service
            BattleInvitationResponse invitation = battleInvitationService.getInvitationById(token, invitationId);
            return ResponseEntity.ok(new ResponseObject<>(HttpStatus.OK.value(), "Invitation details retrieved", invitation));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), e.getMessage(), null));
        }
    }
}