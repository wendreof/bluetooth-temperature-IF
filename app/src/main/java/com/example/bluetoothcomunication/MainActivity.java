package com.example.bluetoothcomunication;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
	
	private static final String endereco_MAC_do_Bluetooth_remoto = "00:21:13:00:EE:AE";
	private static final int CODIGO_PARA_ATIVACAO_BLUETOOTH = 1;
	private static final UUID MEU_UUID = UUID.fromString ( "00001101-0000-1000-8000-00805F9B34FB" );
	Button conectar;
	Button desconectar;
	Button receberDadosTemperatura;
	Button receberDadosUmidade;
	EditText medicoesRecebidas;
	// representa um dispositivo bluetooth remoto
	private BluetoothDevice dispositivoBluetoothRemoto;
	//representa o adaptador Bluetooth do dispositivo local
	/*
	 * O BluetoothAdapter permite executar tarefas fundamentais do Bluetooth,
	 * como iniciar a descoberta de dispositivos,
	 * consultar uma lista de dispositivos ligados (emparelhados),
	 * instanciar um dispositivo Bluetooth usando um endereço MAC conhecido
	 * e criar um BluetoothServerSocket para ouvir solicitações de conexão
	 * de outros dispositivos e iniciar uma procura de dispositivos Bluetooth
	 * */
	private BluetoothAdapter meuBluetoothAdapter = null;
	//um soquete bluetooth conectado ou conectando
    /*A interface para soquetes Bluetooth é semelhante à dos soquetes TCP: Socket e ServerSocket.
    No lado do servidor, use um BluetoothServerSocket para criar um soquete
    de servidor de escuta.
    Quando uma conexão é aceita pelo BluetoothServerSocket,
    ele retorna um novo BluetoothSocket para gerenciar a conexão.
    No lado do cliente, use um único BluetoothSocket para iniciar
    uma conexão de saída e gerenciar a conexão.
    */
	private BluetoothSocket bluetoothSocket = null;
	/*
	 * Essa classe abstrata é a superclasse de todas as classes que representam
	 * um fluxo de entrada de bytes.
	 * Os aplicativos que precisam definir uma subclasse de InputStream
	 * sempre devem fornecer um método que retorne o próximo byte de entrada.*/
	private InputStream inputStream = null;
	
	/*
	 * A classe OutputStream abstrata é a superclasse de todas as
	 * classes que representam um fluxo de saída de bytes*/
	private OutputStream outStream = null;
	
	@Override
	protected void onCreate ( Bundle savedInstanceState ) {
		super.onCreate ( savedInstanceState );
		setContentView ( R.layout.activity_main );
		fazerConexoesDoLayout_e_Listeners ( );
		verificarCondicoesDoBlueTooth ( );
	}
	
	public void fazerConexoesDoLayout_e_Listeners ( ) {
		conectar = ( Button ) findViewById ( R.id.conectar );
		desconectar = ( Button ) findViewById ( R.id.desconectar );
		receberDadosTemperatura = ( Button ) findViewById ( R.id.btnMedirTemperatura );
		receberDadosUmidade = ( Button ) findViewById ( R.id.btnMedirUmidade );
		medicoesRecebidas = ( EditText ) findViewById ( R.id.edtTxtResultadoMedicao );
		
		//eventos associados ao respectivos botões
		conectar.setOnClickListener ( new Conectar ( ) );
		desconectar.setOnClickListener ( new Desconectar ( ) );
		receberDadosTemperatura.setOnClickListener ( new ReceberDados ( ) );
		receberDadosUmidade.setOnClickListener ( new ReceberDados ( ) );
	} // fim do método fazerConexoesDoLayout_e_Listeners
	
	public void verificarCondicoesDoBlueTooth ( ) {
		
		//identifique o adaptador Bluetooth local padrão
		meuBluetoothAdapter = BluetoothAdapter.getDefaultAdapter ( );
		
		// verifica se o celular tem bluetooth
		if ( meuBluetoothAdapter == null ) {
			Toast.makeText (
					getApplicationContext ( ),
					"Dispositivo não possui adaptador Bluetooth",
					Toast.LENGTH_LONG ).show ( );
			// finaliza a aplicação
			finish ( );
		} else {
			// verifica se o bluetooth está desligado. Se sim, pede permissão para ligar
			if ( !meuBluetoothAdapter.isEnabled ( ) ) {
				Intent novoIntent = new Intent ( BluetoothAdapter.ACTION_REQUEST_ENABLE );
				startActivityForResult ( novoIntent, CODIGO_PARA_ATIVACAO_BLUETOOTH );
			}
		}// fim do else
	} // fim do método verificarCondicoesDoBlueTooth
	
	// método sobrescrito que irá ser chamado após o clique da mensagem
	@Override
	protected void onActivityResult ( int requestCode, int resultCode, Intent data ) {
		super.onActivityResult ( requestCode, resultCode, data );
		
		switch ( requestCode ) {
			case CODIGO_PARA_ATIVACAO_BLUETOOTH:
				if ( resultCode == Activity.RESULT_OK ) {
					Toast.makeText ( getApplicationContext ( ), "Bluetooth foi ativado",
							Toast.LENGTH_LONG ).show ( );
				} else {
					Toast.makeText ( getApplicationContext ( ), "Bluetooth não foi ativado",
							Toast.LENGTH_LONG ).show ( );
				}
				break;
		}
	} // fim do método onActivityResult
	
	public class Conectar implements View.OnClickListener {
		
		// método sobrescrito da interface View.OnClickListener
		@Override
		public void onClick ( View v ) {
			
			//Valide um endereço Bluetooth, como "00: 43: A8: 23: 10: F0"
			// (os caracteres alfabéticos devem estar em maiúsculas para serem válidos)
			if ( BluetoothAdapter.checkBluetoothAddress ( endereco_MAC_do_Bluetooth_remoto ) ) {
				// atribui o valor do endereço de MAC para a variável dispositivoBluetoothRemoto
				dispositivoBluetoothRemoto =
						meuBluetoothAdapter.getRemoteDevice ( endereco_MAC_do_Bluetooth_remoto );
			} else {
				// exibe uma mensagem de erro
				Toast.makeText (
						getApplicationContext ( ),
						"Endereço MAC do dispositivo Bluetooth remoto não é válido",
						Toast.LENGTH_SHORT ).show ( );
			}
			
			try {
				// atribui o código UUID a variável bluetoothSocket
				bluetoothSocket =
						dispositivoBluetoothRemoto.createInsecureRfcommSocketToServiceRecord ( MEU_UUID );
				bluetoothSocket.connect ( ); // estabelece a conexão
				medicoesRecebidas.setText ( "" );
				Toast.makeText ( getApplicationContext ( ),
						"Conectado", Toast.LENGTH_SHORT ).show ( );
			} catch ( IOException e ) {
				Log.e ( "ERRO AO CONECTAR", "O erro foi" + e.getMessage ( ) );
				Toast.makeText ( getApplicationContext ( ),
						"Conexão não foi estabelecida", Toast.LENGTH_SHORT ).show ( );
			}
		}
	} // fim da classe Conectar
	
	public class Desconectar implements View.OnClickListener {
		
		// método sobrecarregado da interface View.OnClickListener
		@Override
		public void onClick ( View v ) {
			medicoesRecebidas.setText ( "" );
			if ( bluetoothSocket != null ) {
				try {
					// Fecha imediatamente o soquete e libera todos os recursos associados.
					bluetoothSocket.close ( ); // encerra a conexão
					bluetoothSocket = null;
					// exibe uma mensagem
					Toast.makeText ( getApplicationContext ( ),
							"Conexão encerrada", Toast.LENGTH_SHORT ).show ( );
				} catch ( IOException e ) {
					Log.e ( "ERRO AO DESCONECTAR", "O erro foi" + e.getMessage ( ) );
					Toast.makeText (
							getApplicationContext ( ),
							"Erro - A conexão permanece estabelecida",
							Toast.LENGTH_SHORT ).show ( );
				}
			} else {
				Toast.makeText (
						getApplicationContext ( ),
						"Não há nenhuma conexão estabelecida a ser desconectada",
						Toast.LENGTH_SHORT ).show ( );
			}
		} // fim do onClick
	} // fim da classe Desconectar
	
	
	public class ReceberDados implements View.OnClickListener {
		
		// método responsável em enviar os dados
		private void sendData ( String message ) {
			// atribui um array de bytes à variável msgBuffer
			byte[] msgBuffer = message.getBytes ( );
			
			try {
				outStream.write ( msgBuffer );
			} catch ( IOException e ) {
				Toast.makeText ( getApplicationContext ( ), "Erro - Ao enviar dados",
						Toast.LENGTH_SHORT ).show ( );
			}
		} // fim do metodo sendData
		
		// método sobrescrito da interface OnClickListener
		// evento do botão
		@Override
		public void onClick ( View v ) {
			// Verifica se há conexão estabelecida com o Bluetooth.
			if ( bluetoothSocket != null ) {
				medicoesRecebidas.setText ( "" ); // limpa a caixa de texto
				// passa o id do botão acionado
				switch ( v.getId ( ) ) {
					case R.id.btnMedirTemperatura:
						try {
							//Envia Temperatura através do Socket
							outStream = bluetoothSocket.getOutputStream ( );
							sendData ( "t" );
							SystemClock.sleep ( 1000 );
							// Obtenha o fluxo de entrada associado a este soquete.
							inputStream = bluetoothSocket.getInputStream ( );
							
							// Lê bytes deste fluxo e os armazena num array de bytes
							byte[] msgBuffer = new byte[ 1024 ];
							inputStream.read ( msgBuffer );
							// exibe o valor na caixa de texto
							Toast.makeText ( getApplicationContext ( ), msgBuffer.toString ( ), Toast.LENGTH_LONG ).show ( );
							medicoesRecebidas.setText ( new String ( msgBuffer ) );
							
						} catch ( IOException e ) {
							Log.e ( "ERROR", "O erro foi" + e.getMessage ( ) );
							Toast.makeText ( getApplicationContext ( ),
									"Mensagem não recebida", Toast.LENGTH_LONG ).show ( );
						}
						break;
					
					case R.id.btnMedirUmidade:
						try {
							//Envia umidade atravé do Socket
							outStream = bluetoothSocket.getOutputStream ( );
							sendData ( "u" );
							SystemClock.sleep ( 1000 );
							// Obtenha o fluxo de entrada associado a este soquete.
							inputStream = bluetoothSocket.getInputStream ( );
							
							// Lê bytes deste fluxo e os armazena num array de bytes
							byte[] msgBuffer = new byte[ 1024 ];
							inputStream.read ( msgBuffer );
							
							medicoesRecebidas.setText ( new String ( msgBuffer ) );
							
						} catch ( IOException e ) {
							Log.e ( "ERROR", "O erro foi" + e.getMessage ( ) );
							Toast.makeText ( getApplicationContext ( ),
									"Mensagem não recebida", Toast.LENGTH_LONG ).show ( );
						}
						break;
					default:
						Toast.makeText ( getApplicationContext ( ),
								"Botão de medição indisponível", Toast.LENGTH_LONG ).show ( );
						break;
				}
			} else {
				Toast.makeText ( getApplicationContext ( ),
						"Bluetooth não está conectado", Toast.LENGTH_LONG ).show ( );
			}
		} // fim do método sobrescrito onClick
	} // fim da classe ReceberDados
} // fim da classe MainActivity
