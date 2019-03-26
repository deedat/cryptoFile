package client;

import java.io.*;
import opencard.core.service.*;
import opencard.core.terminal.*;
import opencard.core.util.*;
import opencard.opt.util.*;



public class TheClient {

	private PassThruCardService servClient = null;
	boolean DISPLAY = true;
	boolean loop = true;

	static final byte CLA					= (byte)0x00;
	static final byte P1					= (byte)0x00;
	static final byte P2					= (byte)0x00;
	static final byte UPDATECARDKEY				= (byte)0x14;
	static final byte UNCIPHERFILEBYCARD			= (byte)0x13;
	static final byte CIPHERFILEBYCARD			= (byte)0x12;
	static final byte CIPHERANDUNCIPHERNAMEBYCARD		= (byte)0x11;
	static final byte READFILEFROMCARD			= (byte)0x10;
	static final byte WRITEFILETOCARD			= (byte)0x09;
	static final byte UPDATEWRITEPIN			= (byte)0x08;
	static final byte UPDATEREADPIN				= (byte)0x07;
	static final byte DISPLAYPINSECURITY			= (byte)0x06;
	static final byte DESACTIVATEACTIVATEPINSECURITY	= (byte)0x05;
	static final byte ENTERREADPIN				= (byte)0x04;
	static final byte ENTERWRITEPIN				= (byte)0x03;
	static final byte READNAMEFROMCARD			= (byte)0x02;
	static final byte SENDFILETOCARD			= (byte)0x01;
        private final static byte CLA_TEST				= (byte)0x90;
        private final static byte INS_TESTDES_ECB_NOPAD_ENC       	= (byte)0x28;
        private final static byte INS_TESTDES_ECB_NOPAD_DEC       	= (byte)0x29;
        private final static byte INS_DES_ECB_NOPAD_ENC           	= (byte)0x20;
        private final static byte INS_DES_ECB_NOPAD_DEC           	= (byte)0x21;
        private final static byte P1_EMPTY = (byte)0x00;
        private final static byte P2_EMPTY = (byte)0x00;

	static final String FILENAME               = "students.png";
	static final int DMS               = 128;


	public TheClient() {
		try {
			SmartCard.start();
			System.out.print( "Smartcard inserted?... " ); 

			CardRequest cr = new CardRequest (CardRequest.ANYCARD,null,null); 

			SmartCard sm = SmartCard.waitForCard (cr);

			if (sm != null) {
				System.out.println ("got a SmartCard object!\n");
			} else
				System.out.println( "did not get a SmartCard object!\n" );

			this.initNewCard( sm ); 

			SmartCard.shutdown();

		} catch( Exception e ) {
			System.out.println( "TheClient error: " + e.getMessage() );
		}
		java.lang.System.exit(0) ;
	}

	private ResponseAPDU sendAPDU(CommandAPDU cmd) {
		return sendAPDU(cmd, true);
	}

	private ResponseAPDU sendAPDU( CommandAPDU cmd, boolean display ) {
		ResponseAPDU result = null;
		try {
			result = this.servClient.sendCommandAPDU( cmd );
			if(display)
				displayAPDU(cmd, result);
		} catch( Exception e ) {
			System.out.println( "Exception caught in sendAPDU: " + e.getMessage() );
			java.lang.System.exit( -1 );
		}
		return result;
	}


	/************************************************
	 * *********** BEGINNING OF TOOLS ***************
	 * **********************************************/


	private String apdu2string( APDU apdu ) {
		return removeCR( HexString.hexify( apdu.getBytes() ) );
	}


	public void displayAPDU( APDU apdu ) {
		System.out.println( removeCR( HexString.hexify( apdu.getBytes() ) ) + "\n" );
	}


	public void displayAPDU( CommandAPDU termCmd, ResponseAPDU cardResp ) {
		System.out.println( "--> Term: " + removeCR( HexString.hexify( termCmd.getBytes() ) ) );
		System.out.println( "<-- Card: " + removeCR( HexString.hexify( cardResp.getBytes() ) ) );
	}


	private String removeCR( String string ) {
		return string.replace( '\n', ' ' );
	}


	/******************************************
	 * *********** END OF TOOLS ***************
	 * ****************************************/


