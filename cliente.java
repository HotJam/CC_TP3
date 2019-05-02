import java.io.*;
import java.net.*;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

class Cliente {

  private static String username = "";
  private static String password = "";
  private static int porta = 0;
  private static String hostname = "";
  private static String fileName = "";
  private static String destFileName = "";
  private static int lossRate = 0;
  private static int retransmitted = 0;
  private static int totalTransferred = 0;
  private static final double previousTimeElapsed = 0;
  private static final int previousSize = 0;
  private static Chronometer timer;
  private static Scanner input = new Scanner(System.in);
  private static Socket s = null;

//asasas


  public static void main(String[] args) throws IOException, InterruptedException{

    System.out.println("username");
    username = input.nextLine();
    System.out.println("password");
    password = input.nextLine();

    while(!(username.equals("servercctp3") && password.equals("grupo64"))){
      System.out.println("Credencias incorretas! \n ->> tente: usr = servercctp3 passwd = grupo64");
      System.out.println("username");
      username = input.nextLine();
      System.out.println("password");
      password = input.nextLine();
    }


      System.out.println("Autenticação efetuada com sucesso \n > A processar pedido..");
      TimeUnit.SECONDS.sleep(2);

      porta = Integer.parseInt(args[0]);
      hostname = args[1];
      fileName = args[2];
      destFileName = args[3];

      DatagramSocket socket = new DatagramSocket();
      InetAddress address = InetAddress.getByName(hostname);

      byte[] saveDataFile = destFileName.getBytes();
      DatagramPacket filePacket = new DatagramPacket(saveDataFile, saveDataFile.length, address, porta);
      socket.send(filePacket);

      File ficheiro = new File(fileName);
      int fileLenght = (int) ficheiro.length();
      byte[] ficheiroByteArray = new byte[fileLenght];

      //enviar ficheiro
      beginTransfer(socket, ficheiroByteArray, address);

      System.out.println("> ficheiro " + fileName + " com sucesso!");
      socket.close();
  }

    private static void beginTransfer(DatagramSocket socket, byte[] fileByteArray, InetAddress address) throws IOException {

        int sequenceNumber = 0;
        boolean flag;
        int ackSequence = 0;

        for (int i = 0; i < fileByteArray.length; i = i + 1021) {
            sequenceNumber += 1;
            // Create message
            byte[] message = new byte[1024];
            message[0] = (byte) (sequenceNumber >> 8);
            message[1] = (byte) (sequenceNumber);

            if ((i + 1021) >= fileByteArray.length) {
                flag = true;
                message[2] = (byte) (1);
            } else {
                flag = false;
                message[2] = (byte) (0);
            }

            if (!flag) {
                System.arraycopy(fileByteArray, i, message, 3, 1021);
            } else { // If it is the last message
                System.arraycopy(fileByteArray, i, message, 3, fileByteArray.length - i);
            }

            DatagramPacket sendPacket = new DatagramPacket(message, message.length, address, porta);

            Random random = new Random();
            int randomInt = random.nextInt(100);
            lossRate = 100;

            if (randomInt <= lossRate) {
                  socket.send(sendPacket);
            }

            totalTransferred = sendPacket.getLength() + totalTransferred;
            totalTransferred = Math.round(totalTransferred);

            if (Math.round(totalTransferred / 1000) % 50 == 0) {
                printCurrentStats(totalTransferred, previousSize, timer, previousTimeElapsed);
            }

            System.out.println("Sent: Sequence number = " + sequenceNumber);

            // For verifying the the packet
            boolean ackRec;

            // The acknowledgment is not correct
            while (true) {
                // Create another packet by setting a byte array and creating
                // data gram packet
                byte[] ack = new byte[2];
                DatagramPacket ackpack = new DatagramPacket(ack, ack.length);

                try {
                    // set the socket timeout for the packet acknowledgment
                    socket.setSoTimeout(50);
                    socket.receive(ackpack);
                    ackSequence = ((ack[0] & 0xff) << 8)
                            + (ack[1] & 0xff);
                    ackRec = true;

                }
                // we did not receive an ack
                catch (SocketTimeoutException e) {
                    System.out.println("Socket timed out waiting for the ");
                    ackRec = false;
                }

                // everything is ok so we can move on to next packet
                // Break if there is an acknowledgment next packet can be sent
                if ((ackSequence == sequenceNumber)
                        && (ackRec)) {
                    System.out.println("Ack received: Sequence Number = "
                            + ackSequence);
                    break;
                }

                // Re send the packet
                else {
                    socket.send(sendPacket);
                    System.out.println("Resending: Sequence Number = "
                            + sequenceNumber);
                    // Increment retransmission counter
                    retransmitted += 1;
                }
            }
        }
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
