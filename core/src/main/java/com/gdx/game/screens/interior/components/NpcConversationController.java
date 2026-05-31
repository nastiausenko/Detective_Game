package com.gdx.game.screens.interior.components;

import com.badlogic.gdx.Gdx;
import com.gdx.game.model.DialogueHistory;
import com.gdx.game.app.model.GameContext;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class NpcConversationController {
    public interface Listener {
        void onAnswer(String question, String answer);
        void onFactsDiscovered(int count);
    }

    private static final int ANSWER_TIMEOUT_SECONDS = 40;
    private static final ScheduledExecutorService timeoutExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "npc-answer-timeout");
        thread.setDaemon(true);
        return thread;
    });

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
        AtomicBoolean completed = new AtomicBoolean(false);

        timeoutExecutor.schedule(() -> {
            if (!completed.compareAndSet(false, true)) return;

            Gdx.app.postRunnable(() -> {
                if (!active) return;
                listener.onAnswer(q, "Мені потрібна мить. Повторіть питання, будь ласка.");
            });
        }, ANSWER_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        game.getNpcDialogueService().askNpcAsync(characterId, q, buildingId, (answer, error) -> {
            if (!completed.compareAndSet(false, true)) return;

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
            count -> {
                if (count <= 0) return;
                Gdx.app.postRunnable(() -> {
                    if (active) {
                        listener.onFactsDiscovered(count);
                    }
                });
            },
            (newlyRevealed, error) -> {
                if (error != null) {
                    error.printStackTrace();
                }
            });
    }
}
