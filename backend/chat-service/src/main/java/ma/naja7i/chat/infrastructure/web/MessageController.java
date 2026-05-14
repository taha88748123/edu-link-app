package ma.naja7i.chat.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.naja7i.chat.application.ConversationService;
import ma.naja7i.chat.application.MessageService;
import ma.naja7i.chat.application.PresenceService;
import ma.naja7i.chat.domain.Message;
import ma.naja7i.chat.infrastructure.web.dto.CreateMessageRequest;
import ma.naja7i.chat.infrastructure.web.dto.MessageView;
import ma.naja7i.chat.infrastructure.web.dto.ReadRequest;
import ma.naja7i.shared.dto.ApiResponse;
import ma.naja7i.shared.security.UserContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chat/conversations/{id}")
@RequiredArgsConstructor
@Tag(name = "Chat — Messages", description = "List, send and read messages within a conversation")
public class MessageController {

    private final MessageService messageService;
    private final ConversationService conversationService;
    private final PresenceService presenceService;

    @GetMapping("/messages")
    @PreAuthorize("hasAnyRole('STUDENT','PROF','ADMIN')")
    @Operation(summary = "Paginated message history (cursor-based on msgId).")
    public ApiResponse<List<MessageView>> list(@AuthUser UserContext ctx,
                                               @PathVariable("id") UUID conversationId,
                                               @RequestParam(required = false) UUID before,
                                               @RequestParam(defaultValue = "50") int size) {
        List<Message> page = messageService.listMessages(conversationId, ctx.userId(), ctx.role(),
                before, size);
        return ApiResponse.of(page.stream().map(MessageView::of).toList());
    }

    @PostMapping("/messages")
    @PreAuthorize("hasAnyRole('STUDENT','PROF')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Post a new message. Anti-bypass moderation runs synchronously.")
    public ApiResponse<MessageView> send(@AuthUser UserContext ctx,
                                         @PathVariable("id") UUID conversationId,
                                         @Valid @RequestBody CreateMessageRequest body) {
        Message saved = messageService.send(conversationId, ctx.userId(), ctx.role(),
                body.type(), body.body(), body.attachments());
        return ApiResponse.of(MessageView.of(saved));
    }

    @PostMapping("/typing")
    @PreAuthorize("hasAnyRole('STUDENT','PROF')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Mark the caller as typing (Redis TTL 5s).")
    public void typing(@AuthUser UserContext ctx, @PathVariable("id") UUID conversationId) {
        // Authorise: must be a participant of the conversation.
        conversationService.getOwned(conversationId, ctx.userId(), ctx.role());
        presenceService.markTyping(conversationId, ctx.userId());
    }

    @PostMapping("/read")
    @PreAuthorize("hasAnyRole('STUDENT','PROF')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Mark messages as read up to (and including) lastReadMessageId.")
    public void markRead(@AuthUser UserContext ctx,
                         @PathVariable("id") UUID conversationId,
                         @Valid @RequestBody ReadRequest body) {
        messageService.markRead(conversationId, ctx.userId(), ctx.role(), body.lastReadMessageId());
    }
}
