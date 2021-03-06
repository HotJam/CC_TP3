import java.io.*;
import java.net.*;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.StringTokenizer;

class cliente {

  private static String username = "";
  private static String password = "";
  private static int porta = 0;
  private static String hostname = "";
  private static String fileName = "";
  private static String destFileName = "";
  private static int retransmitted = 0;
  private static int totalTransferred = 0;
  private static final double previousTimeElapsed = 0;
  private static final int previousSize = 0;
  private static Chronometer timer = new Chronometer();
  private static Scanner input = new Scanner(System.in);
  private static DatagramSocket socket;


  public static void main(String[] args) throws IOException, InterruptedException{

      System.out.print("username ");
      username = input.nextLine();
      System.out.print("password ");
      password = input.nextLine();

      while(!(username.equals("servercctp2") && password.equals("grupo64"))){
        System.out.println("Credenciais incorretas! \n -> tente: usr = servercctp2 passwd = grupo64");
        System.out.print("username ");
        username = input.nextLine();
        System.out.print("password ");
        password = input.nextLine();
      }

      System.out.println(" Aguarde..\n\n");
      TimeUnit.SECONDS.sleep(1);
      System.out.println(">Autenticação efetuada com sucesso!");

      String linha = "";
      String[] comando;
      boolean exit_flag = false;

      //while(!exit_flag){
        System.out.println("Insira comandos no formato:    [get <file> <dest> <lossrate>]  [put <file> <dest> <lossrate>]\n");
        System.out.print(">");
        linha = input.nextLine();
        comando = linha.split(" ");
        switch (comando[0]){
          case "get":
            System.out.println(">Funcionalidade não suportada!");
            break;
          case "put":
            fileName = comando[1];
            setFileName(fileName);
            destFileName = comando[2];
            setDestFile(destFileName);

            porta = Integer.parseInt(args[0]);
            setPort(porta);
            hostname = args[1];
            setHostname(hostname);

            socket = new DatagramSocket();
            InetAddress address = InetAddress.getByName(getHostname());

            String aux = getDestFileName();
            byte[] saveDataFile = new byte[1024];
            saveDataFile = aux.getBytes();

            DatagramPacket filePacket = new DatagramPacket(saveDataFile, saveDataFile.length, address, getPort());
            socket.send(filePacket);

            File ficheiro = new File(getFileName());
            FileInputStream fileInputStream = new FileInputStream(ficheiro);
            byte[] ficheiroByteArray = new byte[(int)ficheiro.length()];
            fileInputStream.read(ficheiroByteArray);

            //enviar ficheiro
            timer.start();
            beginTransfer(socket, ficheiroByteArray, address);
            String stats = getFinalStats(ficheiroByteArray, retransmitted);
            sendServerFinalStats(socket, address, stats);
            break;
        /*  case "sair":
            exit_flag = true;
            String exit = "sair";
            byte[] exitConnection = new byte[1024];
            exitConnection = exit.getBytes();

            InetAddress add = InetAddress.getByName(getHostname());
            DatagramPacket exitPacket = new DatagramPacket(exitConnection, exitConnection.length, add, getPort());
            socket.send(exitPacket);
            socket.close();
            break;*/
          default:
            System.out.println(">Comando não suportado!\n>Tente novamente ou escreva 'sair' para cancelar!");
        }
      //}
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

            // verificar valor da mensagem
            // se a flag contiver o valor false é pq chegou ao fim do ficheiro
            if (!flag) {
                System.arraycopy(fileByteArray, i, message, 3, 1021);
                //System.out.println("DADOS: " + fileByteArray.length);
            } else {
                System.arraycopy(fileByteArray, i, message, 3, fileByteArray.length - i);
                //System.out.println("DADOS Final : " + fileByteArray.length);
            }

            DatagramPacket sendPacket = new DatagramPacket(message, message.length, address, getPort());
            socket.send(sendPacket);


            totalTransferred = sendPacket.getLength() + totalTransferred;
            totalTransferred = Math.round(totalTransferred);

            System.out.println("Sent: Sequence number = " + sequenceNumber);

            // flag para verificação de pacote ackRec = true -> ok!
            boolean ackRec;

            while (true) {

                // Criar Novo DatagramPacket para enviar Acks
                byte[] ack = new byte[2];
                DatagramPacket ackpack = new DatagramPacket(ack, ack.length);

                try {

                    // set timeout para os Acks
                    socket.setSoTimeout(50);
                    socket.receive(ackpack);
                    ackSequence = ((ack[0] & 0xff) << 8) + (ack[1] & 0xff);
                    ackRec = true;

                }
                catch (SocketTimeoutException e) {
                    // se não receber Acks
                    System.out.println("Socket timed out waiting for the ");
                    ackRec = false;
                }


                // Se ackSequence e o sequence Number são iguais e recebeu um Ack (ackRec=true)
                if ((ackSequence == sequenceNumber) && (ackRec)) {
                    System.out.println("Ack received: Sequence Number = " + ackSequence);
                    break;
                }
                else {
                    // Renviar pacote e incrementar o contador de retransmissões
                    socket.send(sendPacket);
                    System.out.println("Resending: Sequence Number = " + sequenceNumber);
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
        double transferTime = timer.getSeconds();
        double fileSizeMB = fileSizeKB / 1000;
        double velocidade = fileSizeMB / transferTime;

        System.out.println("\n\n");
        System.out.println("---------------------------------------------------------");
        System.out.println("                    --Stats--");
        System.out.println("---------------------------------------------------------");
        System.out.println("O ficheiro '" + fileName + "' foi enviado com sucesso!");
        System.out.println("Tamanho do ficheiro = " + fileSizeMB + " mb");
        System.out.println("Tempo de transferência: " + transferTime + " Seconds");
        System.out.println("Velocidade de transferência: " + velocidade + " Mbps");
        System.out.println("Número de retransmissões: " + retransmitted);
        System.out.println("---------------------------------------------------------");
        System.out.println("\n\n");


        return "File Size: " + fileSizeMB + " mb\n"
                + "Velocidade: " + velocidade + " Mbps"
                + "\nTempo de transferência: " + transferTime + " Seconds";
    }

    private static void sendServerFinalStats(DatagramSocket socket, InetAddress address, String finalStatString) {
        byte[] bytesData;
        // convert string to bytes para ser recebido como datagrama no servidor
        bytesData = finalStatString.getBytes();
        DatagramPacket statPacket = new DatagramPacket(bytesData, bytesData.length, address, porta);
        try {
            socket.send(statPacket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    private static int getPort() {
        return porta;
    }

    private static void setPort(int port) {
        porta = port;
    }

    private static String getFileName() {
        return fileName;
    }

    private static void setFileName(String file_name) {
        fileName = file_name;
    }

    private static void setDestFile(String dest_file) {
        destFileName = dest_file;
    }

    private static String getDestFileName() {
        return destFileName;
    }

    private static String getHostname() {
        return hostname;
    }

    private static void setHostname(String host) {
        hostname = host;
    }

}
