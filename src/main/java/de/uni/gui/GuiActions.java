package de.uni.gui;

import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;

public interface GuiActions {

    void mouseOver(MouseEvent event);

    void mouseClicked(MouseEvent event);

    void keyPressed(KeyEvent event);

    void keyReleased(KeyEvent event);

}
