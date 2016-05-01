import java.util.Date;
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
    HashMap<String, Double> distVec;
    HashMap<String, Integer> forwardTo;
    private ArrayList<HashMap<String, Double>> neighborsDistVecs;
    int listenPort;
    private InetAddress myIp;
    DatagramSocket outSocket;
    private DatagramSocket inSocket;
    
    private void initializeDistVec()    {
        for (Double cost: neighborCosts) {
            int index = neighborCosts.indexOf(cost);
            AbstractMap.SimpleImmutableEntry<InetAddress,Integer> neighbor = neighbors.get(index);
            String distVecKey = Router.getNeighborIdString(neighbor.getKey(), neighbor.getValue());
            distVec.put(distVecKey, cost);
        }
    }
    
    public static String getNeighborIdString(InetAddress ip, int port)  {
        return ip.getHostAddress().concat(" ").concat(String.valueOf(port));
    }

    private void initializeNeighborDVs()    {
        for (AbstractMap.SimpleImmutableEntry<InetAddress,Integer> neighbor: neighbors) {
            neighborsDistVecs.add(new HashMap<String, Double>());
        }
    }

    private void initializeMyIp()   {
        byte[] receiveData = new byte[PAYLOAD_SIZE];
        DatagramPacket response;
        while (myIp == null)    {
            response = new DatagramPacket(receiveData, receiveData.length);
            try {
                inSocket.receive(response); 
                String received = new String(response.getData());
                try {
                    myIp = InetAddress.getByName(received.trim());
                }   catch (UnknownHostException e)  {} 
            }   catch (IOException e)   {
                System.out.println("I/O error occurred while reading from socket. Exiting...");
                System.exit(0);
            }
            receiveData = new byte[PAYLOAD_SIZE];
        }
    }

    private void initializeForwardTable()   {
        for (AbstractMap.SimpleImmutableEntry<InetAddress,Integer> neighbor: neighbors) {
            String forwardTableKey = Router.getNeighborIdString(neighbor.getKey(), neighbor.getValue());
            forwardTo.put(forwardTableKey, neighbors.indexOf(neighbor) + 1);
        }
    }

    public Router(int listenPort, ArrayList<Double> costs, ArrayList<AbstractMap.SimpleImmutableEntry<InetAddress,Integer>> interfaceTuples) {
        this.listenPort = listenPort;
        neighbors = interfaceTuples; 
        neighborCosts = costs;
        distVec = new HashMap<String, Double>();
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
        new Thread(new AddressVerifier(this)).start();
        initializeMyIp();
        forwardTo = new HashMap<String, Integer>();
        initializeForwardTable();
        Advertiser advertiser = new Advertiser(this);
        new Thread(advertiser).start();
    }
   
    private HashMap<String, Double> parseAdForDistVec(String adPayload, InetAddress neighborIp, int neighborPort) {
        HashMap<String, Double> result = new HashMap<String, Double>();
        String[] entries = adPayload.trim().split(":");
        for (int i=0; i<entries.length; i++)    {
            String[] entryComponents = entries[i].split(" ");
            double distance = Double.parseDouble(entryComponents[2]);
            result.put(entryComponents[0].concat(" ").concat(entryComponents[1]), distance);
        }
        result.put(Router.getNeighborIdString(neighborIp, neighborPort), 0.0);
        return result;
    }
    
    private HashMap<String, Double> updateDistVec() {
        HashMap<String, Double> result = new HashMap<String, Double>();
        // iterate through each neighbor's dist vec
        for (HashMap<String, Double> neighborDV: neighborsDistVecs) {
            int neighborIndex = neighborsDistVecs.indexOf(neighborDV);
            for (String nodeIdStr: neighborDV.keySet()) {
                if (nodeIdStr.equals(Router.getNeighborIdString(myIp, listenPort)))
                    continue;
                double distToNodeViaThisNeighbor = neighborCosts.get(neighborIndex) + neighborDV.get(nodeIdStr);
                if (result.containsKey(nodeIdStr))  { 
                    if (distToNodeViaThisNeighbor < result.get(nodeIdStr))  {
                        result.put(nodeIdStr, distToNodeViaThisNeighbor);
                        forwardTo.put(nodeIdStr, neighborIndex+1);
                    }
                }
                else    {
                    result.put(nodeIdStr, distToNodeViaThisNeighbor);
                    forwardTo.put(nodeIdStr, neighborIndex+1);
                }
            }
        }
        return result;
    }

    private void displayRoutingTable()  { 
        Date curTime = new Date();
        StringBuilder header = new StringBuilder("Node ");
        header.append(Router.getNeighborIdString(myIp, listenPort))
            .append(" @ ").append(curTime.toString());
        System.out.println(header.toString());
        System.out.println("host            port    distance    interface");
        for (String nodeIdStr: distVec.keySet())    {
            int interfaceNum = forwardTo.get(nodeIdStr);
            String[] nodeIdComponents = nodeIdStr.split(" ");
            StringBuilder sb = new StringBuilder();
            sb.append(nodeIdComponents[0])
                .append("\t").append(nodeIdComponents[1])
                .append("\t").append(distVec.get(nodeIdStr))
                .append("\t").append("\t").append(interfaceNum);
            System.out.println(sb.toString());
        }
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
                if (packetComponents.length < 3)
                    continue;
                String payload = packetComponents[packetComponents.length-1];
                int neighborPort = Integer.parseInt(packetComponents[packetComponents.length-2]);
                InetAddress neighborIp = null;
                int neighborIndex;
                for (int i=0; i<packetComponents.length-2; i++)   {
                    try {
                        neighborIp = InetAddress.getByName(packetComponents[i]);
                    }   catch (UnknownHostException e)  {}
                    if ((neighborIndex = neighbors.indexOf(new AbstractMap.SimpleImmutableEntry<InetAddress,Integer>(neighborIp, neighborPort))) > -1)  {
                        HashMap<String, Double> neighborDV = parseAdForDistVec(payload, neighborIp, neighborPort);
                        if (!(neighborDV.equals(neighborsDistVecs.get(neighborIndex)))) {
                            // update distance vector for this neighbor
                            neighborsDistVecs.set(neighborIndex, neighborDV);
                            HashMap<String, Double> recomputed = updateDistVec();
                            // display modified routing table
                            if (!(recomputed.equals(distVec)))    {
                                distVec = recomputed;
                                displayRoutingTable();
                            }
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
