package Bank.Model;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * Esta classe encapsula os metodos para os registros bancarios como
 * nome do usuario, senha, e saldo
 */
public class Bank {
	//Basico...
	//Lista com os usuarios
	List<String> users;
	//Dicionario para guardar as senhas
	Map<String, String> pass;
	//Dicionario para guardas os saldo dos clientes
	Map<String, Integer> balances;
	
	//Construtor do Banco, instancia seus atributos
	public Bank() throws IOException {
		users = new ArrayList<String>();
		pass = new HashMap<String, String>();
		balances = new HashMap<String, Integer>();
		
		//Leitura de arquivo para o cadastro dos clientes
		FileReader input = new FileReader("/home/douglasantos/eclipse-workspace/SistemaBankDistribuido/registros.txt");
		BufferedReader buffer = new BufferedReader(input);
		String line = "";
		
		//Pular a mensagem inicial
		while(!(line = buffer.readLine()).equals("***********"));
		
		//Ler os usuarios listados no arquivo
		while((line = buffer.readLine())!= null) {
			//usamos o '--' com separador
			String[] userInfo = line.split(" -- ");
			String username = userInfo[0];
			String password = userInfo[1];
			
			//Adicinando usuario na lista de usuarios
			users.add(username);
			//Adicionando a senha do usuario
			pass.put(username, password);
			//Adicionando seu saldo
			
			//Conferindo se foi passado o valor do saldo
			int balance;
			if (userInfo.length == 3) {
				balance = Integer.parseInt(userInfo[2]);
			}else {
				balance = 0;
			}
			
			balances.put(username, balance);		
		}
		
	}
	
	/*
	 * Dado o nome do usuario de origem e de destino, se realiza a transferencia
	 */
	public boolean transfer(String originUser, int amount, String destinyUser) {
		int curBalanceOrigin = balances.get(originUser);
		int curBalanceDestiny = balances.get(destinyUser);
		
		if(amount > curBalanceOrigin) {
			return false;
		}
		
		//Atualizando saldos
		int newBalanceOrigin = curBalanceOrigin - amount;
		int newBalanceDestiny = curBalanceDestiny - amount;
		
		balances.put(originUser, newBalanceOrigin);
		balances.put(destinyUser, newBalanceDestiny);
		return true;
	}
	
	/*
	 * Dado o nome do usuario e o valor a ser retirado, a função realiza o saque
	 */
	public boolean withdraw(String username, int amount) {
		int currentBalance = balances.get(username);
		if(amount > currentBalance) {
			return false;
		}
		
		// Saldo atualizado
		int newBalance = currentBalance - amount;
		balances.put(username, newBalance);
		return true;
	}
	
	/*
	 *Dado o nome do usuario eh depositado a quantia na saldo do usuario 
	 */
	public int deposit(String username, int amount) {
		//Saldo atual
		int currentBalance = balances.get(username);
		// Novo saldo
		int newBalance = currentBalance + amount;
		//Atualizar saldo
		balances.put(username, newBalance);
		
		return newBalance;
		
	}
	
	/*
	 * imprimir valor do saldo em '$ xx.xx
	 */
	public String printBalance(int balance) {
		String result =  String.valueOf(balance);		
		return "$" + result;
	}
	
	/*
	 * Saldo do cliente
	 */
	public int getBalance(String username) {
		return balances.get(username);
	}
	
	/*
	 * Pegar senha do usuario
	 */
	public String getPassword(String username) {
		return pass.get(username);
	}
	
	/*
	 * Checar se um usuario está cadastrado no Banco
	 */
	public boolean checkUser(String username) {
		return users.contains(username);
	}
	
	
	
	
	
}
