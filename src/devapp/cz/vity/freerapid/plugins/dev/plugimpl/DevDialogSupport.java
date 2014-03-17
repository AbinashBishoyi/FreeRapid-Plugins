package cz.vity.freerapid.plugins.dev.plugimpl;

import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.interfaces.DialogSupport;
import org.jdesktop.application.ApplicationContext;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * @author Ladislav Vitasek
 */
public class DevDialogSupport implements DialogSupport {
    private final static Object captchaLock = new Object();

    /**
     * result from the user's input for password
     */
    private volatile String passwordResult;

    public DevDialogSupport(final ApplicationContext context) {
    }

    @Override
    public PremiumAccount showAccountDialog(final PremiumAccount account, final String title) throws Exception {
        return account;
    }

    @Override
    public boolean showOKCancelDialog(final Component container, final String title) throws Exception {
        final boolean[] dialogResult = {false};
        if (!EventQueue.isDispatchThread()) {
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    dialogResult[0] = showInputDialog(title, container, true) == 0;
                }
            });
        } else return showInputDialog(title, container, true) == 0;
        return dialogResult[0];

    }

    @Override
    public void showOKDialog(final Component container, final String title) throws Exception {
        if (!EventQueue.isDispatchThread()) {
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    showInputDialog(title, container, false);
                }
            });
        } else showInputDialog(title, container, false);
    }

    @Override
    public String askForCaptcha(final BufferedImage image) throws Exception {
        synchronized (captchaLock) {
            final String[] captchaResult = {""};
            if (!EventQueue.isDispatchThread()) {
                SwingUtilities.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        captchaResult[0] = getCaptcha(image);
                    }
                });
            } else captchaResult[0] = getCaptcha(image);
            return captchaResult[0];
        }
    }

    private String getCaptcha(BufferedImage image) {
        return (String) JOptionPane.showInputDialog(null, "Insert what you see", "Insert CAPTCHA", JOptionPane.PLAIN_MESSAGE, new ImageIcon(image), null, null);
    }

    private static int showInputDialog(final String title, final Object inputObject, boolean cancelButton) {
        final String[] buttons;
        if (cancelButton)
            buttons = new String[]{"OK", "Cancel"};
        else
            buttons = new String[]{"Cancel"};
        final Object[] objects = new Object[buttons.length];
        for (int i = 0; i < buttons.length; i++) {
            final String s = buttons[i];
            assert s != null;
            objects[i] = s;
        }
        return JOptionPane.showOptionDialog(null, inputObject, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, objects, objects[0]);
    }

    @Override
    public String askForPassword(final String name) throws Exception {
        synchronized (captchaLock) {
            passwordResult = "";
            if (!EventQueue.isDispatchThread()) {
                SwingUtilities.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        askPassword(name);
                    }
                });
            } else askPassword(name);
            return passwordResult;
        }
    }

    private void askPassword(final String name) {
        passwordResult = (String) JOptionPane.showInputDialog(null, "Password protected", "Insert Password", JOptionPane.PLAIN_MESSAGE, null, null, null);
    }

}