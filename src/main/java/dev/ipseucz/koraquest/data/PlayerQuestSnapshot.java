package dev.ipseucz.koraquest.data;

import java.util.Map;
import java.util.Set;

public record PlayerQuestSnapshot(Set<String> active, Set<String> completed, Map<String, Integer> progress) {
}