	private boolean selectApplet() {
		boolean cardOk = false;
		try {
			CommandAPDU cmd = new CommandAPDU( new byte[] {
				(byte)0x00, (byte)0xA4, (byte)0x04, (byte)0x00, (byte)0x0A,
				    (byte)0xA0, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x62, 
				    (byte)0x03, (byte)0x01, (byte)0x0C, (byte)0x06, (byte)0x01
			} );
			ResponseAPDU resp = this.sendAPDU( cmd );
			if( this.apdu2string( resp ).equals( "90 00" ) )
				cardOk = true;
		} catch(Exception e) {
			System.out.println( "Exception caught in selectApplet: " + e.getMessage() );
			java.lang.System.exit( -1 );
		}
		return cardOk;
	}


	private void initNewCard( SmartCard card ) {
		if( card != null )
			System.out.println( "Smartcard inserted\n" );
		else {
			System.out.println( "Did not get a smartcard" );
			System.exit( -1 );
		}

		System.out.println( "ATR: " + HexString.hexify( card.getCardID().getATR() ) + "\n");


		try {
			this.servClient = (PassThruCardService)card.getCardService( PassThruCardService.class, true );
		} catch( Exception e ) {
			System.out.println( e.getMessage() );
		}

		System.out.println("Applet selecting...");
		if( !this.selectApplet() ) {
			System.out.println( "Wrong card, no applet to select!\n" );
			System.exit( 1 );
			return;
		} else 
			System.out.println( "Applet selected" );

		mainLoop();
	}


	void updateDESKey() {
	}
	
	void dechiffrer() {
		try{
			FileInputStream fr = new FileInputStream("outCipher.bin");
            FileOutputStream fw = new FileOutputStream("outCipher2.png");  
            byte[] buff = new byte[DMS];
 			int len;

            while( (len= fr.read(buff)) == DMS){
                buff = this.cipherGeneric(INS_DES_ECB_NOPAD_DEC, buff);
				fw.write(buff);
			}	
			
            buff = this.cipherGeneric(INS_DES_ECB_NOPAD_DEC, buff, len);
			buff= removepadding(buff);
			fw.write(buff);
		
			fr.close();
			fw.close();
					
        }catch (IOException e){
                    System.out.println(e.getMessage());
         }
	}


        byte[] removepadding(byte[] buff){
		int padSize =buff[buff.length-1];
		System.out.println(padSize);
		byte[] result = new byte[buff.length- padSize ];
		System.arraycopy(buff, 0, result , 0, result.length);
		return result ;
	}
        

