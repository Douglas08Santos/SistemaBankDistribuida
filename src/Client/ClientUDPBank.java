package Client;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.security.MessageDigest;
import java.util.Scanner;

import Bank.enumer.ClientSequence;
import Bank.enumer.ServerSequence;


/*
 * Lado cliente da aplicação
 * Como executar: java RemoteBankUdp <IP>: <PORT> <USERNAME> <PASSWORD> 
 * 					<COMMAND> <AMOUNT>
 */

/*
 *Seria melhor o usuario fazer o login em execução
 */
public class ClientUDPBank {
	//Capturando informação passada pelo args
	//Principais deviam ser o Ip, Port(para estabelecer conexão)
	
	public static void main(String[] args) throws Exception {
		Scanner input = new Scanner(System.in);
		//Verifica se foi passado os args necessarios
		/*if(!(args.length >= 1)) {
			throw new IllegalArgumentException("Falta os argumentos do endereço IP e a porta");
		}
		String[] addressAndPort = args[0].split(":");
		//Verifica se o IP e porta foram passados na forma correta
		if(addressAndPort.length != 2) {
			throw new IllegalArgumentException("IP e porta informado de "
					+ "forma errada, tente assim: a.b.c.d:port");
		}
		
		//Verifica se o IP está escrito na forma correta
		String[] stringIp = addressAndPort[0].split(".");
		if(stringIp.length != 4) {
			throw new IllegalArgumentException("IP tem que ser na forma a.b.c.d");
		}*/
		
		String[] stringIp = {"127", "0", "0", "1"};
		
		byte[] ipAddress = {Byte.parseByte(stringIp[0]),
							Byte.parseByte(stringIp[1]),
							Byte.parseByte(stringIp[2]),
							Byte.parseByte(stringIp[3])};
		int port = Integer.parseInt(/*addressAndPort[1]*/ "2019");
		
		//Debug
		System.out.println("Rodando");
				
		//int amount = input.nextInt();
		
				
		String[] receiveDataComponents;
		int sequenceNumber;
		String receiveData = "";
		byte[] sendData;
		
		/*
		 * Definindo socket
		 */
		DatagramSocket clientSocket = new DatagramSocket();
		clientSocket.setSoTimeout(1000);
		
		String request = ClientSequence.REQUEST_CONNECTION.ordinal() 
				+ ":Pedido de Conexão";
		sendData = request.getBytes();
		
		/*
		 * Criando o pacote
		 */
		DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, 
										InetAddress.getByAddress(ipAddress), port);
		
		//Debug
		//System.out.println(username + ": Pedido de autenticação");
		
		byte[] inboundData = new byte[1024];
		
		DatagramPacket receivePacket = new DatagramPacket(inboundData, inboundData.length);
		
		clientSocket.send(sendPacket);
		
		boolean flagAck = false;
		receivePacket.setData(new byte[1024]);
		//Pedido do nome do usuario
		while(!flagAck) {
			try {
				clientSocket.receive(receivePacket);
				receiveDataComponents = new String(receivePacket.getData(), 0, 
						receivePacket.getLength()).split(":");
				sequenceNumber = Integer.parseInt(receiveDataComponents[0]);
				receiveData = receiveDataComponents[1];
				
				if(sequenceNumber == ServerSequence.REQUEST_USERNAME.ordinal()) {
					flagAck = true;
				}
				
			} catch (SocketTimeoutException e) {
				clientSocket.send(sendPacket);
				continue;
			}
		}
		
		String[] ackComponents = new String(receivePacket.getData()).split(":");
		sequenceNumber = Integer.parseInt(ackComponents[0]);
		
		//Usuario informa o seu nome de usuario
		System.out.println("Nome de Usuário(username): ");
		String username = input.nextLine();
		
		//Enviando o nome do usuario ao server
		sendData = (ClientSequence.SEND_USERNAME.ordinal() + ":" + username).getBytes();
		sendPacket.setData(sendData);
		sendPacket.setLength(sendData.length);
		
