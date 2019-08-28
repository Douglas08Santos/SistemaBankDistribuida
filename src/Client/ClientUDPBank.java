package Client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Scanner;



import Bank.enumer.ClientSequence;
import Bank.enumer.ServerSequence;


/*
 * Lado cliente da aplicação
 * Como executar: java RemoteBankUdp <IP>: <PORT> <USERNAME> <PASSWORD> 
 * 					<COMMAND> <AMOUNT>
 */
public class ClientUDPBank {
	private DatagramSocket clientSocket;
	private byte[] ipAddress;
	private int port;
	
	/**
	 * @return the clientSocket
	 */
	public DatagramSocket getClientSocket() {
		return clientSocket;
	}
	/**
	 * @param clientSocket the clientSocket to set
	 */
	public void setClientSocket(DatagramSocket clientSocket) {
		this.clientSocket = clientSocket;
	}
	/**
	 * @return the ipAddress
	 */
	public byte[] getIpAddress() {
		return ipAddress;
	}
	/**
	 * @param ipAddress the ipAddress to set
	 */
	public void setIpAddress(byte[] ipAddress) {
		this.ipAddress = ipAddress;
	}
	/**
	 * @return the port
	 */
	public int getPort() {
		return port;
	}
	/**
	 * @param port the port to set
	 */
	public void setPort(int port) {
		this.port = port;
	}
	
	
	public ClientUDPBank(byte[] ipAddress, int port) {
		try {
			this.clientSocket = new DatagramSocket();
			clientSocket.setSoTimeout(1000);
			this.ipAddress = ipAddress;
			this.port = port;
			
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			 
	}
	/*
	 * Envia ao servidor uma requisição de conexão
	 */
	public void requestConnection() throws IOException {
		byte[] data = new byte[1024];
		String request = ClientSequence.REQUEST_CONNECTION.ordinal() 
				+ ":Pedido de Conexão";
		data = request.getBytes();
		
		try {
			DatagramPacket sendPacket = new DatagramPacket(data, data.length, 
					InetAddress.getByAddress(getIpAddress()), getPort());
			getClientSocket().send(sendPacket);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		
		
		
	}
	//Função que espera a resposta do servidor
	public boolean response(int serverFlag) throws IOException {
		byte[] inboundData = new byte[1024];
		DatagramPacket receivePacket = new DatagramPacket(inboundData, inboundData.length);
		String[] receiveDataComponents = null;
		int sequenceNumber = 0;
		boolean flagAck = false;
		receivePacket.setData(new byte[1024]);
		boolean result = false;
		String msg = null;
			
		while(!flagAck) {
			try {
				getClientSocket().receive(receivePacket);
				receiveDataComponents = new String(receivePacket.getData(), 0, 
						receivePacket.getLength()).split(":");
				sequenceNumber = Integer.parseInt(receiveDataComponents[0]);
				if(sequenceNumber == serverFlag) {
					flagAck = true;
					if (serverFlag == ServerSequence.LOGIN_RESULT.ordinal()) {
						result = receiveDataComponents[1].equals("LOGIN_OK");;
					}else if (serverFlag == ServerSequence.SEND_RESULT.ordinal()) {
						msg = receiveDataComponents[1];
						System.out.println(msg);
						
					}
					result = true;			
					
				}

			} catch (SocketTimeoutException e) {
				requestConnection();
				continue;
			}
		}
		
		return result;
	}

	/*
	 * Envia o nome de usuario e senha para o servidor
	 * TODO: falta colocar o md5
	 */
	public void sendUserAndPassword(String username, String password) throws IOException {
		byte[] data = new byte[1024];
		//Autenticação ao server
		data = (ClientSequence.SEND_USERNAME_PASSWORD.ordinal() + ":" + username + ":" + password + ":").getBytes();
		DatagramPacket sendPacket;
		try {
			sendPacket = new DatagramPacket(data, data.length, 
					InetAddress.getByAddress(getIpAddress()), getPort());
			getClientSocket().send(sendPacket);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void operationbank(String username, int opt) throws IOException {
		Scanner input = new Scanner(System.in);
		byte[] data = new byte[1024];
		if(opt == 1) {
			System.out.println(opt + "Quantia desejada: ");
			int amount = input.nextInt();
			data = (ClientSequence.SEND_COMMAND.ordinal()
					+ ":"
					+ "deposit"
					+ ":"
					+ amount
					+ ":"
					+ username).getBytes();
		}else if(opt == 2) {
			System.out.println(opt + "Quantia desejada: ");
			int amount = input.nextInt();
			data = (ClientSequence.SEND_COMMAND.ordinal()
					+ ":"
					+ "withdraw"
					+ ":"
					+ amount
					+ ":"
					+ username).getBytes();
		}else if(opt == 3) {
			System.out.println(opt + "Quantia desejada: ");
			int amount = input.nextInt();
			System.out.println("Digite o nome do usuário de destino: ");
			String destinyUser = input.nextLine();
			data = (ClientSequence.SEND_COMMAND.ordinal()
					+ ":"
					+ "transfer"
					+ ":"
					+ amount
					+ ":"
					+ username
					+ ":"
					+ destinyUser
					+ ":").getBytes();
		}
		DatagramPacket sendPacket;
		try {
			sendPacket = new DatagramPacket(data, data.length, 
					InetAddress.getByAddress(getIpAddress()), getPort());
			getClientSocket().send(sendPacket);	
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
			
		
		
	}
	public static void main(String[] args) throws Exception {
		//Verifica se foi passado os args necessarios
		if(!(args.length >= 1)) {
			throw new IllegalArgumentException("Falta os argumentos do endereço IP e a porta");
		}
		String[] addressAndPort = args[0].split(":");

		//Verifica se o IP e porta foram passados na forma correta
		if(addressAndPort.length != 2) {
			throw new IllegalArgumentException("IP e porta informado de "
					+ "forma errada, tente assim: a.b.c.d:port");
		}

		//Verifica se o IP está escrito na forma correta
		String[] stringIp = addressAndPort[0].split("\\.");

		if(stringIp.length != 4) {
			throw new IllegalArgumentException("IP tem que ser na forma a.b.c.d");
		}

		byte[] ipAddress = {Byte.parseByte(stringIp[0]),
				Byte.parseByte(stringIp[1]),
				Byte.parseByte(stringIp[2]),
				Byte.parseByte(stringIp[3])};
		int port = Integer.parseInt(addressAndPort[1]);
		
		/*
		 * Se o codigo será rodado na forma de debug
		 */
		boolean debug = false;
		if (args.length >= 2) { 
			debug = args[1].equals("-d");	
		}

		if (debug) {
			System.out.println("Rodando - IP: " + addressAndPort[0] +"\nPort: " + addressAndPort[1]);
		}	
		
		
		ClientUDPBank myCliente = new ClientUDPBank(ipAddress, port);
		myCliente.requestConnection();
		
		byte[] inboundData = new byte[1024];
		DatagramPacket receivePacket = new DatagramPacket(inboundData, inboundData.length);
		String[] receiveDataComponents = null;
		int sequenceNumber = 0;
		boolean connection = false;
		boolean flagAck = false;
		String receiveData = null;
		//Confirmação de conexão
		while(!flagAck) {
			try {
				myCliente.getClientSocket().receive(receivePacket);
				receiveDataComponents = new String(receivePacket.getData(), 0, 
						receivePacket.getLength()).split(":");
				sequenceNumber = Integer.parseInt(receiveDataComponents[0]);
				receiveData = receiveDataComponents[1];
				if(sequenceNumber == ServerSequence.RESPONSE_CONNECTION.ordinal()) {
					flagAck = true;
					connection = true;
				}
			} catch (SocketTimeoutException e) {
				myCliente.requestConnection();
				continue;
			}
		}
		
		 

		if (connection) {			
			if (debug) {
				System.out.println("Servidor respondeu!!!");
			}
			Scanner input = new Scanner(System.in);
			//Usuario informa o seu nome de usuario
			System.out.println("Nome de Usuário(username): ");
			String username = input.nextLine();

			//Usuario informa a sua senha
			System.out.println("Senha: ");
			String password = input.nextLine();

			myCliente.sendUserAndPassword(username, password);
			boolean accessOk = myCliente.response(ServerSequence.LOGIN_RESULT.ordinal());			 
			System.out.println(accessOk);			

			while(accessOk) {
				System.out.println("Operação desejada: \n"
						+ "1 - Desposito\n"
						+ "2 - Saque\n"
						+ "3 - Transferencia\n"
						+ "4 - Sair");	
				int opt = input.nextInt();
				
				if(opt == 4) {
					accessOk = false;				
				}else {
					myCliente.operationbank(username, opt);
					myCliente.response(ServerSequence.SEND_RESULT.ordinal());
				}
				
							
				

			}
		}
	}

}	



