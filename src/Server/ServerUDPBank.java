package Server;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

import Bank.Model.Bank;
import Bank.enumer.ClientSequence;
import Bank.enumer.ServerSequence;
/*
 * Definindo classe do servidor UDP
 * 
 * Como executar: java ServerUdp <PORT>
 * 
 * TODO
 * -- Transferencia entre contas
 * -- Requisição da senha para o usuario
 * -- Generate salt para a codificação md5
 */
public class ServerUDPBank {
	public void saveData(Bank myBank) {
		
	}
	
	public void sendingData(DatagramSocket socket) {
		//TODO
	}
	
	public static void main(String args[]) throws Exception{
		Bank myBank = new Bank();
		
		byte[] inputData = new byte[1024];
		
		if (args.length < 1) {
			throw new IllegalArgumentException("Falta os argumentos do endereço IP e a porta");
		}
		
		int port = Integer.parseInt(args[0]);
		boolean debug = false; 
		if (args.length >= 2) { 
			debug = args[1].equals("-d");	
		}
		
		
		String[] receiveDataComponents;	
		String receiveData;
		InetAddress address;
		int sequenceNumber;
		int clientPort;
		byte[] sendData = new byte[1024];
		
		DatagramSocket socket = new DatagramSocket(port);
		System.out.println("Server na porta " + port);
		//pacote recebido do cliente
		DatagramPacket receivePacket = new DatagramPacket(inputData, inputData.length);	
		socket.setSoTimeout(60*100);
		//Fica ouvindo a rede
		boolean flagAck = false;				
		while(!flagAck) {
			try {
				sequenceNumber = -1;
				socket.receive(receivePacket);
				receiveDataComponents = new String(receivePacket.getData(), 0, 
						receivePacket.getLength()).split(":");
				sequenceNumber = Integer.parseInt(receiveDataComponents[0]);
				receiveData = receiveDataComponents[1];

				if(sequenceNumber !=  -1) {
					flagAck = true;					
				}
			} catch (SocketTimeoutException e) {				
				continue;
			}
		}
		while(true) {
			receiveData = new String(receivePacket.getData(), 0, receivePacket.getLength());
			//Capturando o endereço ip do cliente(emissor)
			address = receivePacket.getAddress();
			//E a porta usada pelo cliente
			clientPort = receivePacket.getPort();
			
			receiveDataComponents = receiveData.split(":");
			sequenceNumber = Integer.parseInt(receiveDataComponents[0]);
			receiveData = receiveDataComponents[1];
			
			//Criando pacote para responder as requisições do usuario
			DatagramPacket sendPacket = new DatagramPacket(sendData, 
					sendData.length, address, clientPort);
			/*
			 * Se a conexão não estiver ocupada, e tiver recebido o pacote, começa a operação
			 */
			
			
			if (sequenceNumber == ClientSequence.REQUEST_CONNECTION.ordinal()) {
				//Mensagem de confirmação de conexão estabelecida
				sendData = (ServerSequence.RESPONSE_CONNECTION.ordinal() + ":ACK").getBytes();
				sendPacket.setData(sendData);
				sendPacket.setLength(sendData.length);
				socket.send(sendPacket);
				if (debug) {
					System.out.println("IP: " + address + "\nPort: " + clientPort);
				}							
			}else if (sequenceNumber == ClientSequence.SEND_USERNAME_PASSWORD.ordinal()) {
				String username = receiveDataComponents[1];
				String password = receiveDataComponents[2];
				//Verificando se a senha foi aceita
				boolean accessOK = password.equals(myBank.getPassword(username));
				if(accessOK) {
					sendData = (ServerSequence.LOGIN_RESULT.ordinal() + ":" 
								+ "LOGIN_OK" + ":").getBytes();
				}else {
					sendData = (ServerSequence.LOGIN_RESULT.ordinal() + ":"
								+ "LOGIN_FAIL" +":").getBytes();
				}
				sendPacket.setData(sendData);
				sendPacket.setLength(sendData.length);
				socket.send(sendPacket);
			}else if (sequenceNumber == ClientSequence.SEND_COMMAND.ordinal()) {
				String strCommand = receiveDataComponents[1];
				String username = receiveDataComponents[3];
				String result = username + " o saldo inicial é de - " 
								+ myBank.printBalance(myBank.getBalance(username));
				int amount = 0;
				if (receiveDataComponents[2] != null) {
					amount = (int) (Float.parseFloat(receiveDataComponents[2]));
				}
				
				if (debug) {
					System.out.println(strCommand + ":" + amount);
				}
				
				if (strCommand.equals("deposit")) {
					myBank.deposit(username, amount);
					result += " Deposito realizado!! Seu saldo é de - " 
							+ myBank.printBalance(myBank.getBalance(username));
					
				}else if (strCommand.equals("withdraw")) {
					if (myBank.withdraw(username, amount)) {
						result += " Saque realizado!! Seu saldo é de - "
								+ myBank.printBalance(myBank.getBalance(username));
					}else {
						result += " Saldo não realizado!! Saldo indisponivel "
								+ "se saldo atual é de - " +myBank.printBalance(myBank.getBalance(username));
					}
				}else if (strCommand.equals("transfer")) {
					try {						
						String destinyUser = receiveDataComponents[4];
						if (myBank.transfer(username, amount, destinyUser)) {
							result += "Transferência Concluída - <" + username +
									"> --" + amount + "--> - <"+ destinyUser+ ">";
						}else {
							result += "Transferencia Negada";
						}
						
					} catch (ArrayIndexOutOfBoundsException e) {
						result += "Transferencia Negada";
					}
				}
				
				if (debug) {					
					System.out.println("Resultado das transações: " + result);
				}
				
				//Enviar para o usuario o log das suas atividades
				sendData = (ServerSequence.SEND_RESULT.ordinal() + ":"+result).getBytes();
				sendPacket.setData(sendData);
				sendPacket.setLength(sendData.length);
				socket.send(sendPacket);
			}
			//Fica ouvindo a rede
			flagAck = false;				
			while(!flagAck) {
				try {
					sequenceNumber = -1;
					socket.receive(receivePacket);
					receiveDataComponents = new String(receivePacket.getData(), 0, 
							receivePacket.getLength()).split(":");
					sequenceNumber = Integer.parseInt(receiveDataComponents[0]);
					receiveData = receiveDataComponents[1];

					if(sequenceNumber !=  -1) {
						flagAck = true;					
					}
				} catch (SocketTimeoutException e) {
					socket.send(sendPacket);
					continue;
				}
			}
		}
	}
}