		// Pedido da senha
		flagAck = false;
		receivePacket.setData(new byte[1024]);
		//Pedido da senha do usuario
		while(!flagAck) {
			try {
				clientSocket.receive(receivePacket);
				receiveDataComponents = new String(receivePacket.getData(), 0, 
						receivePacket.getLength()).split(":");
				sequenceNumber = Integer.parseInt(receiveDataComponents[0]);
				receiveData = receiveDataComponents[1];
				
				if(sequenceNumber == ServerSequence.REQUEST_PASSWORD.ordinal()) {
					flagAck = true;
				}
				
			} catch (SocketTimeoutException e) {
				clientSocket.send(sendPacket);
				continue;
			}
		}
		
		ackComponents = new String(receivePacket.getData()).split(":");
		sequenceNumber = Integer.parseInt(ackComponents[0]);
		
		//codificar senha
		/*
		 * TODO
		 */
		
		//Usuario informa a sua senha
		System.out.println("Senha: ");
		String password = input.nextLine();
		
		
		//enviando a senha
		sendData = (ClientSequence.SEND_PASSWORD.ordinal() + ":" + password).getBytes();
		sendPacket.setLength(sendData.length);
		clientSocket.send(sendPacket);
		
		//esperando resultado da autenticação
		flagAck = false;
		receivePacket.setData(new byte[1024]);
		while(!flagAck) {
			try {
				clientSocket.receive(receivePacket);
				receiveDataComponents = new String(receivePacket.getData(), 0, 
						receivePacket.getLength()).split(":");
				sequenceNumber = Integer.parseInt(receiveDataComponents[0]);
				receiveData = receiveDataComponents[1];
				
				if(sequenceNumber == ServerSequence.LOGIN_RESULT.ordinal()) {
					flagAck = true;
				}
				
			} catch (SocketTimeoutException e) {
				clientSocket.send(sendPacket);
				continue;
			}
		}
		
		boolean accessOk = receiveData.equals("Acesso permitido");
		
		while(accessOk) {
			System.out.println("Operação desejada: \n"
					+ "1 - Desposito\n"
					+ "2 - Saque\n"
					+ "3 - Transferencia\n"
					+ "4 - Sair");
			int opt = input.nextInt();
			System.out.println("Quantia desejada: \n");
			int amount = input.nextInt();
			
			if(opt == 1) {
				sendData = (ClientSequence.SEND_COMMAND.ordinal()
                        + ":"
                        + "deposit"
                        + ":"
                        + amount).getBytes();
			}else if(opt == 2) {
				sendData = (ClientSequence.SEND_COMMAND.ordinal()
                        + ":"
                        + "withdraw"
                        + ":"
                        + amount).getBytes();
			}else if(opt == 3) {
				System.out.println("Digite o nome do usuário de destino: \n");
				String destinyUser = input.nextLine();
				sendData = (ClientSequence.SEND_COMMAND.ordinal()
                        + ":"
                        + "transfer"
                        + ":"
                        + amount
                        + ":"
                        + destinyUser).getBytes();
			}else if(opt == 4) {
				accessOk = false;
			}
			
			// Enviar para o server a operação desejada
            sendPacket.setData(sendData);
            sendPacket.setLength(sendData.length);
            clientSocket.send(sendPacket);
            
            //Aguardando resposta do server sobre a operação requisitada
            flagAck = false;
            receivePacket.setData(new byte[1024]);
            while(!flagAck) {
    			try {
    				clientSocket.receive(receivePacket);
    				receiveDataComponents = new String(receivePacket.getData(), 0, 
    						receivePacket.getLength()).split(":");
    				sequenceNumber = Integer.parseInt(receiveDataComponents[0]);
    				receiveData = receiveDataComponents[1];
    				
    				if(sequenceNumber == ServerSequence.SEND_RESULT.ordinal()) {
    					flagAck = true;
    				}
    				
    			} catch (SocketTimeoutException e) {
    				clientSocket.send(sendPacket);
    				continue;
    			}
    		}
            System.out.println(receiveData);
            
		}
		
		

	}
	

}
