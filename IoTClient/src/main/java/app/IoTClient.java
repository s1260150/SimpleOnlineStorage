package app;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Hashtable;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.*;

import java.awt.event.*;

import com.mylib.*;


public class IoTClient
{
    public static String HOST = "192.168.10.120";
    //public static String HOST = "localhost";
    public static final int PORT    = 50576;

    private Server server;

    private MyFile rootFile;
    private Path fChooserPath = Paths.get("./");

    private JFrame frame;
    private JScrollPane scrollPane;
    private JTree tree;

    public IoTClient() throws Exception
    {
        frame = new JFrame();
        frame.setLayout(null);

        tree = new JTree();
        tree.addTreeSelectionListener(new TreeSelectionListener(){
            @Override
            public void valueChanged(TreeSelectionEvent e) {
            }
        });

        scrollPane = new JScrollPane();
        scrollPane.getViewport().setView(tree);
        scrollPane.setBounds(0, 0, 640, 400);

        frame.getContentPane().add(scrollPane);

        JButton recBtn = new JButton("Receive");
        recBtn.setBounds(10, 410, 80, 25);
        recBtn.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode)tree.getLastSelectedPathComponent();

                if (node != null){
                    String fname = ((MyFile)node.getUserObject()).toPath().toString();
                    
                    server.receiveFiles(fname);
                }
            }
        });

        frame.getContentPane().add(recBtn);

        JButton sendBtn = new JButton("Send");
        sendBtn.setBounds(100, 410, 80, 25);
        sendBtn.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {

                DefaultMutableTreeNode node = (DefaultMutableTreeNode)tree.getLastSelectedPathComponent();

                String recPath;
                if (node != null){
                    MyFile mFile = (MyFile)node.getUserObject();
                    if(mFile.isFile())
                    {
                        JOptionPane.showMessageDialog(frame, "Please specify the correct destination", "Warn", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    recPath = mFile.toPath().toString();
                }
                else
                {
                    recPath = (rootFile != null ? rootFile.getFilename() : "");
                }

                JFileChooser filechooser = new JFileChooser(fChooserPath.toString());
                filechooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

                int selected = filechooser.showOpenDialog(frame);
                if (selected == JFileChooser.APPROVE_OPTION)
                {
                    File file = filechooser.getSelectedFile();

                    fChooserPath = file.toPath();

                    server.sendFiles(file.getPath(), recPath);
                    return;
                }
                else if (selected == JFileChooser.ERROR_OPTION)
                {
                    System.out.println("FileChooser : error");
                    return;
                }
            }
        });

        frame.getContentPane().add(sendBtn);

        JButton deleteBtn = new JButton("Delete");
        deleteBtn.setBounds(190, 410, 80, 25);
        deleteBtn.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode)tree.getLastSelectedPathComponent();

                if (node != null){
                    MyFile mFile = (MyFile)node.getUserObject();
                    String fname = mFile.toPath().toString();
                    
                    if(rootFile.toPath().toString().equals(mFile.toPath().toString()))
                    {
                        JOptionPane.showMessageDialog(frame, "Can not remove root directory", "Warn", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    server.deleteFiles(fname);
                }
            }
        });

        frame.getContentPane().add(deleteBtn);

        frame.addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                if(server != null) server.close();
            }
        });
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(640, 480);
        frame.setTitle("Title");
        frame.setVisible(true);
    }

    public void update() throws IOException
    {
        //MyReader user = new MyReader(System.in);
        //HOST = user.readLine();
        try(Socket cs = new Socket(HOST, PORT))
        {
            server = new Server(this, cs);
            server.update();
        }
        finally
        {
            System.out.println("Connection Closed");
        }
    }

    public void sendFiles(String source, String recPath)
    {
        server.sendFiles(source, recPath);
    }

    public void buildTreeView(Hashtable<MyFile, Object> table)
    {
        scrollPane.remove(tree);
        tree = new JTree(table);
        scrollPane.getViewport().setView(tree);
    }
    
    public Hashtable<MyFile, Object> getTreeData(MyFile file)
    {
        Hashtable<MyFile, Object> table = new Hashtable<MyFile, Object>();

        for(MyFile f : file.files)
        {
            if(f.isFile())
            {
                table.put(f, f);
            }
            else
            {
                table.put(f, getTreeData(f));
            }
        }

        return table;
    }

    public void setRootFile(MyFile rootFile)
    {
        this.rootFile = rootFile;
    }
}