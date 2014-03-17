package cz.vity.freerapid.plugins.services.microsoftdownloads;

import javax.swing.*;
import java.awt.*;

/**
 * @author ntoskrnl
 */
class MicrosoftDownloadsWGAConfirmUI extends JPanel {

    public MicrosoftDownloadsWGAConfirmUI() {
        initComponents();
    }

    private void initComponents() {
        JLabel label = new JLabel("<html>This download requires WGA validation.<br>Validation involves downloading a ~1.5MB tool from Microsoft.<br>Please click OK to continue with validation.</html>", getWGAIcon(), JLabel.LEFT);

        setLayout(new BorderLayout());

        add(label, BorderLayout.CENTER);
    }

    private Icon getWGAIcon() {
        final java.net.URL url = getClass().getResource("/resources/wga.png");
        return url == null ? new ImageIcon() : new ImageIcon(url);
    }

}
