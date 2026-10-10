package com.hotel.ui;

/** A main-window page that can reload its data and optionally preselect a room. */
interface Screen {
    void refresh();

    default void focusRoom(int roomNumber) {
    }
}
