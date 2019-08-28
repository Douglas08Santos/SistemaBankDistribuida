package Server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
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
	private int port;
	private DatagramSocket socket;
	private Bank myBank;
	
	public Bank getMyBank() {
		return myBank;
	}
	
	public void setMyBank(Bank myBank) {
		this.myBank = myBank;
	}
	
	
	public DatagramSocket getSocket() {
		return socket;
	}
	public void setSocket(DatagramSocket socket) {
		this.socket = socket;
	}
	
	public ServerUDPBank(int port) throws IOException {
		try {
			this.port = port;
			this.socket = new DatagramSocket(port);
			this.myBank = new Bank();
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	public int getPort() {
		return port;
	}
	
	public void setPort(int port) {
		this.port = port;
	}
	public void saveData(Bank myBank) {
		
	}
	
	public void sendingData(DatagramSocket socket) {
		//TODO
	}
	private void responseForClient(String[] receiveDataComponents, InetAddress address, boolean debug) throws IOException {
		byte[] data = new byte[1024];
		DatagramPacket sendPacket = new DatagramPacket(data, data.length, address, getPort());
		int flag = Integer.parseInt(receiveDataComponents[0]);
		
		if(flag == ClientSequence.REQUEST_CONNECTION.ordinal()) {
			
			data = (ServerSequence.RESPONSE_CONNECTION.ordinal() + ":ACK").getBytes();
			if (debug) {
				System.out.println("IP: " + address + "\nPort: " + getPort());
			}	
			
									
		}else if (flag == ClientSequence.SEND_USERNAME_PASSWORD.ordinal()) {
			String username = receiveDataComponents[1];
			String password = receiveDataComponents[2];
			//Verificando se a senha foi aceita
			boolean accessOK = password.equals(getMyBank().getPassword(username));
			if(accessOK) {
				data = (ServerSequence.LOGIN_RESULT.ordinal() + ":" 
							+ "LOGIN_OK" + ":").getBytes();
			}else {
				data = (ServerSequence.LOGIN_RESULT.ordinal() + ":"
							+ "LOGIN_FAIL" +":").getBytes();
			}			
		}else if (flag == ClientSequence.SEND_COMMAND.ordinal()) {
			data = responseCommand(receiveDataComponents,debug);
		}
		
		sendPacket.setData(data);
		sendPacket.setLength(data.length);
		getSocket().send(sendPacket);
		System.out.println(sendPacket.getAddress() + "\n" + sendPacket.getPort());

	}
	
	private byte[] responseCommand(String[] receiveDataComponents, boolean debug) {
		byte[] data = new byte[1024];
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
				result += "Transferencia Negada, seu usuario destino";
			}
		}
		
		if (debug) {					
			System.out.println("Resultado das transações: " + result);
		}
		
		data = (ServerSequence.SEND_RESULT.ordinal() + ":"+result).getBytes();
		return data;
	}
	
	
	public void listen(boolean debug) throws IOException {
		boolean flagAck = false;
		byte[] inputData = new byte[1024];
		DatagramPacket receivePacket = new DatagramPacket(inputData, inputData.length);
		String[] receiveDataComponents;
		
		while (true) {
			while(!flagAck) {
				try {
					int sequenceNumber = -1;
					getSocket().receive(receivePacket);
					receiveDataComponents = new String(receivePacket.getData(), 0, 
							receivePacket.getLength()).split(":");
					sequenceNumber = Integer.parseInt(receiveDataComponents[0]);
					
					if(sequenceNumber !=  -1) {
						flagAck = true;					
					}
					
					if (sequenceNumber == ClientSequence.REQUEST_CONNECTION.ordinal()) {
						responseForClient(receiveDataComponents, receivePacket.getAddress(), debug);
					}else if (sequenceNumber == ClientSequence.SEND_USERNAME_PASSWORD.ordinal()) {					
						responseForClient(receiveDataComponents, receivePacket.getAddress(), debug);
					}else if (sequenceNumber == ClientSequence.SEND_COMMAND.ordinal()) {
						responseForClient(receiveDataComponents, receivePacket.getAddress(), debug);
					}
				} catch (SocketTimeoutException e) {				
					e.printStackTrace();
				}
			}
		}
		
		
	}
	
	
	

	public static void main(String args[]) throws Exception{
		if (args.length < 1) {
			throw new IllegalArgumentException("Falta o argumento do numero da porta");
		}
		
		int port = Integer.parseInt(args[0]);
		ServerUDPBank myServer = new ServerUDPBank(port);
		
		boolean debug = false; 
		if (args.length >= 2) { 
			debug = args[1].equals("-d");	
		}
		
		System.out.println("Server na porta " + port);
		myServer.getSocket().setSoTimeout(60*100);
		
		myServer.listen(debug);
		
		
		
		
		
		
	}
}
