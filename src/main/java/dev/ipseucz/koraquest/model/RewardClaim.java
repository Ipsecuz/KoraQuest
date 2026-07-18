package dev.ipseucz.koraquest.model;

import java.util.List;
import java.util.UUID;

public record RewardClaim(
        String claimId,
        UUID uuid,
        String questId,
        String cycleName,
        String cycleId,
        List<String> commands,
        String status,
        int attempts,
        int nextCommandIndex,
        long createdAt
) {
    public RewardClaim {
        cycleName = cycleName == null || cycleName.isBlank() ? cycleId == null ? "daily" : cycleId.split(":", 2)[0] : cycleName;
        commands = commands == null ? List.of() : List.copyOf(commands);
    }
}
