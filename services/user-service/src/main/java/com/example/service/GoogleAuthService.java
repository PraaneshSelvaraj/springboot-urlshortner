package com.example.service;

import com.example.dto.GoogleUserInfo;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import java.util.Collections;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GoogleAuthService {

  private final GoogleIdTokenVerifier verifier;

  public GoogleAuthService(@Value("${google.client-id}") String clientId) {
    this.verifier =
        new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
            .setAudience(Collections.singletonList(clientId))
            .build();
  }

  public Optional<GoogleUserInfo> verifyToken(String idToken) {
    try {
      GoogleIdToken googleIdToken = verifier.verify(idToken);
      if (googleIdToken != null) {
        GoogleIdToken.Payload payload = googleIdToken.getPayload();

        String googleId = payload.getSubject();
        String email = payload.getEmail();
        String name = (String) payload.get("name");
        String picture = (String) payload.get("picture");

        return Optional.of(new GoogleUserInfo(googleId, email, name, picture));
      }
      return Optional.empty();
    } catch (Exception e) {
      System.err.println("Error verifying Google ID token: " + e.getMessage());
      return Optional.empty();
    }
  }
}
