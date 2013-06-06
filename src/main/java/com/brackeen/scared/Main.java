package com.brackeen.scared;

import com.brackeen.app.App;
import com.brackeen.app.view.Scene;
import java.awt.Color;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import javax.swing.JFrame;

public class Main extends App {
    
    private static final long serialVersionUID = 1L;
    
    public static void main(String[] args) {
        final Main main = new Main();
        
        // Create frame and add Applet to it
        final JFrame frame = new JFrame("Scared");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setBackground(Color.BLACK);
        frame.setSize(640, 480);
        main.setSize(640, 480);
        frame.getContentPane().setLayout(null);
        frame.getContentPane().add(main);
        
        // Start
        frame.setVisible(true);
        main.init();
        main.start();
        
        // Center applet on resize
        frame.getContentPane().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                main.setLocation((frame.getWidth() - main.getWidth()) / 2, (frame.getHeight() - main.getHeight()) / 2);
            }
        });
    }
    
    @Override
    public Scene createFirstScene() {
        return new TitleScene();
    }
}
