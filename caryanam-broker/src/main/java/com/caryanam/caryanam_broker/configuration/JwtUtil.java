package com.caryanam.caryanam_broker.configuration;

import com.corundumstudio.socketio.AuthorizationResult;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class JwtUtil {

    private final String SECRET = "mysecretkeymysecretkeymysecretkey"; // 32+ chars

    private final Key key = Keys.hmacShaKeyFor(SECRET.getBytes());


    public String generateToken(String username,
                                String fullName,
                                String role,
                                String deviceType,
                                Long id) {

        return Jwts.builder()
                .setSubject(username)
                .claim("fullName", fullName)   // ✅ NEW ADDED
                .claim("role", role)
                .claim("deviceType", deviceType)
                .claim("id", id)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60))
                .signWith(key)
                .compact();
    }

    public String extractDeviceType(String token) {
        return getClaims(token).get("deviceType", String.class);
    }

    public String extractUsername(String token) {
        return getClaims(token).getSubject();
    }


    public boolean validateToken(String token, String username) {

        return (username.equals(extractUsername(token)) && !isTokenExpired(token));
    }

    private boolean isTokenExpired(String token) {
        return getClaims(token).getExpiration().before(new Date());
    }

    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    @Component
    public class UserSessionStore {

        private final Map<String, Set<String>> sessions = new ConcurrentHashMap<>();

        public void addSession(String username, String token) {
            sessions.computeIfAbsent(username, k -> new HashSet<>()).add(token);
        }

        public void removeSessionByToken(String username, String token) {
            Set<String> userTokens = sessions.get(username);
            if (userTokens != null) {
                userTokens.remove(token);
            }
        }

        public Set<String> getSessions(String username) {
            return sessions.get(username);
        }
    }

}