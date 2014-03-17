package cz.vity.freerapid.plugins.services.hulu;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

/**
 * @author tong2shot
 */
public class HuluSettingsPanel extends JPanel {

    private HuluSettingsConfig config;

    public HuluSettingsPanel(HuluServiceImpl service) throws Exception {
        super();
        config = service.getConfig();
        initPanel();
    }

    private void initPanel() {
        final JLabel lblUsername = new JLabel("Email address:");
        final JTextField txtfldUsername = new JTextField(40);
        final JLabel lblPassword = new JLabel("Password:");
        final JPasswordField pswdfldPassword = new JPasswordField(40);
        final JLabel lblQuality = new JLabel("Preferred quality level:");
        final JComboBox<VideoQuality> cbbVideoQuality = new JComboBox<VideoQuality>(VideoQuality.getItems());
        final JCheckBox checkSubtitles = new JCheckBox("Download subtitles", config.isDownloadSubtitles());

        lblUsername.setAlignmentX(Component.LEFT_ALIGNMENT);
        txtfldUsername.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblPassword.setAlignmentX(Component.LEFT_ALIGNMENT);
        pswdfldPassword.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblQuality.setAlignmentX(Component.LEFT_ALIGNMENT);
        cbbVideoQuality.setAlignmentX(Component.LEFT_ALIGNMENT);
        checkSubtitles.setAlignmentX(Component.LEFT_ALIGNMENT);

        txtfldUsername.setText(config.getUsername());
        pswdfldPassword.setText(config.getPassword());
        cbbVideoQuality.setSelectedItem(config.getVideoQuality());

        txtfldUsername.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                //
            }

            @Override
            public void focusLost(FocusEvent e) {
                config.setUsername(txtfldUsername.getText().trim().isEmpty() ? null : txtfldUsername.getText());
            }
        });

        pswdfldPassword.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                //
            }

            @Override
            public void focusLost(FocusEvent e) {
                config.setPassword((pswdfldPassword.getPassword().length == 0) ? null : new String(pswdfldPassword.getPassword()));
            }
        });

        cbbVideoQuality.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                config.setVideoQuality((VideoQuality) cbbVideoQuality.getSelectedItem());
            }
        });

        checkSubtitles.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                config.setDownloadSubtitles(checkSubtitles.isSelected());
            }
        });


        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(lblUsername);
        add(txtfldUsername);
        add(lblPassword);
        add(pswdfldPassword);
        add(lblQuality);
        add(cbbVideoQuality);
        add(Box.createRigidArea(new Dimension(0, 5)));
        add(checkSubtitles);
        add(Box.createRigidArea(new Dimension(0, 15)));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
    }

}
