package ma.naja7i.chat.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.naja7i.chat.application.ConversationService;
import ma.naja7i.chat.domain.Conversation;
import ma.naja7i.chat.domain.ConversationStatus;
import ma.naja7i.chat.infrastructure.web.dto.ConversationView;
import ma.naja7i.chat.infrastructure.web.dto.CreateConversationRequest;
import ma.naja7i.shared.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Service-to-service endpoints protected by {@code X-Internal-Key}. The most common
 * caller is booking-service when a booking transitions into a state that requires
 * a chat channel (request / accept).
 */
@RestController
@RequestMapping("/api/v1/internal/chat")
@RequiredArgsConstructor
@Tag(name = "Chat — Internal", description = "Service-to-service endpoints (X-Internal-Key required)")
public class InternalChatController {

    private final ConversationService conversationService;

    @PostMapping("/conversations")
    @PreAuthorize("hasRole('INTERNAL')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Idempotently create a conversation for a booking.")
    public ApiResponse<ConversationView> createConversation(@Valid @RequestBody CreateConversationRequest req) {
        ConversationStatus initial = req.status() == null ? ConversationStatus.PRE_ACCEPT : req.status();
        Conversation conv = conversationService.createOrGet(
                req.bookingId(), req.profId(), req.studentId(), initial);
        return ApiResponse.of(ConversationView.of(conv, null));
    }
}
