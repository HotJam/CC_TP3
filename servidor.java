import java.io.*;
import java.net.*;

public class servidor {

  //usr = serverCCTP3 pass = grupo64
  private static int totalTransferred = 0;
  private static Chronometer timer = new Chronometer();
  private static String fileName = "";
  private int id=0;
  private static int porta = 0;
  private static String decodeFile = null;
  private static ServerSocket server;
  private static Socket ss;


  public static void main(String[] args) throws IOException, FileNotFoundException{

      porta = Integer.parseInt(args[0]);
      System.out.println("--SERVIDOR ATIVO--");

      //instancia datagram socket para atribuir conexão server socket para comunicação
      DatagramSocket socketData = new DatagramSocket(porta);

      boolean exit_flag = false;


          //instancia do pacote a ser recebido
          byte[] fileNamePacket = new byte[1024];
          DatagramPacket filePacket = new DatagramPacket(fileNamePacket, fileNamePacket.length);
          socketData.receive(filePacket);


          try{
            decodeFile = new String(fileNamePacket, "UTF-8");
          }
          catch (UnsupportedEncodingException e){
            e.printStackTrace();
          }

          fileName = decodeFile.trim();
          File file = new File(fileName);
          //file.canRead();
          file.canWrite();
          file.canExecute();
          FileOutputStream outToFile = new FileOutputStream(file);

          //BufferedWriter bw = new BufferedWriter(new FileWriter(file));

          System.out.println("> A processar " + fileName + "...");
          acceptTransfer(outToFile, socketData);

          byte[] finalStatData = new byte[1024];
          DatagramPacket receiveStatPacket = new DatagramPacket(finalStatData, finalStatData.length);
          socketData.receive(receiveStatPacket);
          printStatPacket(finalStatData);

          socketData.close();
  }

  private static void acceptTransfer(FileOutputStream outToFile, DatagramSocket socket) throws IOException {

        // flag para última mensagem
        boolean flag;
        int sequenceNumber = 0;
        int findLast = 0;

        while (true) {
            byte[] message = new byte[1024];
            byte[] fileByteArray = new byte[1021];

            // Buscar pacote e receber mensagem
            DatagramPacket receivedPacket = new DatagramPacket(message, message.length);
            socket.setSoTimeout(0);
            socket.receive(receivedPacket);

            message = receivedPacket.getData();
            totalTransferred = receivedPacket.getLength() + totalTransferred;
            totalTransferred = Math.round(totalTransferred);

            if(sequenceNumber == 0){
              timer.start();
            }

            // Get Port e ADDRESS para mandar acknowledgment
            InetAddress address = receivedPacket.getAddress();
            int port = receivedPacket.getPort();

            System.out.println("> ADDRESS: " + address.toString() + " PORTA: " + port);


            // calcular sequence number
            sequenceNumber = ((message[0] & 0xff) << 8) + (message[1] & 0xff);

            // atribuir valor booleano à mensagem sequencia de bits a 1
            // se flag = true ocorreu um problema !
            flag = (message[2] & 0xff) == 1;


            // se o sequence number é igual ao útlimo recebido +1 está OK
            // a seguir é preciso ir buscar os dados da útlima mensagem recebida com sucesso
            if (sequenceNumber == (findLast + 1)) {

                // é preciso dizer que o sequence number atual será o útlimo recebido para a próxima itereção
                findLast = sequenceNumber;

                // Copiar os dados da mensagem recebida
                System.arraycopy(message, 3, fileByteArray, 0, 1021);

                // Escrever os dados recebidos para o ficheiro output
                //outToFile.writeInt(fileByteArray.length);
                outToFile.write(fileByteArray);
                outToFile.flush();
                //IOUtils.write(fileByteArray, outToFile);

                System.out.println("Received: Sequence number: " + findLast);

                // Enviar acknowledgement
                sendAck(findLast, socket, address, port);
            } else {
                // Se o sequence number não estiver correto é mandado um novo acknowledgment
                System.out.println("Expected sequence number: "
                        + (findLast + 1) + " but received "
                        + sequenceNumber + ". DISCARDING");

                sendAck(findLast, socket, address, port);
            }

            // Check flag message
            if (flag) {
                outToFile.flush();
                outToFile.close();
                break;
            }
        }

    }

    private static void sendAck(int findLast, DatagramSocket socket, InetAddress address, int port) throws IOException {

        byte[] ackPacket = new byte[2];
        ackPacket[0] = (byte) (findLast >> 8);
        ackPacket[1] = (byte) (findLast);

        // Declarar o datagrama a ser enviado
        DatagramPacket acknowledgement = new DatagramPacket(ackPacket, ackPacket.length, address, port);

        socket.send(acknowledgement);
        System.out.println("Sent ack: Sequence Number = " + findLast);
    }

    private static void printStatPacket(byte[] finalStatData){
      try {
          String decode = new String(finalStatData, "UTF-8");
          System.out.println("\n\n");
          System.out.println("---------------------------------------------------------");
          System.out.println("                    --Stats--");
          System.out.println("---------------------------------------------------------");
          System.out.println("Ficheiro guardado como: " + fileName);
          System.out.println("" + decode.trim());
          System.out.println("---------------------------------------------------------\n");

      } catch (UnsupportedEncodingException e) {
          e.printStackTrace();
      }
    }

}
