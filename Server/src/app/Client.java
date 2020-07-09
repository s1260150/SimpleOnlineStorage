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
            e.printStackTrace();
            System.out.println("Connection Closed");
        }
        catch(MyInvalidPathException e)
        {
            e.printStackTrace();
        }
        System.out.println("Good Bye !!");
    }

    public void receiveFiles() throws IOException, MyInvalidPathException
    {
        String recPath = reader.readString();

        new MyFilesReader(reader).read(Paths.get(IoTServer.RESOURCES_FILE_PATH).resolveSibling(recPath));
        
        System.out.println("Rebuild Tree View");

        MyFile myFile = new MyFileSystem().scan(Paths.get(IoTServer.RESOURCES_FILE_PATH));
        Hashtable<MyFile, Object> treeData = new Hashtable<MyFile, Object>();
        treeData.put(myFile, iot.getTreeData(myFile));
        
        iot.buildTreeView(treeData);

        sendFileString();
    }

    public void sendFiles() throws IOException, MyInvalidPathException
    {
        String filename = reader.readString();

        Path p = Paths.get(filename).normalize();

        new MyFilesWriter(writer).write(p);
    }

    public void deleteFiles() throws IOException, MyInvalidPathException
    {
        String filename = reader.readString();

        System.out.println("deleteFiles filename : " + filename);
        
        Path p = Paths.get(filename).normalize();

        if(!p.toString().equals(Paths.get(IoTServer.RESOURCES_FILE_PATH).toString()))
        {
            new MyFileSystem().removeAll(p);
            
            System.out.println("Deleted : " + p);
        }

        System.out.println("Rebuild Tree View");

        MyFile myFile = new MyFileSystem().scan(Paths.get(IoTServer.RESOURCES_FILE_PATH));
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
}
