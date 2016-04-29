import java.util.ArrayList;
import java.util.AbstractMap;
import java.net.InetAddress;
import java.util.HashMap;
import java.net.UnknownHostException;

class Router    {
    private final ArrayList<AbstractMap.SimpleImmutableEntry<InetAddress,Integer>> neighbors;
    private final ArrayList<Double> neighborCosts;
    // does not include self; adds entry for self when it advertises to neighbors
    HashMap<Integer, Double> distVec;
    private ArrayList<HashMap<Integer, Double>> neighborsDistVecs;
    private DatagramSocket outSocket;
    DatagramSocket inSocket;
    
    private HashMap<Integer, Double> initializeDistVec()    {
        for (Double cost: neighborCosts) {
            int index = neighborCosts.indexOf(cost);
            distVec.put(index, cost);
        }
    }

    public Router(int listenPort, ArrayList<Double> costs, ArrayList<AbstractMap.SimpleImmutableEntry<InetAddress,Integer>> interfaceTuples) {
        neighbors = interfaceTuples;
        neighborCosts = costs;
        distVec = initializeDistVec();
        inSocket = new DatagramSocket(listenPort); 
        outSocket = new DatagramSocket();
        Advertiser advertiser = new Advertiser(this);
        new Thread(advertiser).start();
    }

    private void advertise()    {
        // create packet header with source IP and port
        StringBuilder sb = new StringBuilder();
        // iterate through neighbors, send to each one
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
        router.advertise();
    }
}