	void chiffrer() {
            FileInputStream fr = null;
            try {
               // fr = new FileInputStream("C:/Users/DELL/Desktop/JavaCardSDK-stud/scripts.win32/toCipher.txt");
                fr = new FileInputStream(FILENAME);
				
				byte[] buff = new byte[DMS];
				FileOutputStream fw = new FileOutputStream("outCipher.bin");
               int len;
			  // int i=fr.read(buff);
			    //System.out.println(i);
               while((len = fr.read(buff)) == DMS)
			   {
                    buff = this.cipherGeneric(INS_DES_ECB_NOPAD_ENC, buff);
                    System.out.println(buff.length);
                    try{
                            
                        fw.write(buff);
						
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
				if(len==-1)
				len=0;


			   
				buff=addPadding(buff,len);
				buff=this.cipherGeneric(INS_DES_ECB_NOPAD_ENC,buff);
				fw.write(buff);
				fr.close();
                fw.close();
            }catch (IOException e){
                    e.printStackTrace();
            }
	}
        
        
        
        /*byte[] cipherGeneric(char[] buff, boolean display){
            char[] apdu = new char[buff.length + 5];
            char[] preData = {CLA_TEST, INS_DES_ECB_NOPAD_ENC, P1, P2, (char) buff.length};
            System.arraycopy(preData, (short)0, apdu, (short)0, (short)preData.length);
            System.arraycopy(buff, (short)0, apdu, (short)5, (short)buff.length);
            CommandAPDU cmd = new CommandAPDU( new String(apdu).getBytes() );
            displayAPDU(cmd);
            ResponseAPDU resp = this.sendAPDU(cmd, display);
            return resp.getBytes();
        }*/
        
        byte[] addPadding(byte[] buff,int len){
		    int pad_size=8-len % 8;
            byte[]result = new byte[len+pad_size];
			System.arraycopy(buff,0,result,0,len);
			for (int i=0;i<pad_size;i++)
			{
			 result[len+i]=(byte)pad_size;
			 
			}

            return result;
        }
        
    private byte[] cipherGeneric( byte typeINS, byte[] challenge ) {
        byte[] result = new byte[challenge.length];
        // TO COMPLETE
        byte [] apdu_head = { CLA_TEST, typeINS, P1_EMPTY, P2_EMPTY, (byte) challenge.length} ;
        byte [] apdu_send = new byte [ challenge.length + apdu_head.length+1] ; //1 corresponds a l ajout du LE a la fin

        System.arraycopy(apdu_head, (short)0, apdu_send, (short)0, (short)apdu_head.length);
        System.arraycopy(challenge, (short)0, apdu_send, (short) apdu_head.length, (short)challenge.length);
        apdu_send[apdu_send.length-1]=apdu_send[4];   //4 corresponds a l'offset pour recuperer le LC

        System.out.println("// writeNameToCard //");
        CommandAPDU cmd = new CommandAPDU(apdu_send);
        ResponseAPDU resp;
        this.displayAPDU(cmd);
        resp = this.sendAPDU( cmd, DISPLAY );
        byte[] result_temp=resp.getBytes();
        System.arraycopy(result_temp, (short)0, result, (short)0, (short)result_temp.length-2);    		
        
        return result;
    }




	private byte[] cipherGeneric(byte typeINS, byte[] challenge, int size) {
		byte[] temp = new byte[size];
		System.arraycopy(challenge,0,temp,0,size);
		challenge=temp;
		
		byte[] result = new byte[challenge.length];
		// TO COMPLETE

		CommandAPDU cmd;
		ResponseAPDU resp;

		byte[] cmd_1 = new byte[challenge.length + 6];
		cmd_1[0] = CLA_TEST;
		cmd_1[1] = typeINS;
		cmd_1[2] = P1;
		cmd_1[3] = P2;
		cmd_1[4] = (byte) challenge.length;
		for (int i = 0; i < challenge.length; i++) {
			cmd_1[i + 5] = (byte) challenge[i];
		}
		cmd_1[challenge.length + 5] = (byte) challenge.length;

		cmd = new CommandAPDU(cmd_1);
		resp = this.sendAPDU(cmd, DISPLAY);
		byte[] bytes = resp.getBytes();
		System.arraycopy(bytes, (short) 0, result, (short) 0, (short) challenge.length);
		return result;
	}











	void readNameFromCard() {
		byte[] apdu = {CLA, READNAMEFROMCARD, P1, P2, 0};
		CommandAPDU cmd = new CommandAPDU( apdu );
		ResponseAPDU resp = this.sendAPDU(cmd, true);
		byte[] bytes = resp.getBytes();
		String msg = "";
		for (int i = 0; i < bytes.length - 2; i++)
			msg += new StringBuffer("").append((char) bytes[i]);
		System.out.println(msg);
	}


	/*void writeNameToCard() {
		System.out.print( "Student name : " );
		String name = this.readKeyboard();
		byte[] data = name.getBytes();
		byte[] apdu = new byte[data.length + 5];

		byte[] preData = {CLA, WRITENAMETOCARD, P1, P2, (byte) data.length};

		System.arraycopy(preData, (short)0, apdu, (short)0, (short)preData.length);
		System.arraycopy(data, (short)0, apdu, (short)5, (short)data.length);

		CommandAPDU cmd = new CommandAPDU( apdu );

		this.sendAPDU(cmd, true);
	}*/


	void exit() {
		loop = false;
	}


	void runAction( int choice ) {
		switch( choice ) {
			case 3: updateDESKey(); break;
			case 2: dechiffrer(); break;
			case 1: chiffrer(); break;
			case 0: exit(); break;
			default: System.out.println( "unknown choice!" );
		}
	}


	String readKeyboard() {
		String result = null;

		try {
			BufferedReader input = new BufferedReader( new InputStreamReader( System.in ) );
			result = input.readLine();
		} catch( Exception e ) {}

		return result;
	}


	int readMenuChoice() {
		int result = 0;

		try {
			String choice = readKeyboard();
			result = Integer.parseInt( choice );
		} catch( Exception e ) {}

		System.out.println( "" );

		return result;
	}


	void printMenu() {
		System.out.println( "" );
		System.out.println( "3: Changer la clÃ© / DES dans la carte" );
		System.out.println( "2: Déchiffrer" );
		System.out.println( "1: Chiffrer" );
		System.out.println( "0: exit" );
		System.out.print( "--> " );
	}


	void mainLoop() {
		while( loop ) {
			printMenu();
			int choice = readMenuChoice();
			runAction( choice );
		}
	}


	public static void main( String[] args ) throws InterruptedException {
		new TheClient();
	}


}
