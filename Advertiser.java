import java.util.Enumeration;
import java.net.NetworkInterface;
import java.util.AbstractMap;
import java.net.InetAddress;
import java.net.DatagramPacket;
import java.io.IOException;
import java.net.UnknownHostException;
import java.net.SocketException;

class Advertiser extends Thread    {
    private Router router;
    public Advertiser(Router r) {
        router = r;
    }

    private String getAllLocalIPAddresses()    {
        StringBuilder sb = new StringBuilder();
        try {
            Enumeration<NetworkInterface> n = NetworkInterface.getNetworkInterfaces();
            while (n.hasMoreElements()) {
                NetworkInterface netInt = n.nextElement();
                Enumeration<InetAddress> addresses = netInt.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    sb.append(addr.getHostAddress()).append("/");
                    //System.out.println("  " + addr.getHostAddress());
                }
            }
        }   catch (SocketException e)   {
            System.out.println("I/O error occurred while reading network interfaces. Exiting...");
            System.exit(0);
        }
        return sb.toString();
    }

    private String getPayload() {
        StringBuilder sb = new StringBuilder();
        for (String neighborIdStr: router.distVec.keySet())   {
            double neighborDist = router.distVec.get(neighborIdStr);
            sb.append(neighborIdStr).append(" ").append(neighborDist).append(":");
        }
        return sb.toString();
    }

    public void run()   {
        String potentialIPsForThisHost = getAllLocalIPAddresses();
        while (true)    {
            String payload = getPayload();
            String msg = potentialIPsForThisHost.concat(String.valueOf(router.listenPort)).concat("/").concat(payload);
            byte[] sendData = msg.getBytes();
            for (AbstractMap.SimpleImmutableEntry<InetAddress,Integer> neighbor: router.neighbors)  {
                InetAddress destIP = neighbor.getKey();
                int destPort = neighbor.getValue();
                DatagramPacket advertisement = new DatagramPacket(sendData, sendData.length, destIP, destPort);
                try {
                    router.outSocket.send(advertisement);
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
