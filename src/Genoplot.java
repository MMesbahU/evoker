import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.event.*;
import java.awt.*;
import java.io.*;
import java.util.Vector;
import java.util.StringTokenizer;
import java.util.ArrayDeque;

public class Genoplot extends JFrame implements ActionListener {

    private DataDirectory db;

    private JTextField snpField;
    private JButton goButton;
    private JPanel plotArea;

    private PrintStream output;
    private ArrayDeque<String> snpList;
    private String currentSNPinList;
    //private int snpListIndex;
    //private boolean snpListActive;
    JFileChooser jfc;

    private JButton yesButton;
    private JButton maybeButton;
    private JButton noButton;

    private JMenuItem loadList;
    private JMenu historyMenu;
    private ButtonGroup snpGroup;
    private JMenuItem returnToListPosition;

    public static void main(String[] args){

        new Genoplot();

    }

    Genoplot(){
        super("Evoke...");

        jfc = new JFileChooser("user.dir");

        JMenuBar mb = new JMenuBar();

        int menumask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

        JMenu fileMenu = new JMenu("File");
        JMenuItem openDirectory = new JMenuItem("Open directory");
        openDirectory.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, menumask));
        openDirectory.addActionListener(this);
        fileMenu.add(openDirectory);
        JMenuItem openRemote = new JMenuItem("Connect to remote server");
        openRemote.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, menumask));
        openRemote.addActionListener(this);
        fileMenu.add(openRemote);
        loadList = new JMenuItem("Load marker list");
        loadList.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, menumask));
        loadList.addActionListener(this);
        loadList.setEnabled(false);
        fileMenu.add(loadList);
        /*JMenuItem dumpImages = new JMenuItem("Dump PNGs of all SNPs in list");
        dumpImages.addActionListener(this);
        fileMenu.add(dumpImages);*/

        if (!(System.getProperty("os.name").toLowerCase().contains("mac"))){
            JMenuItem quitItem = new JMenuItem("Quit");
            quitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, menumask));
            quitItem.addActionListener(this);
            fileMenu.add(quitItem);
        }
        
        mb.add(fileMenu);


        historyMenu = new JMenu("History");
        returnToListPosition = new JMenuItem("Return to current list position");
        snpGroup = new ButtonGroup();
        returnToListPosition.setEnabled(false);
        returnToListPosition.addActionListener(this);
        historyMenu.add(returnToListPosition);
        historyMenu.addSeparator();
        mb.add(historyMenu);

        setJMenuBar(mb);

        JPanel controlsPanel = new JPanel();

        snpField = new JTextField(10);
        snpField.setEnabled(false);
        JPanel snpPanel = new JPanel();
        snpPanel.add(new JLabel("SNP:"));
        snpPanel.add(snpField);
        goButton = new JButton("Go");
        goButton.addActionListener(this);
        goButton.setEnabled(false);
        snpPanel.add(goButton);
        JButton randomSNPButton = new JButton("Random");
        randomSNPButton.addActionListener(this);
        snpPanel.add(randomSNPButton);
        controlsPanel.add(snpPanel);

       /* JButton back = new JButton("Back");
        back.setAlignmentX(Component.CENTER_ALIGNMENT);
        back.addActionListener(this);
        controlsPanel.add(back);

        JPanel listPanel = new JPanel();
        JButton lp = new JButton("Prev");
        lp.addActionListener(this);
        listPanel.add(lp);
        JButton ln = new JButton("Next");
        ln.addActionListener(this);
        listPanel.add(ln);
        controlsPanel.add(listPanel);*/

        controlsPanel.add(Box.createRigidArea(new Dimension(50,1)));

        JPanel scorePanel = new JPanel();
        scorePanel.add(new JLabel("Approve?"));

        yesButton = new JButton("Yes");
        scorePanel.registerKeyboardAction(this,"Yes",
                KeyStroke.getKeyStroke(KeyEvent.VK_Y,0),
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        yesButton.addActionListener(this);
        yesButton.setEnabled(false);
        scorePanel.add(yesButton);

        maybeButton = new JButton("Maybe");
        scorePanel.registerKeyboardAction(this,"Maybe",
                KeyStroke.getKeyStroke(KeyEvent.VK_M,0),
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        maybeButton.addActionListener(this);
        maybeButton.setEnabled(false);
        scorePanel.add(maybeButton);

        noButton = new JButton("No");
        scorePanel.registerKeyboardAction(this,"No",
                KeyStroke.getKeyStroke(KeyEvent.VK_N,0),
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        noButton.addActionListener(this);
        noButton.setEnabled(false);
        scorePanel.add(noButton);

        controlsPanel.add(scorePanel);

        controlsPanel.setMaximumSize(new Dimension(2000,(int)controlsPanel.getPreferredSize().getHeight()));
        controlsPanel.setMinimumSize(new Dimension(10,(int)controlsPanel.getPreferredSize().getHeight()));

        plotArea = new JPanel();
        plotArea.setPreferredSize(new Dimension(700,350));
        plotArea.setBorder(new LineBorder(Color.BLACK));
        plotArea.setBackground(Color.WHITE);

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.add(plotArea);
        contentPanel.add(controlsPanel);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e){
                System.exit(0);
            }
        });
        

        this.setContentPane(contentPanel);
        this.pack();
        this.setVisible(true);

    }

    public void actionPerformed(ActionEvent actionEvent) {
        try{
            String command = actionEvent.getActionCommand();
            if (command.equals("Go")){
                plotIntensitas(snpField.getText());
            }else if (command.equals("No")){
                noButton.requestFocusInWindow();
                recordVerdict(-1);
            }else if (command.equals("Maybe")){
                maybeButton.requestFocusInWindow();
                recordVerdict(0);
            }else if (command.equals("Yes")){
                yesButton.requestFocusInWindow();
                recordVerdict(1);
            }else if (command.equals("Return to current list position")){
                plotIntensitas(currentSNPinList);
            }else if (command.equals("Random")){
                plotIntensitas(db.getRandomSNP());                
            }else if (command.startsWith("PLOTSNP")){
                String[] bits = command.split("\\s");
                plotIntensitas(bits[1]);
            }else if (command.equals("Open directory")){
                JFileChooser jfc = new JFileChooser(System.getProperty("user.dir"));
                jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                if (jfc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION){
                    db = new DataDirectory(jfc.getSelectedFile().getAbsolutePath());
                    finishLoadingDataSource();
                }
            }else if (command.equals("Connect to remote server")){
                DataClient dc = new DataClient(this);
                if (dc.getConnectionStatus()){
                    db = new DataDirectory(dc);
                    finishLoadingDataSource();
                }
            }else if (command.equals("Load marker list")){
                //JFileChooser jfc = new JFileChooser(System.getProperty("user.dir"));
                jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
                if (jfc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION){
                    loadList(jfc.getSelectedFile().getAbsolutePath());
                }
                FileOutputStream fos = new FileOutputStream(jfc.getSelectedFile().getAbsolutePath()+".scores");
                output = new PrintStream(fos);
            }else if (command.equals("Dump PNGs of all SNPs in list")){
                dumpAll();
            }else if (command.equals("Quit")){
                System.exit(0);
            }
        }catch (IOException ioe){
            JOptionPane.showMessageDialog(this,ioe.getMessage(),"File error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void dumpAll() throws IOException{
        for (String snp : snpList){
            //TODO: borken!
            /*for (int i = 0; i < collectionDropdown.getItemCount(); i++){
                collectionDropdown.setSelectedIndex(i);
                plotIntensitas(snp);
                //ppp.saveToFile(new File(snp+"-"+collectionDropdown.getSelectedItem()+".png"));
            } */
        }
    }

    private void recordVerdict(int v){
        if (currentSNPinList != null){
            output.println(currentSNPinList + "\t" + v);
            if (!snpList.isEmpty()){
                currentSNPinList = snpList.pop();
                plotIntensitas(currentSNPinList);
            }else{
                plotIntensitas("ENDLIST");
                currentSNPinList = null;
                output.close();
                yesButton.setEnabled(false);
                noButton.setEnabled(false);
                maybeButton.setEnabled(false);
                returnToListPosition.setEnabled(false);
            }
        }
    }

    private void viewedSNP(String name){

        boolean alreadyHere = false;
        for (Component i : historyMenu.getMenuComponents()){
            if (i instanceof  JMenuItem){
                if (((JMenuItem)i).getText().equals(name)){
                    ((JRadioButtonMenuItem)i).setSelected(true);
                    alreadyHere = true;
                    break;
                }
            }
        }

        if (alreadyHere){
            if (currentSNPinList != null){
                if (snpGroup.getSelection().getActionCommand().equals("PLOTSNP "+currentSNPinList)){
                    yesButton.setEnabled(true);
                    noButton.setEnabled(true);
                    maybeButton.setEnabled(true);
                }else{
                    //we're viewing a SNP from the history, so we can't allow
                    //the user to take any action on a SNP list (if one exists) because
                    //we're not viewing the "active" SNP from the list

                    yesButton.setEnabled(false);
                    noButton.setEnabled(false);
                    maybeButton.setEnabled(false);

                    //TODO: actually allow you to go back and change your mind?
                }
            }
        }else{
            //new guy
            JRadioButtonMenuItem snpItem = new JRadioButtonMenuItem(name);
            snpItem.setActionCommand("PLOTSNP "+name);
            snpItem.addActionListener(this);
            snpGroup.add(snpItem);
            snpItem.setSelected(true);
            historyMenu.add(snpItem,2);

            //only track last ten
            if (historyMenu.getMenuComponentCount() > 12){
                historyMenu.remove(12);
            }
        }

    }

    private void finishLoadingDataSource(){
        if (db != null){
            if (db.getCollections().size() == 3){
                this.setSize(new Dimension(1000,420));
            }else if (db.getCollections().size() == 4){
                this.setSize(new Dimension(700,750));
            }
            goButton.setEnabled(true);
            snpField.setEnabled(true);
            loadList.setEnabled(true);
        }
    }

    private void plotIntensitas(String name){
        plotArea.removeAll();
        plotArea.setLayout(new BoxLayout(plotArea,BoxLayout.Y_AXIS));

        if (name != null){
            if (!name.equals("ENDLIST")){
                plotArea.add(new JLabel(name));
                fetchRecord(name);
                viewedSNP(name);
            }else{
                //I tried very hard to get the label right in the middle and failed because java layouts blow
                plotArea.add(Box.createVerticalGlue());
                JPanel p = new JPanel();
                p.add(new JLabel("End of list."));
                p.setBackground(Color.WHITE);
                plotArea.add(p);
                plotArea.add(Box.createVerticalGlue());
            }
        }

        //seems to need both of these to avoid floating old crud left behind
        plotArea.revalidate();
        plotArea.repaint();
    }

    private void loadList(String filename)throws IOException{
        snpList = new ArrayDeque<String>();
        BufferedReader listReader = new BufferedReader(new FileReader(filename));
        String currentLine;
        StringTokenizer st;
        while ((currentLine = listReader.readLine()) != null){
            st = new StringTokenizer(currentLine);
            snpList.add(st.nextToken());
        }
        listReader.close();

        try{
            this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            db.listNotify(snpList.clone());
            currentSNPinList = snpList.pop();
            plotIntensitas(currentSNPinList);
            returnToListPosition.setEnabled(true);
            yesButton.setEnabled(true);
            noButton.setEnabled(true);
            maybeButton.setEnabled(true);
            yesButton.requestFocusInWindow();
        }finally{
            this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }


    }

    private void fetchRecord(String name){

        try{
            if (db.isRemote()){
                this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            }
            JPanel plotHolder = new JPanel();
            plotHolder.setBackground(Color.WHITE);
            plotArea.add(plotHolder);


            Vector<String> v = db.getCollections();
            Vector<PlotPanel> plots = new Vector<PlotPanel>();
            double maxdim=0;
            for (String c : v){
                PlotPanel pp = new PlotPanel(c,"a","b",
                        db.getRecord(name, c));

                pp.refresh();
                if (pp.getMaxDim() > maxdim){
                    maxdim = pp.getMaxDim();
                }
                plots.add(pp);
            }

            for (PlotPanel pp : plots){
                pp.setMaxDim(maxdim);
                plotHolder.add(pp);
            }
        }finally{
            this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
    }
}
