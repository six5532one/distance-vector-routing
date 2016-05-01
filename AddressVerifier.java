import java.util.AbstractMap;
import java.net.InetAddress;
import java.net.DatagramPacket;
import java.io.IOException;

class AddressVerifier extends Thread   {
    private Router router;

    public AddressVerifier(Router r)    {
        router = r;
    }

    public void run()   {
        while (true)    {
            for (AbstractMap.SimpleImmutableEntry<InetAddress,Integer> neighbor: router.neighbors)  { 
                InetAddress destIP = neighbor.getKey();
                int destPort = neighbor.getValue();
                byte[] sendData = destIP.getHostAddress().getBytes();
                DatagramPacket msg = new DatagramPacket(sendData, sendData.length, destIP, destPort);
                try {
                    router.outSocket.send(msg);
                }   catch (IOException e)   {
                    System.out.println("I/O error occurred while sending advertisement to neighbor. Exiting...");
                    System.exit(0);
                }
            }
            try {
                Thread.sleep(5000);
            }   catch (InterruptedException e)  {
                System.out.println("a thread has interrupted this thread's sleep interval");
            }
        }
    }
}
