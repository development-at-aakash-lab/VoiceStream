package com.iitb.voicestream;

import static android.media.MediaRecorder.AudioSource.MIC;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;


public class VideoStream extends Activity {

	private boolean isRecording=false;
	private Button      recordButton;
	public  EditText    ipAddressField;
	public  EditText    portField;
	public  AudioRecord recorder;
	public  ProgressBar progressBar;
	private int port          =50005;
	private int sampleRate    =44100;
	private int channelConfig =AudioFormat.CHANNEL_IN_MONO;
	private int encodingFormat=AudioFormat.ENCODING_PCM_16BIT;
	int minBufSize=AudioRecord.getMinBufferSize(sampleRate,channelConfig,encodingFormat)+4096;
	int bufferSize=0;
	public String ipAddress;
	private static final String IPV4_NUM       ="([01]?\\d\\d?|2[0-4]\\d|25[0-5])";
	private static final String IP_DOT         ="\\.";
	private static final String IPV4_PATTERN   ="^"+IPV4_NUM+IP_DOT+IPV4_NUM+IP_DOT+IPV4_NUM+IP_DOT+IPV4_NUM+"$";
	public static final  String PERMISSION_TEXT="You may start talking";

	@Override
	protected
	void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		// NetworkOnMainThreadException Solution is these two lines
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy); 
		
		
		recordButton=(Button)findViewById(R.id.bRecord);
		recordButton.setText("Start");
		ipAddressField=(EditText)findViewById(R.id.etIpAddress);
		portField=(EditText)findViewById(R.id.etPort);
		progressBar=(ProgressBar)findViewById(R.id.progressBar);
	}

	public
	void stopStreaming(){
		recordButton.setEnabled(false);
		recordButton.setText("");
		recorder.stop();
		recorder.release();
		recorder=null;
		progressBar.setIndeterminate(false);
		recordButton.setText("Start");
		recordButton.setEnabled(true);
	}

	public
	void startStreaming(){
		ipAddress=getIpAddress();
		port=getPort();
		if(!isValidIPAddressAndPort(ipAddress,port)) return;
		recordButton.setEnabled(false);
		recordButton.setText("");
		new Thread(new Runnable(){
			@Override
			public
			void run(){
				try{
					DatagramSocket socket=new DatagramSocket();
					byte[] buffer=new byte[minBufSize];
					DatagramPacket packet;
					final InetAddress destination=InetAddress.getByName(ipAddress);
					recorder=new AudioRecord(MIC,sampleRate,channelConfig,encodingFormat,minBufSize*10);
					recorder.startRecording();
					runOnUiThread(new Runnable(){
						@Override
						public
						void run(){
							recordButton.setText("Stop");
							recordButton.setEnabled(true);
							progressBar.setIndeterminate(true);
						}
					});
					while(isRecording){
						bufferSize=recorder.read(buffer,0,buffer.length);
						packet=new DatagramPacket(buffer,buffer.length,destination,port);
						socket.send(packet);
					}
				}
				catch(UnknownHostException e){
					//Log.e("VS","UnknownHostException");
				}
				catch(SocketException e){
					e.printStackTrace();
				//	Log.e("VS","IOException");
				}
				catch(IOException e){
					e.printStackTrace();
					//Log.e("VS","IOException");
				}
			}
		}).start();
	}

	@SuppressWarnings("ConstantConditions")
	private
	String getIpAddress(){
		return ipAddressField.getText().toString();
	}

	@SuppressLint("NewApi")
	private
	int getPort(){
		@SuppressWarnings("ConstantConditions") String portText=portField.getText().toString();
		return Integer.valueOf(portText.isEmpty()?"0":portText);
	}

	private
	boolean isValidIPAddressAndPort(String ipAddress,int port){
		if(!Pattern.compile(IPV4_PATTERN).matcher(ipAddress).matches()){
			Toast.makeText(this,"IP Address is invalid",Toast.LENGTH_SHORT).show();
			return isRecording=false;
		}
		if(port<49152||port>65535){
			Toast.makeText(this,"Allowed Range for port is 49152 to 65535",Toast.LENGTH_LONG).show();
			return isRecording=false;
		}
		return true;
	}

	public
	void onRecordPress(View v){
		isRecording=!isRecording;
		if(isRecording) startStreaming();
		else stopStreaming();
	}

	/*@Override
	public
	boolean onCreateOptionsMenu(Menu menu){
		getMenuInflater().inflate(R.menu.main,menu);
		return true;
	}*/

	@Override
	public
	boolean onOptionsItemSelected(MenuItem item){
		int id=item.getItemId();
		return id==R.id.action_settings||super.onOptionsItemSelected(item);
	}

	public
	void onRequestPress(View view){
		final byte[] request=("Raise Hand").getBytes();
		new Thread(new Runnable(){
			@Override
			public
			void run(){
				try{
					port=getPort();
					System.out.println("b4 intet");
					final InetAddress destination=InetAddress.getByName(getIpAddress());
					System.out.println("after intet");
					new DatagramSocket().send(new DatagramPacket(request,request.length,destination,port));
					System.out.println("b4 waiting");
					 runOnUiThread(new Runnable() {  
		                    @Override
		                    public void run() {
		                        // TODO Auto-generated method stub

		                    	try {
									while(waitingForPermission()) ;
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
		                    }
		                });
					
					System.out.println("after waiting");
				}
				catch(SocketException e){
					e.printStackTrace();
				}
				catch(UnknownHostException e){
					e.printStackTrace();
				}
				catch(IOException e){
					e.printStackTrace();
				}
			}
		}).start();
	}

	private
	boolean waitingForPermission() throws IOException{
		byte[] receiveData=new byte[8192];
		DatagramSocket socket=new DatagramSocket(port);
		System.out.println("after waiting2");

		DatagramPacket receivePacket=new DatagramPacket(receiveData,receiveData.length);
		System.out.println("after waiting3");

		socket.receive(receivePacket);

		final String requestText=new String(receivePacket.getData());
	//	Log.d("requestText",requestText);
		if(requestText.contains(PERMISSION_TEXT)){
			isRecording=true;
			startStreaming();
			System.out.println("after waiting4");

			return false;
		}
		return true;
	}

	public
	void onDefaultIP(View view){ipAddressField.setText("10.105.22.26");}

	public
	void onDefaultPort(View view){portField.setText("50005");}

	public
	void onWithdrawPress(View view){
		final byte[] request=("Withdraw").getBytes();
		new Thread(new Runnable(){
			@Override
			public
			void run(){
				try{
					port=getPort();
					final InetAddress destination=InetAddress.getByName(getIpAddress());
					System.out.println("wd 1");
					new DatagramSocket().send(new DatagramPacket(request,request.length,destination,port));
					System.out.println("wd 2");
					isRecording=false;
					
					runOnUiThread(new Runnable(){
						@Override
						public void run(){
							stopStreaming();
						}
					});
					
					System.out.println("wd 3");
				}
				catch(SocketException e){
					e.printStackTrace();
				}
				catch(UnknownHostException e){
					e.printStackTrace();
				}
				catch(IOException e){
					e.printStackTrace();
				}
			}
		}).start();
	}
}
