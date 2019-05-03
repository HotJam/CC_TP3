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
  private static Chronometer timer = new Chronometer();
  private static Scanner input = new Scanner(System.in);
  private static Socket s = null;


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
      TimeUnit.SECONDS.sleep(1);

      lossRate = Integer.parseInt(args[0]);
      porta = Integer.parseInt(args[1]);
      hostname = args[2];
      fileName = args[3];
      destFileName = args[4];

      DatagramSocket socket = new DatagramSocket();
      InetAddress address = InetAddress.getByName(hostname);

      byte[] saveDataFile = destFileName.getBytes();
      DatagramPacket filePacket = new DatagramPacket(saveDataFile, saveDataFile.length, address, porta);
      socket.send(filePacket);

      File ficheiro = new File(fileName);
      int fileLength = (int) ficheiro.length();
      byte[] ficheiroByteArray = new byte[fileLength];

      //enviar ficheiro
      timer.start();
      beginTransfer(socket, ficheiroByteArray, address);
      String stats = getFinalStats(ficheiroByteArray, retransmitted);
      sendServerFinalStats(socket, address, stats);
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

            if(randomInt <= lossRate){
              socket.send(sendPacket);
            }

            totalTransferred = sendPacket.getLength() + totalTransferred;
            totalTransferred = Math.round(totalTransferred);

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

/*    public static void printCurrentStats(int totalTransferred, int previousSize, Chronometer timer, double previousTimeElapsed) {

        int sizeDifference = totalTransferred / 1000 - previousSize;
        double difference = timer.getTime() - previousTimeElapsed;
        double velocidade = totalTransferred / 1000 / timer.getCurrentTime();

        System.out.println();
        System.out.println();
        System.out.println("---------------------------------------------------------");
        System.out.println("novos bytes recebidos: " + sizeDifference + "Kb");
        System.out.println("Recebidos: " + totalTransferred / 1000 + "Kb");
        System.out.println("Timer: " + timer.getCurrentTime() / 1000 + " Seconds");
        System.out.println("Descarga : " + velocidade + "Mbps");

        System.out.println();
        System.out.println("---------------------------------------------------------");
    }
*/
    private static String getFinalStats(byte[] fileByteArray, int retransmitted) {
        timer.stop();
        double fileSizeKB = (fileByteArray.length) / 1024;
        double transferTime = timer.getFinalTime() / 1000;
        double fileSizeMB = fileSizeKB / 1000;
        double velocidade = fileSizeMB / transferTime;

        System.out.println("\n\n");
        System.out.println("---------------------------------------------------------");
        System.out.println("                    --Stats--");
        System.out.println("---------------------------------------------------------");
        System.out.println("O ficheiro '" + fileName + "' foi enviado com sucesso!");
        System.out.println("Tamanho do ficheiro = " + totalTransferred / 1000 / 1000 + " MB");
        System.out.println("Tempo de transferência: " + transferTime + " Seconds");
        System.out.println("Velocidade de transferência:" + velocidade + " MB/s");
        System.out.println("Número de retransmissões: " + retransmitted);
        System.out.println("---------------------------------------------------------");
        System.out.println("\n\n");


        return "File Size: " + fileSizeMB + "mb\n"
                + "Velocidade: " + velocidade + " Mbps"
                + "\nTempo de transferência: " + transferTime + " Seconds";
    }

    private static void sendServerFinalStats(DatagramSocket socket, InetAddress address, String finalStatString) {
        byte[] bytesData;
        // convert string to bytes so we can send
        bytesData = finalStatString.getBytes();
        DatagramPacket statPacket = new DatagramPacket(bytesData, bytesData.length, address, porta);
        try {
            socket.send(statPacket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
