/*
 * MirrorChooserUI.java
 *
 * Created on 10. prosinec 2008, 13:34
 */

package cz.vity.freerapid.plugins.services.rapidshare;

import javax.swing.*;

/**
 * @author Ludìk
 */
 class MirrorChooserUI extends javax.swing.JPanel {
        // Variables declaration - do not modify
    private javax.swing.JComboBox jComboBox1;
    private javax.swing.DefaultComboBoxModel mirrors;
    // End of variables declaration
    /**
     * Creates new form MirrorChooser
     * @param mirrors List of mirrors
     */
    public MirrorChooserUI(MirrorChooser mirrors) {
        this.mirrors = new DefaultComboBoxModel(mirrors.getArray());
         initComponents();
    }
  
     private void initComponents() {

        jComboBox1 = new javax.swing.JComboBox();

        //jComboBox1.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
          jComboBox1.setModel(mirrors);
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(67, 67, 67)
                .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, 190, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(78, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(72, 72, 72)
                .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(83, Short.MAX_VALUE))
        );
    }

   public Object getChoosen() {
       return mirrors.getSelectedItem();
    }


}
