Packet Format
==============
Each advertisement has the format:
```
A1/A2/.../Ak/<port number of router that is sending the message>/<payload>
```
where A1, A2, ... Ak are each an IP address of the router sending the message.

The payload at the end of each advertisement has the format:
```
<node1_IP> <node1_Port> <distance from this router to node1>:...:<noden_IP> <noden_Port> <distance from this router to noden>
```

Testing
=======
I ran these commands to test the program:
```
// Node X (bern.clic.cs.columbia.edu)
$ ssh ec2805@128.59.15.59
$ java Router 9000 128.59.15.58:9000:2 128.59.15.66:9000:7 | tee nodeX_log.txt

// Node Y (lisbon.clic.cs.columbia.edu)
$ ssh ec2805@128.59.15.58
$ java Router 9000 128.59.15.59:9000:2 128.59.15.66:9000:1 | tee nodeY_log.txt

// Node Z (islamabad.clic.cs.columbia.edu)
$ ssh ec2805@128.59.15.66
$ java Router 9000 128.59.15.59:9000:7 128.59.15.58:9000:1 128.59.15.68:9000:3 | tee nodeZ_log.txt

// Node W (baghdad.clic.cs.columbia.edu)
$ ssh ec2805@128.59.15.68
$ java Router 9000 128.59.15.66:9000:3 128.59.15.55:9000:5 | tee nodeW_log.txt

// Node V (amman.clic.cs.columbia.edu)
$ ssh ec2805@128.59.15.55
$ java Router 9000 128.59.15.68:9000:5 128.59.15.75:9000:2 | tee nodeV_log.txt

// Node U (wellington.clic.cs.columbia.edu)
$ ssh ec2805@128.59.15.75
$ java Router 9000 128.59.15.55:9000:2 | tee nodeU_log.txt
