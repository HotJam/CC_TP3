import java.io.*;
import java.net.*;

public class servidor {

  //usr = serverCCTP3 pass = grupo64
  private static int totalTransferred = 0;
  private static String fileName = "";
  private int id=0;
  //private HashMap<String, String> utilizadores; //<username, password>

  public static void main(String[] args) throws IOException, FileNotFoundException{

    int porta = Integer.parseInt(args[0]);
    System.out.println("--SERVIDOR ATIVO--");
    //instancia socket para atribuir conexão
    DatagramSocket socketData = new DatagramSocket(porta);

      //instancia do pacote a ser enviado
      byte[] fileNamePacket = new byte[1024];
      DatagramPacket filePacket = new DatagramPacket(fileNamePacket, fileNamePacket.length);

      try{
        File file = new File(fileName);
        FileOutputStream outToFile = new FileOutputStream(file);
        acceptTransfer(outToFile, socketData);
        System.out.println("> A processar " + fileName + "...");
      }
      catch (FileNotFoundException f){
        f.printStackTrace();
      }


      byte[] destFile = new byte[1024];
      DatagramPacket destFilePacket = new DatagramPacket(destFile, destFile.length);

      socketData.receive(destFilePacket);

      System.out.println("Ficheiro " + fileName + " recebido com sucesso!");
      socketData.close();
  }

  private static void acceptTransfer(FileOutputStream outToFile, DatagramSocket socket) throws IOException {

        // last message flag
        boolean flag;
        int sequenceNumber = 0;
        int findLast = 0;

        while (true) {
            byte[] message = new byte[1024];
            byte[] fileByteArray = new byte[1021];

            // Receive packet and retrieve message
            DatagramPacket receivedPacket = new DatagramPacket(message, message.length);
            socket.setSoTimeout(0);
            socket.receive(receivedPacket);

            message = receivedPacket.getData();
            totalTransferred = receivedPacket.getLength() + totalTransferred;
            totalTransferred = Math.round(totalTransferred);

            // Get port and address for sending acknowledgment
            InetAddress address = receivedPacket.getAddress();
            int port = receivedPacket.getPort();

            // Retrieve sequence number
            sequenceNumber = ((message[0] & 0xff) << 8) + (message[1] & 0xff);
            // Retrieve the last message flag
            // a returned value of true means we have a problem
            flag = (message[2] & 0xff) == 1;
            // if sequence number is the last one +1, then it is correct
            // we get the data from the message and write the message
            // that it has been received correctly
            if (sequenceNumber == (findLast + 1)) {

                // set the last sequence number to be the one we just received
                findLast = sequenceNumber;

                // Retrieve data from message
                System.arraycopy(message, 3, fileByteArray, 0, 1021);

                // Write the message to the file and print received message
                outToFile.write(fileByteArray);
                System.out.println("Received: Sequence number:"
                        + findLast);

                // Send acknowledgement
                sendAck(findLast, socket, address, port);
            } else {
                System.out.println("Expected sequence number: "
                        + (findLast + 1) + " but received "
                        + sequenceNumber + ". DISCARDING");
                // Re send the acknowledgement
                sendAck(findLast, socket, address, port);
            }

            // Check for last message
            if (flag) {
                outToFile.close();
                break;
            }
        }
    }

    private static void sendAck(int findLast, DatagramSocket socket, InetAddress address, int port) throws IOException {
        // send acknowledgement
        byte[] ackPacket = new byte[2];
        ackPacket[0] = (byte) (findLast >> 8);
        ackPacket[1] = (byte) (findLast);
        // the datagram packet to be sent
        DatagramPacket acknowledgement = new DatagramPacket(ackPacket,
                ackPacket.length, address, port);
        socket.send(acknowledgement);
        System.out.println("Sent ack: Sequence Number = " + findLast);
    }

    public static void printCurrentStats(int totalTransferred, int previousSize, Chronometer timer, double previousTimeElapsed) {
        System.out.println();
        System.out.println();
        System.out.println("---------------------------------------------------------\n");

        int sizeDifference = totalTransferred / 1000 - previousSize;
        double difference = timer.getTime() - previousTimeElapsed;
        double throughput = totalTransferred / 1000 / timer.getTime();


        System.out.println("novos bytes recebidos: " + sizeDifference + "Kb");
        System.out.println("Recebidos: " + totalTransferred / 1000 + "Kb");
        System.out.println("Timer: " + timer.getTime() / 1000 + " Seconds");
        System.out.println("Descarga :" + throughput + "Mbps");

        System.out.println();
        System.out.println();
        System.out.println("---------------------------------------------------------\n");
    }
}
