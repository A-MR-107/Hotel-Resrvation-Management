package com.hotel.ui;

import com.hotel.model.MenuItem;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.JPanel;

/** Menu as tappable tiles, split into Food and Drinks tabs. No scroll bar needed. */
final class MenuPicker extends JPanel {

    private final Consumer<MenuItem> onSelect;
    private final PillButton foodTab = new PillButton("Food", PillButton.Style.SECONDARY);
    private final PillButton drinkTab = new PillButton("Drinks", PillButton.Style.SECONDARY);
    private final JPanel grid = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
    private List<MenuItem> items = List.of();
    private boolean drinks;
    private MenuItem selected;

    MenuPicker(Consumer<MenuItem> onSelect) {
        this.onSelect = onSelect;
        setLayout(new BorderLayout(0, 12));
        setOpaque(false);

        JPanel tabs = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        tabs.setOpaque(false);
        tabs.add(foodTab);
        tabs.add(drinkTab);
        foodTab.addActionListener(e -> showTab(false));
        drinkTab.addActionListener(e -> showTab(true));
        add(tabs, BorderLayout.NORTH);

        grid.setOpaque(false);
        add(grid, BorderLayout.CENTER);
    }

    void setItems(List<MenuItem> all) {
        items = all;
        long food = all.stream().filter(i -> !i.drink()).count();
        long drink = all.size() - food;
        foodTab.setText("Food (" + food + ")");
        drinkTab.setText("Drinks (" + drink + ")");
        showTab(drinks);
    }

    private void showTab(boolean showDrinks) {
        drinks = showDrinks;
        foodTab.setActive(!showDrinks);
        drinkTab.setActive(showDrinks);
        grid.removeAll();
        for (MenuItem item : items) {
            if (item.drink() != showDrinks) {
                continue;
            }
            Chip chip = new Chip(item.name(), UiSupport.money(item.price()),
                    UiSupport.FREE_BG, UiSupport.FREE_BORDER, 178, 66);
            chip.setChosen(selected != null && selected.id() == item.id());
            chip.addActionListener(e -> choose(item));
            grid.add(chip);
        }
        grid.revalidate();
        grid.repaint();
    }

    private void choose(MenuItem item) {
        selected = item;
        showTab(drinks);
        onSelect.accept(item);
    }
}
