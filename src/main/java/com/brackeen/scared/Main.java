package com.brackeen.scared;

import com.brackeen.app.App;
import com.brackeen.app.view.Scene;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

public class Main extends App {
    
    private static final long serialVersionUID = 1L;
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater( new Runnable() {
            public void run() {
                final Main main = new Main();

                // Create frame
                JFrame frame = new JFrame("Scared");
                final Container contentPane = frame.getContentPane();
                frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                contentPane.setBackground(Color.BLACK);

                // Add applet to frame
                main.setSize(640, 480);
                contentPane.setPreferredSize(new Dimension(640, 480));
                contentPane.setLayout(null);
                contentPane.add(main);

                // Show frame
                frame.pack();
                Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
                frame.setLocation((dim.width - frame.getWidth())/2, (dim.height - frame.getHeight())/2);        
                frame.setVisible(true);
                
                // Start
                main.init();
                main.start();

                // Center applet on resize
                contentPane.addComponentListener(new ComponentAdapter() {
                    @Override
                    public void componentResized(ComponentEvent e) {
                        main.setLocation((contentPane.getWidth() - main.getWidth()) / 2, (contentPane.getHeight() - main.getHeight()) / 2);
                    }
                });
            }
        });
    }
    
    @Override
    public Scene createFirstScene() {
        return new TitleScene();
    }
}
