package app;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.Hashtable;

import com.mylib.*;

public class Server {
    private IoTClient iot;
    private Socket sock;
    private MyReader reader;
    private MyWriter writer;
    
    static final String MSG_REQUEST_FILES = "REQUEST_FILES";
    static final String MSG_SEND_FILES = "SEND_FILES";
    static final String MSG_DELETE_FILES = "DELETE_FILES";

    public Server(IoTClient iot, Socket sc) throws IOException
    {
        this.iot = iot;
        this.sock = sc;
        this.reader = new MyReader(sc.getInputStream());
        this.writer = new MyWriter(sc.getOutputStream());
    }

    public void update()
    {
        Thread receiver = new Thread(){
            public void run(){
                try
                {
                    String fileString = reader.readString();

                    rebuildTreeView(fileString);

                    while(true){}
                }
                catch(IOException e)
                {
                    e.printStackTrace();
                }
            }
        };

        receiver.start();

        while(receiver.isAlive());
    }

    public void sendFiles(String source, String recPath)
    {
        try
        {
            Path path = Paths.get(new MyFileSystem().resolve(source)).normalize();
            if(Files.notExists(path))
            {
                System.out.println("\"" + path.toString() + "\"" + " doesn't exists");
                return;
            }

            System.out.println("送信します");
            System.out.println("レシーブディレクトリ : " + recPath);
            System.out.println("送信対象 : " + path.toString());

            writer.writeString(MSG_SEND_FILES);

            writer.writeString(recPath);

            new MyFilesWriter(writer).write(path);

            String fileString = reader.readString();

            rebuildTreeView(fileString);
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    public void receiveFiles(String source)
    {
        try
        {
            writer.writeString(MSG_REQUEST_FILES);

            writer.writeString(source);

            new MyFilesReader(reader).read(Paths.get("./Resources"));
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    public void deleteFiles(String source)
    {
        try
        {
            writer.writeString(MSG_DELETE_FILES);

            writer.writeString(source);

            String fileString = reader.readString();

            System.out.println(fileString);
            
            rebuildTreeView(fileString);
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    public void rebuildTreeView(String fileString)
    {
        System.out.println(fileString);

        MyFile myFile = MyFile.build(fileString);

        iot.setRootFile(myFile);

        Hashtable<MyFile, Object> treeData = new Hashtable<MyFile, Object>();
                
        treeData.put(myFile, iot.getTreeData(myFile));

        iot.buildTreeView(treeData);
    }

    public boolean isClosed()
    {
        if(sock == null) return true;
        return sock.isClosed();
    }

    public void close()
    {
        if(!isClosed())
        {
            try
            {
                sock.close();
            }
            catch(IOException e)
            {
                System.out.print("socket can't close");
                e.printStackTrace();
            }
            finally
            {
                sock = null;
            }
        }
    }
}

