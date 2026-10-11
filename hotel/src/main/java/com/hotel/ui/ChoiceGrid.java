package com.hotel.ui;

import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.swing.JPanel;

/**
 * A grid of clickable option cards that replaces a drop-down list.
 * The user clicks a card to choose it; listeners are told when the choice changes.
 */
final class ChoiceGrid<T> extends JPanel {

    private final List<T> items = new ArrayList<>();
    private final List<ChoiceCard> cards = new ArrayList<>();
    private final List<Runnable> listeners = new ArrayList<>();
    private T selected;

    ChoiceGrid(int columns) {
        setOpaque(false);
        setLayout(new GridLayout(0, columns, 12, 12));
    }

    /** Replaces the options. The first item matching preferred is selected, if any. */
    void setItems(List<T> newItems, Function<T, String> title, Function<T, String> subtitle, Predicate<T> preferred) {
        items.clear();
        items.addAll(newItems);
        cards.clear();
        removeAll();
        for (T item : items) {
            ChoiceCard card = new ChoiceCard(title.apply(item), subtitle.apply(item));
            card.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    select(item);
                }
            });
            cards.add(card);
            add(card);
        }
        selected = null;
        for (T item : items) {
            if (preferred.test(item)) {
                selected = item;
                break;
            }
        }
        updateCards();
        revalidate();
        repaint();
        fireChanged();
    }

    T getSelected() {
        return selected;
    }

    void onChange(Runnable listener) {
        listeners.add(listener);
    }

    void select(T item) {
        selected = item;
        updateCards();
        fireChanged();
    }

    private void updateCards() {
        for (int i = 0; i < cards.size(); i++) {
            cards.get(i).setSelected(selected != null && items.get(i) == selected);
        }
    }

    private void fireChanged() {
        for (Runnable r : listeners) {
            r.run();
        }
    }
}
