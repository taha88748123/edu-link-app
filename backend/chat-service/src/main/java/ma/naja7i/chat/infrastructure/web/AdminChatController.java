package ma.naja7i.chat.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import ma.naja7i.chat.application.ConversationService;
import ma.naja7i.chat.application.MessageService;
import ma.naja7i.chat.domain.Conversation;
import ma.naja7i.chat.domain.Message;
import ma.naja7i.chat.infrastructure.web.dto.AdminMessageView;
import ma.naja7i.chat.infrastructure.web.dto.ConversationView;
import ma.naja7i.shared.dto.ApiResponse;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * ADMIN-only endpoints for chat moderation / investigation. The most sensitive
 * action is {@code GET /admin/messages/{id}}: it returns the unmasked
 * {@code bodyOriginal} field that is hidden from public views.
 */
@RestController
@RequestMapping("/api/v1/chat/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Chat — Admin", description = "Anti-bypass investigation endpoints (ADMIN only)")
public class AdminChatController {

    private final ConversationService conversationService;
    private final MessageService messageService;

    @GetMapping("/conversations")
    @Operation(summary = "Search conversations by booking / prof / student.")
    public ApiResponse<List<ConversationView>> search(@RequestParam(required = false) UUID bookingId,
                                                      @RequestParam(required = false) UUID profId,
                                                      @RequestParam(required = false) UUID studentId,
                                                      @RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "20") int size) {
        Page<Conversation> result = conversationService.adminSearch(bookingId, profId, studentId, page, size);
        return ApiResponse.of(
                result.getContent().stream().map(c -> ConversationView.of(c, null)).toList(),
                new ApiResponse.PageMeta(result.getNumber(), result.getSize(), result.getTotalElements()));
    }

    @GetMapping("/messages/{id}")
    @Operation(summary = "Fetch a message with its bodyOriginal (UN-masked).")
    public ApiResponse<AdminMessageView> getMessage(@PathVariable("id") UUID id) {
        Message m = messageService.getForAdmin(id);
        return ApiResponse.of(AdminMessageView.of(m));
    }

    @PostMapping("/conversations/{id}:close")
    @Operation(summary = "Force-close a conversation (manual moderation action).")
    public ApiResponse<ConversationView> close(@PathVariable("id") UUID id) {
        Conversation conv = conversationService.close(id, "ADMIN_CLOSE");
        return ApiResponse.of(ConversationView.of(conv, null));
    }
}
