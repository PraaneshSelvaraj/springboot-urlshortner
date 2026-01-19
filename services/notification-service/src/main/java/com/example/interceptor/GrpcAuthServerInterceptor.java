package com.example.interceptor;

import com.example.context.GrpcUserContext;
import com.example.util.JwtUtil;
import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class GrpcAuthServerInterceptor implements ServerInterceptor {

  private static final Metadata.Key<String> AUTHORIZATION_KEY =
      Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);

  private final JwtUtil jwtUtil;

  private static final Set<String> PUBLIC_METHODS = Set.of();

  private static class NoOpServerCallListener<ReqT> extends ServerCall.Listener<ReqT> {}

  public GrpcAuthServerInterceptor(JwtUtil jwtUtil) {
    this.jwtUtil = jwtUtil;
  }

  @Override
  public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
      ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {

    String methodName = call.getMethodDescriptor().getFullMethodName();

    if (PUBLIC_METHODS.contains(methodName)) {
      return next.startCall(call, headers);
    }

    String authHeader = headers.get(AUTHORIZATION_KEY);
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      call.close(Status.UNAUTHENTICATED.withDescription("Authentication required"), new Metadata());
      return new NoOpServerCallListener<>();
    }

    String token = authHeader.substring(7);

    try {
      if (!jwtUtil.validateToken(token)) {
        call.close(
            Status.UNAUTHENTICATED.withDescription("Invalid authentication token"), new Metadata());
        return new NoOpServerCallListener<>();
      }

      String tokenType = jwtUtil.extractTokenType(token);
      if (!"auth".equalsIgnoreCase(tokenType)) {
        call.close(Status.UNAUTHENTICATED.withDescription("Invalid token type"), new Metadata());
        return new NoOpServerCallListener<>();
      }

      Long userId = jwtUtil.extractUserId(token);
      String email = jwtUtil.extractEmail(token);
      String role = jwtUtil.extractRole(token);

      GrpcUserContext.UserInfo userInfo = new GrpcUserContext.UserInfo(userId, email, role);

      Context context = Context.current().withValue(GrpcUserContext.USER_INFO_KEY, userInfo);

      return Contexts.interceptCall(context, call, headers, next);

    } catch (Exception e) {
      call.close(
          Status.UNAUTHENTICATED.withDescription("Authentication failed: " + e.getMessage()),
          new Metadata());
      return new NoOpServerCallListener<>();
    }
  }
}
