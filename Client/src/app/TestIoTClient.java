package app;


public class TestIoTClient {
    public static void main(String[] args)
    {
        try
        {
            IoTClient client = new IoTClient();
            client.update();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }   
}