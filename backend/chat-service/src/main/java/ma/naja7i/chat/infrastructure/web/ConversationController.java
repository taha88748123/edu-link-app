package ma.naja7i.chat.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import ma.naja7i.chat.application.ConversationService;
import ma.naja7i.chat.domain.Conversation;
import ma.naja7i.chat.domain.ConversationStatus;
import ma.naja7i.chat.infrastructure.web.dto.ConversationView;
import ma.naja7i.shared.dto.ApiResponse;
import ma.naja7i.shared.security.UserContext;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chat/conversations")
@RequiredArgsConstructor
@Tag(name = "Chat — Conversations", description = "List, fetch and mark conversations as opened")
public class ConversationController {

    private final ConversationService conversationService;

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('STUDENT','PROF')")
    @Operation(summary = "List my conversations (sorted by lastMessageAt desc)")
    public ApiResponse<List<ConversationView>> mine(@AuthUser UserContext ctx,
                                                    @RequestParam(required = false) ConversationStatus status,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        Page<Conversation> result = conversationService.listForCaller(ctx.userId(), status, page, size);
        List<ConversationView> body = result.getContent().stream()
                .map(c -> ConversationView.of(c, ctx.userId()))
                .toList();
        return ApiResponse.of(body,
                new ApiResponse.PageMeta(result.getNumber(), result.getSize(), result.getTotalElements()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDENT','PROF','ADMIN')")
    @Operation(summary = "Get a conversation. Also resets the viewer's unread counter.")
    public ApiResponse<ConversationView> get(@AuthUser UserContext ctx,
                                             @PathVariable("id") UUID id) {
        Conversation conv = conversationService.getOwned(id, ctx.userId(), ctx.role());
        // "Opening" the conversation resets the viewer's unread counter.
        Conversation refreshed = conversationService.resetUnreadFor(id, ctx.userId());
        // Prefer the refreshed entity (carries the updated counts) when available.
        Conversation effective = refreshed == null ? conv : refreshed;
        return ApiResponse.of(ConversationView.of(effective, ctx.userId()));
    }
}
