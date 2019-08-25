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
	public static void main(String args[]) throws Exception{
		Bank myBank = new Bank();
		
		byte[] inputData = new byte[1024];
		//byte[] outputData = new byte[1024];
		
		int port = Integer.parseInt(/*args[0]*/"2019");
		
		String[] receiveDataComponents;	
		String receiveData;
		InetAddress address;
		int sequenceNumber;
		int clientPort;
		byte[] sendData;
		
		DatagramSocket socket = new DatagramSocket(port);
		System.out.println("Cliente na porta " + port);
		
		while(true) {
			
			//Autenticação - criando pacote
			DatagramPacket receivePacket = new DatagramPacket(inputData, inputData.length);
			
			socket.receive(receivePacket);
			
			//Definindo timeOut para os pacotes
			socket.setSoTimeout(1500);
			
			receiveData = new String(receivePacket.getData(), 0, receivePacket.getLength());
			//Capturando o endereço ip do cliente(emissor)
			address = receivePacket.getAddress();
			//E a porta usada pelo cliente
			clientPort = receivePacket.getPort();
			
			receiveDataComponents = receiveData.split(":");
			sequenceNumber = Integer.parseInt(receiveDataComponents[0]);
			receiveData = receiveDataComponents[1];
			
			
			boolean isRequest = (sequenceNumber == ClientSequence.REQUEST_CONNECTION.ordinal());
			
			/*
			 * Se a conexão não estiver ocupada, e tiver recebido o pacote, começa a operação
			 */
			if (isRequest) {
				//Criando o pacote ACK
				sendData = (ServerSequence.REQUEST_USERNAME.ordinal() + ":ACK").getBytes();//requisição do nome do usuario
				DatagramPacket sendPacket = new DatagramPacket(sendData, 
						sendData.length, address, clientPort);
				
				//debug
				//System.out.println("IP: " + address + "\nPort: " + clientPort)
				
				
				socket.send(sendPacket);
				
				//Experando a requisição do nome do usuário
				boolean flagAck = false;
				receivePacket.setData(new byte[1024]);
				while(!flagAck) {
					try {
						socket.receive(receivePacket);
						receiveDataComponents = new String(receivePacket.getData(), 0, 
								receivePacket.getLength()).split(":");
						sequenceNumber = Integer.parseInt(receiveDataComponents[0]);
						receiveData = receiveDataComponents[1];
						
						if(receivePacket.getPort() == clientPort && 
								address.equals(receivePacket.getAddress()) &&
								sequenceNumber == ClientSequence.SEND_USERNAME.ordinal()) {
							
							flagAck = true;
						}
						
					} catch (SocketTimeoutException e) {
						socket.send(sendPacket);
						continue;
					}
				}
				
				//Agora temos o nome do usuario
				sequenceNumber = Integer.parseInt(receiveDataComponents[0]);
				String username = receiveData;
				
				//Debug
				//System.out.println("username" + username);
				
				//Solicitação de senha
				
				sendData = (ServerSequence.REQUEST_PASSWORD.ordinal() + ":ACK").getBytes();//requisição da senha do usuario
				sendPacket = new DatagramPacket(sendData, 
						sendData.length, address, clientPort);
				
				//debug
				//System.out.println("IP: " + address + "\nPort: " + clientPort)
				
				
				socket.send(sendPacket);
				
				flagAck = false;
				receivePacket.setData(new byte[1024]);
				while(!flagAck) {
					try {
						socket.receive(receivePacket);
						receiveDataComponents = new String(receivePacket.getData(), 0, 
								receivePacket.getLength()).split(":");
						sequenceNumber = Integer.parseInt(receiveDataComponents[0]);
						receiveData = receiveDataComponents[1];
						
						if(receivePacket.getPort() == clientPort && 
								address.equals(receivePacket.getAddress()) &&
								sequenceNumber == ClientSequence.SEND_PASSWORD.ordinal()) {
							
							flagAck = true;
						}
						
					} catch (SocketTimeoutException e) {
						socket.send(sendPacket);
						continue;
					}
				}
				
				//Agora temos a senha do usuario(TODO: usar md5)
				sequenceNumber = Integer.parseInt(receiveDataComponents[0]);
				String password = receiveData;
				
				//Verificando se a senha foi aceita
				boolean accessOK = password.equals(myBank.getPassword(username));
				if(accessOK) {
					sendData = (ServerSequence.LOGIN_RESULT.ordinal() + ":" 
								+ "Acesso permitido").getBytes();
				}else {
					sendData = (ServerSequence.LOGIN_RESULT.ordinal() + ":"
								+ "Acesso negado").getBytes();
				}
				sendPacket.setData(sendData);
				sendPacket.setLength(sendData.length);
				socket.send(sendPacket);
				
				//Se o login foi aceito o cliente está autorizado a realizar as operações
				if(accessOK) {
					//Operações do Banco
					flagAck = false;
					receivePacket.setData(new byte[1024]);
					while(!flagAck) {
						try {
							socket.receive(receivePacket);
							receiveDataComponents = new String(receivePacket.getData(), 0, 
									receivePacket.getLength()).split(":");
							sequenceNumber = Integer.parseInt(receiveDataComponents[0]);
							receiveData = receiveDataComponents[1];
							
							if(receivePacket.getPort() == clientPort && 
									address.equals(receivePacket.getAddress()) &&
									sequenceNumber == ClientSequence.SEND_COMMAND.ordinal()) {
								
								flagAck = true;
							}
							
						} catch (SocketTimeoutException e) {
							socket.send(sendPacket);
							continue;
						}
					}
					
					String command = receiveDataComponents[1];
					int amount = (int) (Float.parseFloat(receiveDataComponents[2]) *100);
					//int balance = myBank.getBalance(username);
					
					String result = username + ", balanço: " + myBank.printBalance(myBank.getBalance(username)) + ".";
					
					if(command.equals("deposit")) {
						myBank.deposit(username, amount);
						result += "realizado deposito, o balanço é de: " + 
									myBank.printBalance(myBank.getBalance(username)) + ".";
					}else if (command.equals("withdraw")) {
						if(myBank.withdraw(username, amount)) {
							result += "realizado saque, o balanço é de: " + 
									myBank.printBalance(myBank.getBalance(username)) + ".";
						}else {
							result += "saque não realizado, saldo indisponivel";
						}
					}else if(command.equals("transfer")) {
						if(receiveDataComponents[3] != null) {
							String destinyUser = receiveDataComponents[3];
							if(myBank.transfer(username, amount, destinyUser)) {
								result += "Transferência Concluída: <" + username +
										"> --" + amount + "--><"+ destinyUser+ ">";
							}else {
								result += "Transferencia Negada";
							}
						}
					}
					
					//Debug
					//System.out.println("Resultado das transações: " + result);
					
					//Enviar para o usuario o log das suas atividades
					sendData = (ServerSequence.SEND_RESULT.ordinal() + ":"+result).getBytes();
					sendPacket.setData(sendData);
					sendPacket.setLength(sendData.length);
					socket.send(sendPacket);
				}
				
				
				//Fechando a conexão
				flagAck = false;
				receivePacket.setData(new byte[1024]);
				while(!flagAck) {
					try {
						socket.receive(receivePacket);
						receiveDataComponents = new String(receivePacket.getData(), 0, 
								receivePacket.getLength()).split(":");
						sequenceNumber = Integer.parseInt(receiveDataComponents[0]);
						receiveData = receiveDataComponents[1];
						
						if(receivePacket.getPort() == clientPort && 
								address.equals(receivePacket.getAddress()) &&
								sequenceNumber == ClientSequence.FIN.ordinal()) {
							
							flagAck = true;
						}
						
					} catch (SocketTimeoutException e) {
						
						socket.send(sendPacket);
						continue;
					}
				}
				
				//Debug
				//System.out.println("Recebendo comando para encerrar a conexão");
				
				//Enviando um pacote de fim de conexão
				sendData = (ServerSequence.FIN.ordinal() + ":"+ "FIN_ACK").getBytes();
				sendPacket.setData(sendData);
				sendPacket.setLength(sendData.length);
				socket.send(sendPacket); 
				
				//Debug
				//System.out.println("Enviando FIN");
				
				//Esperando o pacote de FIN_ACK
				flagAck = false;
				receivePacket.setData(new byte[1024]);
				while(!flagAck) {
					try {
						socket.receive(receivePacket);
						receiveDataComponents = new String(receivePacket.getData(), 0, 
								receivePacket.getLength()).split(":");
						sequenceNumber = Integer.parseInt(receiveDataComponents[0]);
						receiveData = receiveDataComponents[1];
						
						if(receivePacket.getPort() == clientPort && 
								address.equals(receivePacket.getAddress()) &&
								sequenceNumber == ClientSequence.FIN_ACK.ordinal()) {
							
							flagAck = true;
						}
						
					} catch (SocketTimeoutException e) {
						
						socket.send(sendPacket);
						continue;
					}
				}
				//Debug
				//System.out.println("Enviando FIN_ACK");
				
			}
			
			socket.setSoTimeout(0);
			
			
			
			
			
			
		}
		
		
		
	}
}
