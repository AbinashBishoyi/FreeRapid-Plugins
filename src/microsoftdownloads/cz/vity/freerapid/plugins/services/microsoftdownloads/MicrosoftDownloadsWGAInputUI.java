package cz.vity.freerapid.plugins.services.microsoftdownloads;

import javax.swing.*;

/**
 * @author ntoskrnl
 */
class MicrosoftDownloadsWGAInputUI extends JPanel {
    private JTextField field;

    public MicrosoftDownloadsWGAInputUI() {
        initComponents();
    }

    private void initComponents() {
        JLabel label1 = new JLabel("<html>Please wait as the WGA tool is downloaded and launched.<br>Then paste the code provided into the box below and click OK.</html>", getWGAIcon(), JLabel.LEFT);
        JLabel label2 = new JLabel("Enter code:");
        field = new JTextField();

        GroupLayout layout = new GroupLayout(this);
        setLayout(layout);

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                        .addComponent(label1)
                        .addGroup(layout.createSequentialGroup()
                        .addComponent(label2)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(field, GroupLayout.PREFERRED_SIZE, 65, GroupLayout.PREFERRED_SIZE))
        );
        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addComponent(label1)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                        .addComponent(label2)
                        .addComponent(field, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE))
        );
    }

    public String getText() {
        return field.getText();
    }

    private Icon getWGAIcon() {
        final java.net.URL url = getClass().getResource("/resources/wga.png");
        return url == null ? new ImageIcon() : new ImageIcon(url);
    }

}
