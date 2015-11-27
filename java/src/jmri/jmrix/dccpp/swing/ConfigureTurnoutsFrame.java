package jmri.jmrix.dccpp.swing;

/*
 * <hr>
 * This file is part of JMRI.
 * <P>
 * JMRI is free software; you can redistribute it and/or modify it under 
 * the terms of version 2 of the GNU General Public License as published 
 * by the Free Software Foundation. See the "COPYING" file for a copy
 * of this license.
 * <P>
 * JMRI is distributed in the hope that it will be useful, but WITHOUT 
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or 
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License 
 * for more details.
 * <P>
 *
 * @author			Mark Underwood Copyright (C) 2011
 * @version			$Revision: 21510 $
 */
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Vector;
import java.util.Iterator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.DefaultCellEditor;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JCheckBox;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.event.EventListenerList;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import jmri.jmrix.dccpp.DCCppTurnout;
import jmri.jmrix.dccpp.DCCppTurnoutManager;
import jmri.jmrix.dccpp.DCCppTrafficController;
import jmri.jmrix.dccpp.DCCppListener;
import jmri.jmrix.dccpp.DCCppMessage;
import jmri.jmrix.dccpp.DCCppReply;
import jmri.util.JmriJFrame;
import jmri.util.WindowMenu;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigureTurnoutsFrame extends JmriJFrame implements DCCppListener {

    // Uncomment this when we add labels...
    public static enum PropertyChangeID {

        MUTE, VOLUME_CHANGE, ADD_DECODER, REMOVE_DECODER
    }

    public static final Map<PropertyChangeID, String> PCIDMap;

    static {
        Map<PropertyChangeID, String> aMap = new HashMap<PropertyChangeID, String>();
        aMap.put(PropertyChangeID.MUTE, "VSDMF:Mute"); // NOI18N
        aMap.put(PropertyChangeID.VOLUME_CHANGE, "VSDMF:VolumeChange"); // NOI18N
        aMap.put(PropertyChangeID.ADD_DECODER, "VSDMF:AddDecoder"); // NOI18N
        aMap.put(PropertyChangeID.REMOVE_DECODER, "VSDMF:RemoveDecoder"); // NOI18N
        PCIDMap = Collections.unmodifiableMap(aMap);
    }

    // Map of Mnemonic KeyEvent values to GUI Components
    private static final Map<String, Integer> Mnemonics = new HashMap<String, Integer>();

    static {
        Mnemonics.put("TurnoutTab", KeyEvent.VK_E); // NOI18N
        Mnemonics.put("AddButton", KeyEvent.VK_A); // NOI18N
        Mnemonics.put("CloseButton", KeyEvent.VK_O); // NOI18N
        Mnemonics.put("SaveButton", KeyEvent.VK_S); // NOI18N
    }

    protected EventListenerList listenerList = new javax.swing.event.EventListenerList();

    private DCCppTurnoutManager turnoutManager;
    private DCCppTrafficController tc;
    
    private JTabbedPane tabbedPane;
    private JPanel turnoutPanel;

    private Object[][] turnoutData;    // positions of Reporters
    private TurnoutTableModel turnoutModel;
    private JTable turnoutTable;

    private List<JMenu> menuList;

    @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "EI_EXPOSE_REP2",
            justification = "2D array of different types passed as complex parameter. "
            + "Better to switch to passing use-specific objects rather than "
            + "papering this over with a deep copy of the arguments. "
            + "In any case, there's no risk of exposure here.")
    public ConfigureTurnoutsFrame(DCCppTurnoutManager sm, DCCppTrafficController t) {
        super(false, false);
        turnoutManager = sm;
        tc = t;
        initGui();
    }

    private void initGui() {

        // NOTE: Look at jmri.jmrit.vsdecoder.swing.ManageLocationsFrame
        // for how to add a tab for turnouts and other things.
        
        this.setTitle(Bundle.getMessage("FieldManageTurnoutsFrameTitle"));
        this.buildMenu();
        
        // Panel for managing sensors
        turnoutPanel = new JPanel();
        turnoutPanel.setLayout(new GridBagLayout());
        JScrollPane turnoutScrollPanel = new JScrollPane();
        turnoutModel = new TurnoutTableModel();
        turnoutTable = new JTable(turnoutModel);
        turnoutTable.setFillsViewportHeight(true);
        turnoutScrollPanel.getViewport().add(turnoutTable);
        turnoutTable.setPreferredScrollableViewportSize(new Dimension(520, 200));
        turnoutTable.getColumn(Bundle.getMessage("FieldTableDeleteColumn")).setCellRenderer(new ButtonRenderer());
        turnoutTable.getColumn(Bundle.getMessage("FieldTableDeleteColumn")).setCellEditor(new ButtonEditor(new JCheckBox(), turnoutTable));

        tabbedPane = new JTabbedPane();
        tabbedPane.addTab(Bundle.getMessage("FieldTurnoutsTabTitle"), turnoutScrollPanel);
        tabbedPane.setToolTipTextAt(0, Bundle.getMessage("ToolTipTurnoutTab"));
        tabbedPane.setMnemonicAt(0, Mnemonics.get("TurnoutTab")); // NOI18N

        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        
        JButton addButton = new JButton(Bundle.getMessage("ButtonAdd"));
        addButton.setToolTipText(Bundle.getMessage("ToolTipButtonMTFAdd"));
        addButton.setMnemonic(Mnemonics.get("AddButton")); // NOI18N
        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                addButtonPressed(e);
            }
        });
        JButton closeButton = new JButton(Bundle.getMessage("ButtonClose"));
        closeButton.setToolTipText(Bundle.getMessage("ToolTipButtonMTFClose"));
        closeButton.setMnemonic(Mnemonics.get("CloseButton")); // NOI18N
        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                closeButtonPressed(e);
            }
        });
        JButton saveButton = new JButton(Bundle.getMessage("ButtonSave"));
        saveButton.setToolTipText(Bundle.getMessage("ToolTipButtonMTFSave"));
        saveButton.setMnemonic(Mnemonics.get("SaveButton")); // NOI18N
        saveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                saveButtonPressed(e);
            }
        });
        buttonPane.add(addButton);
        buttonPane.add(closeButton);
        buttonPane.add(saveButton);

        this.getContentPane().setLayout(new BoxLayout(this.getContentPane(), BoxLayout.Y_AXIS));
        this.getContentPane().add(tabbedPane);
        this.getContentPane().add(buttonPane);
        this.pack();
        this.setVisible(true);
    }

    private void buildMenu() {
        JMenu fileMenu = new JMenu(Bundle.getMessage("MenuFile"));

        //fileMenu.add(new LoadVSDFileAction(Bundle.getMessage("MenuItemLoadVSDFile")));
        //fileMenu.add(new StoreXmlVSDecoderAction(Bundle.getMessage("MenuItemSaveProfile")));
        //fileMenu.add(new LoadXmlVSDecoderAction(Bundle.getMessage("MenuItemLoadProfile")));

        JMenu editMenu = new JMenu(Bundle.getMessage("MenuEdit"));
        //editMenu.add(new VSDPreferencesAction(Bundle.getMessage("MenuItemEditPreferences")));

        //fileMenu.getItem(1).setEnabled(false); // disable XML store
        //fileMenu.getItem(2).setEnabled(false); // disable XML load

        menuList = new ArrayList<JMenu>(3);

        menuList.add(fileMenu);
        menuList.add(editMenu);

        this.setJMenuBar(new JMenuBar());
        this.getJMenuBar().add(fileMenu);
        this.getJMenuBar().add(editMenu);
        //this.addHelpMenu("package.jmri.jmrit.vsdecoder.swing.ManageLocationsFrame", true); // NOI18N

    }
    
    // DCCppListener Methods
    public void message(DCCppReply r) {
        // When we get a SensorDefReply message, add the
        // sensor information to the data map for the model.
        // TODO: FIX THIS
        if (r.isTurnoutReply()) {
            Vector v = new Vector();
            v.add(r.getSensorDefNumInt());
            v.add(r.getSensorDefPinInt());
            v.add(r.getSensorDefPullupBool());
            //v.add("Delete");
            turnoutModel.insertData(v, false);
        }
    }
    
    public void message(DCCppMessage m) {
        // Do nothing
    }
    
    public void notifyTimeout(DCCppMessage m) {
        // Do nothing
    }

    /**
     * Add a standard help menu, including window specific help item.
     *
     * @param ref    JHelp reference for the desired window-specific help page
     * @param direct true if the help menu goes directly to the help system,
     *               e.g. there are no items in the help menu
     *
     * WARNING: BORROWED FROM JmriJFrame.
     */
    public void addHelpMenu(String ref, boolean direct) {
        // only works if no menu present?
        JMenuBar bar = getJMenuBar();
        if (bar == null) {
            bar = new JMenuBar();
        }
        // add Window menu
        bar.add(new WindowMenu(this)); // * GT 28-AUG-2008 Added window menu
        // add Help menu
        jmri.util.HelpUtil.helpMenu(bar, ref, direct);
        setJMenuBar(bar);
    }

    private void addButtonPressed(ActionEvent e) {
        Vector v = new Vector();
        v.add(0);
        v.add(0);
        v.add(false);
        v.add("Delete");
        turnoutModel.insertData(v, true);
    }
    
    private void saveButtonPressed(ActionEvent e) {
        int value = JOptionPane.showConfirmDialog(null, Bundle.getMessage("FieldMTFSaveDialogConfirmMessage"),
                Bundle.getMessage("FieldMTFSaveDialogTitle"),
                JOptionPane.YES_NO_OPTION);
        if (turnoutTable.getCellEditor() != null) {
            turnoutTable.getCellEditor().stopCellEditing();
        }
        if (value == JOptionPane.YES_OPTION) {
            saveTableValues();
            //OperationsXml.save();
        }
        //if (Setup.isCloseWindowOnSaveEnabled()) {
        if (true) {
            dispose();
        }
    }

    @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "WMI_WRONG_MAP_ITERATOR", justification = "only in slow debug")
    private void saveTableValues() {
        // TODO: FIX THIS FOR TURNOUTS
        Iterator it = turnoutModel.getRowData().iterator();
        while(it.hasNext()) {
            Vector r = (Vector)it.next();
            int row = turnoutModel.getRowData().indexOf(r);
            if (turnoutModel.isNewRow(row)) {
                // WARNING: Conversions here are brittle. Be careful.
                String m = "S " + Integer.toString((int)r.elementAt(0)); // Index
                m += " " + (Integer.toString((int)r.elementAt(1)));      // Pin
                m += " " + ((boolean)r.elementAt(2) ? "1" : "0");        // Pullup
                tc.sendDCCppMessage(new DCCppMessage(m), this);
                log.debug("Sending: " + m);
                turnoutModel.setNewRow(row, false);
            } else if (turnoutModel.isMarkedForDelete(row)) {
                String m = "S " + Integer.toString((int)r.elementAt(0));
                tc.sendDCCppMessage(new DCCppMessage(m), this);
                log.debug("Sending: " + m);
                turnoutModel.getRowData().remove(r);
            } else if (turnoutModel.isDirtyRow(row)) {
                // Send a Delete, then an Add (for now).
                String m = "S " + Integer.toString((int)r.elementAt(0));
                tc.sendDCCppMessage(new DCCppMessage(m), this);
                log.debug("Sending: " + m);
                // WARNING: Conversions here are brittle. Be careful.
                m = "S " + Integer.toString((int)r.elementAt(0)); // Index
                m += " " + (Integer.toString((int)r.elementAt(1)));      // Pin
                m += " " + ((boolean)r.elementAt(2) ? "1" : "0");        // Pullup
                tc.sendDCCppMessage(new DCCppMessage(m), this);
                log.debug("Sending: " + m);
                turnoutModel.setNewRow(row, false);
                turnoutModel.setDirtyRow(row, false);
            }
        }
        tc.sendDCCppMessage(new DCCppMessage("E"), this);
        log.debug("Sending: <E> (Write To EEPROM)");
        turnoutModel.fireTableDataChanged();
    }

    private void modeRadioButtonPressed(ActionEvent e) {
    }

    private void closeButtonPressed(ActionEvent e) {
        dispose();
    }

    static private Logger log = LoggerFactory.getLogger(ConfigureTurnoutsFrame.class.getName());

    /**
     * Private class to serve as TableModel for Reporters and Ops Locations
     */
    private static class TurnoutTableModel extends AbstractTableModel {

        // These get internationalized at runtime in the constructor below.
        private String[] columnNames = new String[4];
        private Vector rowData = new Vector();
        private Vector isNew = new Vector();
        private Vector isDirty = new Vector();
        private Vector markDelete = new Vector();

        public TurnoutTableModel() {
            super();
            // Use i18n-ized column titles.
            //columnNames[0] = Bundle.getMessage("FieldTableNameColumn");
            columnNames[0] = Bundle.getMessage("FieldTableIndexColumn");
            columnNames[1] = Bundle.getMessage("FieldTablePinColumn");
            columnNames[2] = Bundle.getMessage("FieldTablePullupColumn");
            columnNames[3] = Bundle.getMessage("FieldTableDeleteColumn");
            rowData = new Vector();
        }

        // Note: May be obsoleted by insertData(Vector v)
        public void insertData(Object[] values, boolean isnew) {
            Vector v = new Vector();
            for (int i = 0; i < values.length; i++) {
                v.add(values[i]);
            }
            v.add("Delete"); // TODO: Fix this
            insertData(v, isnew);
        }
        
        public boolean isNewRow(int row) {
            return((boolean) isNew.elementAt(row));
        }
        
        public void setNewRow(int row, boolean n) {
            isNew.setElementAt(n, row);
        }
        
        public boolean isDirtyRow(int row) {
            return((boolean)isDirty.elementAt(row));
        }
        
        public void setDirtyRow(int row, boolean d) {
            isDirty.setElementAt(d, row);
        }
        
        public boolean isMarkedForDelete(int row) {
            return((boolean)markDelete.elementAt(row));            
        }
        
        public void markForDelete(int row, boolean mark) {
            markDelete.setElementAt(mark, row);
        }

        public boolean contains(Vector v) {
            Iterator it = rowData.iterator();
            while(it.hasNext()) {
                Vector r = (Vector)it.next();
                if (r.firstElement() == v.firstElement()) {
                    return(true);
                }
            }
            return(false);
        }
        
        public void insertData(Vector v, boolean isnew) {
            if (!rowData.contains(v)) {
                v.add("Delete");
                rowData.add(v);
                isNew.add(isnew);
                isDirty.add(false);
                markDelete.add(false);
            }
            fireTableDataChanged();
        }
        // Probably going to get obsoleted... used in "Save Data Values"
        // which is going to work very differently, soon.
//        public HashMap<String, DCCppSensor> getDataMap() {
//            //TODO: What should this be?
//            // Includes only the ones with the checkbox made
//            HashMap<String, DCCppSensor> retv = new HashMap<String, DCCppSensor>();
//            for (Object[] row : rowData) {
//                if ((Boolean) row[1]) {
//                    retv.put((String) row[0],
//                            new DCCppSensor();
//                }
//            }
//            return (retv);
//        }
        public Vector getRowData() {
            return(rowData);
        }
        
        public String getColumnName(int col) {
            return columnNames[col].toString();
        }

        public int getRowCount() {
            return rowData.size();
        }

        public int getColumnCount() {
            return columnNames.length;
        }

        public Object getValueAt(int row, int col) {
            return(((Vector)rowData.elementAt(row)).elementAt(col));
        }

        public boolean isCellEditable(int row, int col) {
            return true;
        }

        public void setValueAt(Object value, int row, int col) {
            ((Vector)((Vector)rowData.elementAt(row))).setElementAt(value, col);
            setDirtyRow(row, true);
            fireTableCellUpdated(row, col);
        }

        public Class<?> getColumnClass(int columnIndex) {
            switch (columnIndex) {
                case 0:
                case 1:
                    return Integer.class;
                case 2:
                    return Boolean.class;
                default:
                    return super.getColumnClass(columnIndex);
            }
        }
    }
    
    class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
        }
        
        public Component getTableCellRendererComponent(JTable table, Object value,
                        boolean isSelected, boolean hasFocus, int row, int column) {
            if (isSelected) {
                setForeground(table.getSelectionForeground());
                setBackground(table.getSelectionBackground());
            } else {
                setForeground(table.getForeground());
                setBackground(UIManager.getColor("Button.background"));
            }
            setText((value == null) ? "" : value.toString());
            return this;
        }
    }
    
    class ButtonEditor extends DefaultCellEditor {
        protected JButton button;
        private String label;
        private boolean isPushed;
        private int index;
        private JTable table;
        
        public ButtonEditor(JCheckBox checkBox, JTable t) {
            super(checkBox);
            button = new JButton();
            button.setOpaque(true);
            table = t;
            button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    fireEditingStopped();
                }
            });
        }
        
        public Component getTableCellEditorComponent(JTable table, Object value,
                    boolean isSelected, int row, int column) {
            if (isSelected) {
                button.setForeground(table.getSelectionForeground());
                button.setBackground(table.getSelectionBackground());
            } else {
                button.setForeground(table.getForeground());
                button.setBackground(table.getBackground());
            }
            label = (value == null) ? "" : value.toString();
            button.setText(label);
            isPushed = true;
            return button;
        }
        
        public Object getCellEditorValue() {
            if (isPushed) {
                int sel = table.getEditingRow();
                TurnoutTableModel model = (TurnoutTableModel) table.getModel();
                int idx = (int)model.getValueAt(sel,0);
                if (model.isMarkedForDelete(sel)) {
                    model.markForDelete(sel, false);
                    log.debug("UnDelete sensor {}", idx);
                    JOptionPane.showMessageDialog(button, "Sensor " + Integer.toString(idx) +
                                                " Not Marked for Deletion");
                } else {
                    model.markForDelete(sel, true);
                    log.debug("Delete sensor {}", idx);
                    JOptionPane.showMessageDialog(button, "Sensor " + Integer.toString(idx) +
                                                " Marked for Deletion");
                }
            }
            isPushed = false;
            return new String(label);
        }
        
        public boolean stopCellEditing() {
            isPushed = false;
            return super.stopCellEditing();
        }
        
        protected void fireEditingStopped() {
            super.fireEditingStopped();
        }
    }

}
