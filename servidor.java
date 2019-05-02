import java.io.*;
import java.net.*;

public class servidor {

  //usr = serverCCTP3 pass = grupo64
  private static int totalTransferred = 0;
  private static String fileName = "";
  private int id=0;
  private HashMap<String, String> utilizadores; //<username, password>

  public static void main(String[] args) throws IOException{
    int porta = Integer.parseInt(args[0]);

    //instancia socket para atribuir conexão
    DatagramSocket socketData = new DatagramSocket(porta);


    while(true){

      //instancia do pacote a ser enviado
      byte[] fileNamePacket = new byte[1024];
      DatagramPacket filePacket = new DatagramPacket(fileNamePacket, fileNamePacket.length);

      //
      File file = new File(fileName);
      FileOutputStream outToFile = new FileOutputStream(file);
      System.out.println("FICHEIRO: " + fileName);

      socketData.receive(filePacket);
      System.out.println("Ficheiro recebido com sucesso!");




      socketData.close();
    }
  }

}
