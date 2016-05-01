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
    private ArrayList<HashMap<String, Double>> neighborsDistVecs;
    int listenPort;
    DatagramSocket outSocket;
    private DatagramSocket inSocket;
    
    private void initializeDistVec()    {
        for (Double cost: neighborCosts) {
            int index = neighborCosts.indexOf(cost);
            distVec.put(index, cost);
        }
    }
    
    private void initializeNeighborDVs()    {
        for (AbstractMap.SimpleImmutableEntry<InetAddress,Integer> neighbor: neighbors) {
            neighborsDistVecs.add(new HashMap<String, Double>());
        }
    }

    public Router(int listenPort, ArrayList<Double> costs, ArrayList<AbstractMap.SimpleImmutableEntry<InetAddress,Integer>> interfaceTuples) {
        this.listenPort = listenPort;
        neighbors = interfaceTuples;
        neighborCosts = costs;
        distVec = new HashMap<Integer, Double>();
        initializeDistVec();
        neighborsDistVecs = new ArrayList<HashMap<String, Double>>();
        initializeNeighborDVs();
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
   
    private HashMap<String, Double> parseAdForDistVec(String adPayload) {
        HashMap<String, Double> result = new HashMap<String, Double>();
        String[] entries = adPayload.trim().split(":");
        for (int i=0; i<entries.length; i++)    {
            String[] entryComponents = entries[i].split(" ");
            double distance = Double.parseDouble(entryComponents[2]);
            result.put(entryComponents[0].concat(" ").concat(entryComponents[1]), distance);
        }
        return result;
    }

    private void route()    {
        byte[] receiveData;
        DatagramPacket response;
        while (true)    {
            receiveData = new byte[PAYLOAD_SIZE];
            response = new DatagramPacket(receiveData, receiveData.length);
            try {
                inSocket.receive(response);
                String received = new String(response.getData());
                String[] packetComponents = received.trim().split("/");
                String payload = packetComponents[packetComponents.length-1];
                System.out.println("payload: "+payload);
                int neighborPort = Integer.parseInt(packetComponents[packetComponents.length-2]);
                InetAddress neighborIp = null;
                int neighborIndex;
                for (int i=0; i<packetComponents.length-2; i++)   {
                    try {
                        neighborIp = InetAddress.getByName(packetComponents[i]);
                    }   catch (UnknownHostException e)  {}
                    if ((neighborIndex = neighbors.indexOf(new AbstractMap.SimpleImmutableEntry<InetAddress,Integer>(neighborIp, neighborPort))) > -1)  {
                        HashMap<String, Double> neighborDV = parseAdForDistVec(payload);
                        if (!(neighborDV.equals(neighborsDistVecs.get(neighborIndex)))) {
                            neighborsDistVecs.set(neighborIndex, neighborDV);
                            System.out.println("Updated distance vector for "+packetComponents[i]+": "+payload);
                        }
                        else    {
                            System.out.println("distance vector for "+packetComponents[i]+" hasn't changed");
                        }
                        break;
                    }
                }   // updated dist vec for the neighbor that sent the ad
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
