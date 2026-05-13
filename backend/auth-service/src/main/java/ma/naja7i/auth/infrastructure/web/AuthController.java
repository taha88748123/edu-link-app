package ma.naja7i.auth.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.naja7i.auth.application.AuthService;
import ma.naja7i.auth.application.PasswordResetService;
import ma.naja7i.auth.application.TokenService;
import ma.naja7i.auth.domain.User;
import ma.naja7i.auth.infrastructure.web.dto.LoginRequest;
import ma.naja7i.auth.infrastructure.web.dto.MeResponse;
import ma.naja7i.auth.infrastructure.web.dto.PasswordResetDto;
import ma.naja7i.auth.infrastructure.web.dto.PasswordResetRequestDto;
import ma.naja7i.auth.infrastructure.web.dto.RefreshRequest;
import ma.naja7i.auth.infrastructure.web.dto.RegisterRequest;
import ma.naja7i.auth.infrastructure.web.dto.RegisterResponse;
import ma.naja7i.auth.infrastructure.web.dto.ResendOtpRequest;
import ma.naja7i.auth.infrastructure.web.dto.TokenResponse;
import ma.naja7i.auth.infrastructure.web.dto.VerifyOtpRequest;
import ma.naja7i.shared.dto.ApiResponse;
import ma.naja7i.shared.security.UserContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Authentication, OTP & token endpoints")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final TokenService tokenService;

    @PostMapping("/register")
    @Operation(summary = "Create a PENDING_OTP user account and send an OTP")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(
            @Valid @RequestBody RegisterRequest body,
            HttpServletRequest request) {
        RegisterResponse out = authService.register(
                body,
                RequestMetadata.clientIp(request),
                RequestMetadata.userAgent(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(out));
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Verify the OTP code and activate the account")
    public ApiResponse<TokenResponse> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest body,
            HttpServletRequest request) {
        return ApiResponse.of(authService.verifyOtp(
                body.userId(),
                body.code(),
                RequestMetadata.clientIp(request),
                RequestMetadata.userAgent(request)));
    }

    @PostMapping("/resend-otp")
    @Operation(summary = "Re-send an OTP code (rate limited)")
    public ApiResponse<Void> resendOtp(
            @Valid @RequestBody ResendOtpRequest body,
            HttpServletRequest request) {
        authService.resendOtp(
                body.userId(),
                body.channel(),
                RequestMetadata.clientIp(request),
                RequestMetadata.userAgent(request));
        return ApiResponse.<Void>of(null);
    }

    @PostMapping("/login")
    @Operation(summary = "Username/password login. Returns access + refresh tokens.")
    public ApiResponse<TokenResponse> login(
            @Valid @RequestBody LoginRequest body,
            HttpServletRequest request) {
        return ApiResponse.of(authService.login(
                body,
                RequestMetadata.clientIp(request),
                RequestMetadata.userAgent(request)));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rotate the refresh token and issue a new access token")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshRequest body) {
        return ApiResponse.of(tokenService.rotate(body.refreshToken()));
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke the presented refresh token (authenticated)")
    public ApiResponse<Void> logout(
            @AuthUser UserContext me,
            @RequestBody(required = false) RefreshRequest body,
            HttpServletRequest request) {
        String refresh = body == null ? null : body.refreshToken();
        authService.logout(
                me.userId(),
                refresh,
                RequestMetadata.clientIp(request),
                RequestMetadata.userAgent(request));
        return ApiResponse.<Void>of(null);
    }

    @PostMapping("/password/reset-request")
    @Operation(summary = "Send a password-reset link/token")
    public ApiResponse<Void> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequestDto body,
            HttpServletRequest request) {
        passwordResetService.requestReset(
                body.phoneOrEmail(),
                RequestMetadata.clientIp(request),
                RequestMetadata.userAgent(request));
        return ApiResponse.<Void>of(null);
    }

    @PostMapping("/password/reset")
    @Operation(summary = "Reset the password using a token and revoke all sessions")
    public ApiResponse<Void> resetPassword(
            @Valid @RequestBody PasswordResetDto body,
            HttpServletRequest request) {
        passwordResetService.resetPassword(
                body.resetToken(),
                body.newPassword(),
                RequestMetadata.clientIp(request),
                RequestMetadata.userAgent(request));
        return ApiResponse.<Void>of(null);
    }

    @GetMapping("/me")
    @Operation(summary = "Return the authenticated user's basic profile")
    public ApiResponse<MeResponse> me(@AuthUser UserContext ctx) {
        User u = authService.getById(ctx.userId());
        return ApiResponse.of(new MeResponse(
                u.getId(),
                u.getEmail(),
                u.getPhone(),
                u.getRole(),
                u.getStatus(),
                u.getLocale(),
                u.isEmailVerified(),
                u.isPhoneVerified(),
                u.getLastLoginAt()));
    }
}
