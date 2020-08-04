package app;

import java.io.*;
import java.net.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Hashtable;

import com.mylib.*;

public class Client extends Thread
{
    private Socket sock;
    private MyReader reader;
    private MyWriter writer;

    private IoTServer iot;

    static final String MSG_REQUEST_FILES = "REQUEST_FILES";
    static final String MSG_SEND_FILES = "SEND_FILES";
    static final String MSG_DELETE_FILES = "DELETE_FILES";

    public Client(IoTServer iot, Socket sc) throws IOException
    {
        this.iot = iot;
        this.sock = sc;
        this.reader = new MyReader(sc.getInputStream());
        this.writer = new MyWriter(sc.getOutputStream());
    }

    @Override
    public void run()
    {
        //本来であれば一番外側の出力ストリームを指定しなければ
        //バッファリングされたデータがフラッシュされないので注意
        try(OutputStream outStream = sock.getOutputStream())
        {
            sendFileString();
            
            while(true)
            {
                String op = reader.readString();

                if(op.equals(MSG_REQUEST_FILES))
                {
                    sendFiles();
                }
                else if(op.equals(MSG_SEND_FILES))
                {
                    receiveFiles();
                }
                else if(op.equals(MSG_DELETE_FILES))
                {
                    deleteFiles();
                }
            }
        }
        catch(IOException e)
        {
            System.out.println("disconnected");
        }
        catch(MyInvalidPathException e)
        {
            e.printStackTrace();
        }
        System.out.println("bye");
    }

    public void receiveFiles() throws IOException, MyInvalidPathException
    {
        System.out.println("receive");

        String recPath = MyPaths.toString(reader.readString());
        
        System.out.println("receive path : " + recPath);

        new MyFilesReader(reader).read(MyPaths.toPath(Paths.get(IoTServer.RESOURCES_FILE_PATH).resolveSibling(recPath)));

        MyFile myFile = new MyFileSystem().scan(MyPaths.toPath(IoTServer.RESOURCES_FILE_PATH));
        Hashtable<MyFile, Object> treeData = new Hashtable<MyFile, Object>();
        treeData.put(myFile, iot.getTreeData(myFile));
        
        iot.buildTreeView(treeData);

        sendFileString();
    }

    public void sendFiles() throws IOException, MyInvalidPathException
    {
        String filename = MyPaths.toString(reader.readString());
        
        System.out.println("send : " + filename);

        Path p = MyPaths.toPath(filename).normalize();

        new MyFilesWriter(writer).write(p);
    }

    public void deleteFiles() throws IOException, MyInvalidPathException
    {
        String filename = MyPaths.toString(reader.readString());

        System.out.println("delete : " + filename);
        
        Path p = MyPaths.toPath(filename).normalize();

        if(p.toString().equals(MyPaths.toString(IoTServer.RESOURCES_FILE_PATH)))
        {
            System.out.println("the root can not be removed");
        }
        else 
        {
            new MyFileSystem().removeAll(p);
            
            System.out.println("deleted : " + p);
        }

        MyFile myFile = new MyFileSystem().scan(MyPaths.toPath((IoTServer.RESOURCES_FILE_PATH)));
        Hashtable<MyFile, Object> treeData = new Hashtable<MyFile, Object>();
        treeData.put(myFile, iot.getTreeData(myFile));
        
        iot.buildTreeView(treeData);

        sendFileString();
    }

    public void sendFileString() throws IOException, MyInvalidPathException
    {
        MyFile x = new MyFileSystem().scan(MyFileSystem.toPath(IoTServer.RESOURCES_FILE_PATH));
        writer.writeString(x.fileString());
    }

    public void close()
    {
        if(sock != null && !sock.isClosed())
        {  
            try
            {
                sock.close();
            }
            catch(IOException ex)
            {
                System.out.println("socket can't close.");
                ex.printStackTrace();
            }
            finally
            {
                sock = null;
            }
        }
    }

    public boolean isClosed()
    {
        return sock.isClosed();
    }
}
