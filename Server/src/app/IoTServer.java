package app;

import java.net.*;
import java.util.Hashtable;

import javax.swing.*;
import javax.swing.event.*;

import com.mylib.*;


public class IoTServer
{
    private ServerSocket serverSock;
    //PORT Number
    public static final int PORT = 50576; //待ち受けポート番号

    static final String RESOURCES_FILE_PATH = "./Resources";

    private JFrame frame;
    JScrollPane scrollPane;
    JTree tree;

    public IoTServer() throws Exception
    {
        frame = new JFrame();
        frame.setLayout(null);

        Hashtable<MyFile, Object> treeData = new Hashtable<MyFile, Object>();
        
        MyFile x = new MyFileSystem().scan(MyFileSystem.toPath("./Resources"));

        treeData.put(x, getTreeData(x));

        tree = new JTree(treeData);
        tree.addTreeSelectionListener(new TreeSelectionListener(){
            @Override
            public void valueChanged(TreeSelectionEvent e) {
            }
        });

        scrollPane = new JScrollPane();
        scrollPane.getViewport().setView(tree);
        scrollPane.setBounds(0, 0, 640, 400);

        frame.getContentPane().add(scrollPane);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setTitle("Control Center");
        frame.setSize(640, 480);
        frame.setVisible(true);
    }

    public void update()
    {
        try(ServerSocket ss = new ServerSocket(PORT))
        {
            serverSock = ss;

            System.out.println(new MyFileSystem().getPath().toString());
            while(true)
            {
                //サーバー側ソケット作成
                Socket clientSock = serverSock.accept();
                System.out.println("Welcome!");

                Client cc= new Client(this, clientSock);
                cc.start();
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
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
}
