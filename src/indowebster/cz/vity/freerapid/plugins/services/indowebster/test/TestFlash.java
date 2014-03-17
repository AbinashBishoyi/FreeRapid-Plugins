package cz.vity.freerapid.plugins.services.indowebster.test;

import com.jpackages.jflashplayer.*;

import javax.swing.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.awt.*;
import java.awt.event.WindowListener;
import java.awt.event.ComponentListener;

/**
 * @author Vity
 */
public class TestFlash implements FlashCallListener, FlashPanelListener {
    private FlashPanel panel;

    public static void main(String[] args) throws JFlashInvalidFlashException, FileNotFoundException, JFlashLibraryLoadFailedException {
        new TestFlash().start();
    }

    private void start() throws JFlashLibraryLoadFailedException, JFlashInvalidFlashException, FileNotFoundException {
        final JFrame jFrame = new JFrame("asdasd");
        jFrame.setSize(300,200);
        jFrame.setLocationRelativeTo(null);
        jFrame.getContentPane().setLayout(new BorderLayout());

        panel = new FlashPanel(new File("d:\\object.swf"));
        panel.setVisible(false);
        panel.setFlashCallListener(this);
        panel.setLoop(false);
        panel.setMenuDisplay(false);
        panel.setQualityLow();        
        panel.addFlashPanelListener(this);
        jFrame.getContentPane().add(panel, BorderLayout.CENTER);
        final ComponentListener[] listeners = jFrame.getComponentListeners();
        for (ComponentListener listener : listeners) {
            System.out.println("listener = " + listener);
        }
        panel.play();
        jFrame.setVisible(true);
    }

    public String call(String s) {
        System.out.println("s = " + s);
        if (s.contains("AESDecryptCtr"))
            panel.stop();
        return s;
    }

    public void FSCommand(String s, String s1) {
        System.out.println("s = " + s);
        System.out.println("s1 = " + s1);
    }
}
