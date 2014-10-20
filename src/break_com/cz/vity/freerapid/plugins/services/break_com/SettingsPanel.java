package cz.vity.freerapid.plugins.services.break_com;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author tong2shot
 */

class SettingsPanel extends JPanel {
    private SettingsConfig config;

    public SettingsPanel(Break_comServiceImpl service) throws Exception {
        super();
        config = service.getConfig();
        initPanel();
    }

    private void initPanel() {
        final JLabel qualityLabel = new JLabel("Preferred video quality:");
        final JComboBox<VideoQuality> qualityList = new JComboBox<VideoQuality>(VideoQuality.getItems());
        qualityLabel.setLabelFor(qualityList);
        qualityLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        qualityList.setAlignmentX(Component.LEFT_ALIGNMENT);
        qualityList.setSelectedItem(config.getVideoQuality());

        qualityList.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                config.setVideoQuality((VideoQuality) qualityList.getSelectedItem());
            }
        });

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(qualityLabel);
        add(qualityList);
        add(Box.createRigidArea(new Dimension(0, 15)));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
    }
}
