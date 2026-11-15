package com.example.workspace.model;

import java.io.Serializable;

public record RevokedAccessTokenId(String jti, String issuer) implements Serializable {
}