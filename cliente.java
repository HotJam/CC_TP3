import java.io.*;
import java.net.*;
import java.util.Random;
import java.util.Scanner;

class Cliente {

  private static String username = "";
  private static String password = "";
  private static int porta = 0;
  private static String hostname = "";
  private static String fileName = "";
  private static Scanner input = new Scanner(System.in);
  private static Socket s = null;
  
  public static void main(String[] args) throws IOException{

    System.out.println("username=");
    username = input.nextLine();
    System.out.println("password=");
    password = input.nextLine();


    if(username.equals("servercctp3") && password.equals("grupo64")){
      System.out.println("Autenticação efetuada com sucesso \n > A enviar pacote..");

      porta = Integer.parseInt(args[0]);
      hostname = args[1];
      fileName = args[2];

      DatagramSocket socket = new DatagramSocket();
      InetAddress address = InetAddress.getByName(hostname);

      byte[] saveDataFile = fileName.getBytes();
      DatagramPacket filePacket = new DatagramPacket(saveDataFile, saveDataFile.length, address, porta);

      File ficheiro = new File(fileName);
      int fileLenght = (int) ficheiro.length();
      byte[] ficheiroByteArray = new byte[fileLenght];

      //enviar ficheiro

      socket.close();
    }
  }

}
