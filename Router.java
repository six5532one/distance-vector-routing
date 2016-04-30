import java.util.ArrayList;
import java.util.AbstractMap;
import java.net.InetAddress;
import java.util.HashMap;
import java.net.UnknownHostException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.io.IOException;

class Router    {
    final ArrayList<AbstractMap.SimpleImmutableEntry<InetAddress,Integer>> neighbors;
    private int PAYLOAD_SIZE = 500;
    private final ArrayList<Double> neighborCosts;
    // does not include self; adds entry for self when it advertises to neighbors
    HashMap<Integer, Double> distVec;
    private ArrayList<HashMap<Integer, Double>> neighborsDistVecs;
    int listenPort;
    DatagramSocket outSocket;
    private DatagramSocket inSocket;
    
    private void initializeDistVec()    {
        for (Double cost: neighborCosts) {
            int index = neighborCosts.indexOf(cost);
            distVec.put(index, cost);
        }
    }

    public Router(int listenPort, ArrayList<Double> costs, ArrayList<AbstractMap.SimpleImmutableEntry<InetAddress,Integer>> interfaceTuples) {
        this.listenPort = listenPort;
        neighbors = interfaceTuples;
        neighborCosts = costs;
        distVec = new HashMap<Integer, Double>();
        initializeDistVec();
        try {
            inSocket = new DatagramSocket(listenPort); 
            outSocket = new DatagramSocket();
        }   catch (SocketException e)   {
            System.out.println("Socket could not be opened, or the socket could not bind to the specified port. Exiting...");
            System.exit(0);
        }
        Advertiser advertiser = new Advertiser(this);
        new Thread(advertiser).start();
    }
    
    private void route()    {
        byte[] receiveData;
        DatagramPacket response;
        while (true)    {
            receiveData = new byte[PAYLOAD_SIZE];
            response = new DatagramPacket(receiveData, receiveData.length);
            try {
                inSocket.receive(response);
            byte[] received = response.getData();
            System.out.println(new String(received));
            }   catch (IOException e)   {
                System.out.println("I/O error occurred while reading from socket. Exiting...");
                System.exit(0);
            }
        }
    }

    public static void main(String[] args)  {
        if (args.length < 2)    {
            System.out.println("Usage: java Router <listenPort> <IP1:port1:cost1> ... <IPn:portn:costn>");
            System.exit(0);
        }
        int listenPort = Integer.parseInt(args[0]);
        ArrayList<Double> costs = new ArrayList<Double>();
        ArrayList<AbstractMap.SimpleImmutableEntry<InetAddress,Integer>> interfaceTuples = new ArrayList<AbstractMap.SimpleImmutableEntry<InetAddress,Integer>>();
        for (int i=1; i<args.length; i++) {
            String[] interfaceTuple = args[i].split(":");
            try {
                InetAddress ip = InetAddress.getByName(interfaceTuple[0]);
                int port = Integer.parseInt(interfaceTuple[1]);
                double cost = Double.parseDouble(interfaceTuple[2]);
                interfaceTuples.add(new AbstractMap.SimpleImmutableEntry<InetAddress,Integer>(ip, port));
                costs.add(cost);
            }   catch (UnknownHostException e)  {
                System.out.println("no IP address found for: " + interfaceTuple[0]);
                System.exit(0);
            }
        }
        Router router = new Router(listenPort, costs, interfaceTuples);
        router.route();
    }
}
