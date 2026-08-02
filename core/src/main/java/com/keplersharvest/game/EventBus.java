package com.keplersharvest.game;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Minimal synchronous publish/subscribe.
 *
 * <p>Handlers may publish further events; those are queued and drained after the current one so a
 * chain of reactions cannot recurse into a half-updated listener list.
 */
public final class EventBus {

    private final List<Consumer<GameEvent>> handlers = new ArrayList<>();
    private final java.util.ArrayDeque<GameEvent> pending = new java.util.ArrayDeque<>();
    private boolean dispatching;

    public void subscribe(Consumer<GameEvent> handler) {
        handlers.add(handler);
    }

    public void publish(GameEvent event) {
        pending.add(event);
        if (dispatching) {
            return;
        }
        dispatching = true;
        try {
            while (!pending.isEmpty()) {
                GameEvent next = pending.poll();
                for (Consumer<GameEvent> handler : List.copyOf(handlers)) {
                    handler.accept(next);
                }
            }
        } finally {
            dispatching = false;
        }
    }
}
