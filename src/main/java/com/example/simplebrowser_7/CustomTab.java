package com.example.simplebrowser_7;

import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.control.Tab;
import javafx.scene.input.MouseEvent;

public class CustomTab extends Tab{

    public CustomTab() {
        super("google");
    }

    protected void close() {
        Event.fireEvent(this, new Event(Tab.CLOSED_EVENT));
    }
}
