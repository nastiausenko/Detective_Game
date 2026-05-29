package com.gdx.game.ui.component.interior;

import com.badlogic.gdx.Gdx;
import com.gdx.game.domain.investigation.DialogueHistory;
import com.gdx.game.infrastructure.GameContext;

public class NpcConversationController {
    public interface Listener {
        void onAnswer(String question, String answer);
        void onFactsDiscovered(int count);
    }

    private final GameContext game;
    private final String characterId;
    private final String buildingId;
    private boolean active = true;

    public NpcConversationController(GameContext game, String characterId, String buildingId) {
        this.game = game;
        this.characterId = characterId;
        this.buildingId = buildingId;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void ask(String question, Listener listener) {
        if (question == null || question.trim().isEmpty()) return;

        final String q = question.trim();
        game.getNpcDialogueService().askNpcAsync(characterId, q, buildingId, (answer, error) -> {
            String response = answer != null ? answer : "";
            if (error != null) {
                error.printStackTrace();
                response = "Вибач, але я зараз не можу відповісти.";
            }

            final String finalAnswer = response;
            Gdx.app.postRunnable(() -> {
                if (!active) return;

                DialogueHistory.append(characterId, q, finalAnswer);
                listener.onAnswer(q, finalAnswer);
            });

            if (error == null) {
                revealFacts(q, finalAnswer, listener);
            }
        });
    }

    private void revealFacts(String question, String answer, Listener listener) {
        game.getFactRevealService().revealFactsAfterExchangeAsync(characterId, question, answer,
            (newlyRevealed, error) -> {
                if (error != null) {
                    error.printStackTrace();
                    return;
                }

                if (newlyRevealed != null && newlyRevealed > 0) {
                    Gdx.app.postRunnable(() -> {
                        if (active) {
                            listener.onFactsDiscovered(newlyRevealed);
                        }
                    });
                }
            });
    }
}
