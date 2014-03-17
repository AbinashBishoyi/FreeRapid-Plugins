package cz.vity.freerapid.plugins.services.youtube;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author Kajda, JPEXS, ntoskrnl
 */
public class YouTubeSettingsPanel extends JPanel {
    private YouTubeSettingsConfig config;

    public YouTubeSettingsPanel(YouTubeServiceImpl service) throws Exception {
        super();
        config = service.getConfig();
        initPanel();
    }

    private void initPanel() {
        final String[] qualityStrings = {"0 (lowest)", "1", "2", "3", "maximum available"};
        final JLabel qualityLabel = new JLabel("Preferred quality level:");
        final JComboBox qualityList = new JComboBox(qualityStrings);
        final JCheckBox orderCheckBox = new JCheckBox("Sort by newest first when adding links from user pages");
        qualityLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        qualityList.setAlignmentX(Component.LEFT_ALIGNMENT);
        orderCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        qualityList.setSelectedIndex(config.getQualitySetting());
        orderCheckBox.setSelected(config.isReversePlaylistOrder());
        qualityList.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                config.setQualitySetting(qualityList.getSelectedIndex());
            }
        });
        orderCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                config.setReversePlaylistOrder(orderCheckBox.isSelected());
            }
        });
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(qualityLabel);
        add(qualityList);
        add(Box.createRigidArea(new Dimension(0, 15)));
        add(orderCheckBox);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
    }

}