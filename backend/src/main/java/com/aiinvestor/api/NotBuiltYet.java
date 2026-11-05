package com.aiinvestor.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** 501 for endpoints in docs/skills/api-contract.md that aren't built yet, so callers can't mistake them for working. */
final class NotBuiltYet {

    private NotBuiltYet() {
    }

    static ResponseStatusException error() {
        return error("This endpoint is not built yet");
    }

    static ResponseStatusException error(String reason) {
        return new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, reason);
    }
}
