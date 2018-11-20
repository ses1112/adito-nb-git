package de.adito.git.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Queue-backed list of colors that delivers colors/takes them back. Operates on FIFO principle
 *
 * @author m.kaspera 20.11.2018
 */
class ColorRoulette {

    private static final List<Color> initialColors = Arrays.asList(
            new Color(0, 255, 0),
            new Color(0, 255, 255),
            new Color(255, 255, 0),
            new Color(200, 200, 200),
            new Color(255, 0, 0),
            new Color(0, 0, 255),
            new Color(200, 100, 0),
            new Color(150, 200, 200),
            new Color(200, 200, 0),
            new Color(0, 200, 200),
            new Color(200, 100, 100),
            new Color(0, 100, 200),
            new Color(100, 255, 255)
    );
    private static final BlockingQueue<Color> availableColors = new ArrayBlockingQueue<>(initialColors.size());

    /**
     * @return the next Color in the queue or null if an error occurs/the queue is empty
     */
    @Nullable
    static Color get() {
        if (availableColors.isEmpty())
            availableColors.addAll(initialColors);
        try {
            return availableColors.take();
        } catch (InterruptedException e) {
            return null;
        }
    }

    /**
     * @param color the Color that should be returned to the queue
     */
    static void returnColor(@NotNull Color color) {
        availableColors.offer(color);
    }

}
