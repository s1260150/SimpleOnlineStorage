package app;

import java.net.*;
import java.util.*;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

import com.mylib.*;


public class IoTServer
{
    private ServerSocket serverSock;
    //PORT Number
    public static final int PORT = 50576; //待ち受けポート番号

    static final String RESOURCES_FILE_PATH = "./Resources";

    List<Client> clients = new ArrayList<>();

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

        scrollPane = new JScrollPane();
        scrollPane.getViewport().setView(tree);
        scrollPane.setBounds(0, 0, 640, 400);

        frame.getContentPane().add(scrollPane);

        frame.addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                if(serverSock != null && !serverSock.isClosed())
                {
                    try
                    {
                        serverSock.close();
                    }
                    catch(IOException ex)
                    {
                        System.out.println("server socket can't close.");
                        ex.printStackTrace();
                    }
                    finally
                    {
                        serverSock = null;
                    }
                }

                for(Client c : clients)
                {
                    if(!c.isClosed())
                    {
                        c.close();
                    }
                }
                System.out.println("finish");
            }
        });
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
                System.out.println("welcome!");

                Client cc= new Client(this, clientSock);
                clients.add(cc);
                cc.start();
            }
        }
        catch(IOException e)
        {

        }
    }
    
    public void buildTreeView(Hashtable<MyFile, Object> table)
    {
        System.out.println("rebuild file tree");

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
