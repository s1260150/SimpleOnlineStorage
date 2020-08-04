package app;

public class TestIoTServer {
    public static void main(String[] args)
    {
        try
        {
            IoTServer server = new IoTServer();
            server.update();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}