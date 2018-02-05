package org.mbds.android.tagnfc;

import java.io.IOException;
import java.util.Arrays;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

/**
 * Attention, nécessite au moins l'API 17 ou 18
 * enableFourgroundNdef.. déprécié...
 */
public class NFCReader extends AppBaseActivity {

	public static final String TAG = "NFC Creator-Reader";

	public static final int REQUEST_CODE = 1000;
	private PendingIntent mPendingIntent;
	private IntentFilter ndefDetected;
    public final static String MESSAGE = "message";
    private static String message = "";
    private NfcAdapter nfcAdapter = null;
    private NdefMessage msg = null;
    private Menu leMenu = null;
    public final static String DOMAIN = "http://www.mbds-fr.org/";    
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTitle(TAG);
		setContentView(R.layout.nfcreading);
		registerBaseActivityReceiver();
		//Ne pas écraser un message qui n'a pas encore été écrit
		//en attente de détection du tag à encoder...
		if (message.equals("")) {
	        try {
	            Bundle bundle = this.getIntent().getExtras();
	            message = bundle.getString(MESSAGE, "");
	    	} catch (Exception e) {
	            // pas de message :)
	    		// l'activité a été activée suite à la détection 
	    		// d'un périphérique NFC qui contient une URI http://www.mbds-fr.org
	        }
		}
	}
	
	
	public NdefMessage createNdefMessage(String text, String mimeType)
	{
		//Message de type MIME270
		NdefMessage msg = new NdefMessage(
			//NdefRecord.createMime(mimeType, text.getBytes())
			//Autre méthode (qui fait la même chose)...
			//NdefRecord(NdefRecord.TNF_MIME_MEDIA,	mimeType.getBytes(), new byte[0], text.getBytes())
			//Message de type application :
			//permet de lancer l’application qui recevra le tag
			//NdefRecord.createApplicationRecord("com.mbds.android.tagnfc"))
			//Message de type URI
			NdefRecord.createUri(text)
		);
		return msg;
	}
	
	//Méthode invoquée lors de la détection d'un tag/périphérique NFC
	@Override
	public void onNewIntent(Intent intent) {
		//Détection d'un périphérique NFC (tag ou autre)
		String action = intent.getAction();
		if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action) ||
			NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action) ||
			NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {
			resolveIntent(intent) ;
		}
	}
	
	@SuppressLint("NewApi")
	private void resolveIntent(Intent intent) {
			boolean isNdefMsgFound = false;
			boolean isWritable = false;
			Tag tag = null;
			Ndef ndef = null;
			int content = -1;
			String[] technologies = null;
			try {
				 //Infos sur le tag
				tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
				technologies = tag.getTechList();
				content = tag.describeContents();
				ndef = Ndef.get(tag);
				isWritable = ndef.isWritable();
				boolean canMakeReadOnly = ndef.canMakeReadOnly();
				byte[] id =tag.getId();
			} catch (Exception e) {
				// il ne s'agit pas d'un tag.... (périphérique NFC ?)
			}
			
			//Sommes-nous en train d'écrire ou de lire ???
			//message renseigné = écriture de Tag
			if (tag!=null && !message.equals("") && isWritable) {
				Toast.makeText(this, "Enregistrement sur le tag reussi !", 
						Toast.LENGTH_SHORT).show();
				//Ecriture
				writeTag(msg, tag);
		        
				message = "";
				msg=null;
				
		        
				Intent main = new Intent(getBaseContext(), TagActivity.class);
	            startActivity(main);
	            finish();
				// On ne lit pas le contenu...
				return;
			}

			//Lecture
			//Récupération des messages
			Parcelable[] rawMsgs = 
		            intent.getParcelableArrayExtra(
		                NfcAdapter.EXTRA_NDEF_MESSAGES);
			NdefMessage[] msgs;
			//Les enregistrements peuvent être imbriqués, 
			//mais ce n'est pas notre utilisation
			String receivedMessages = "";
			if (rawMsgs != null) {
				try {
					msgs = new NdefMessage[rawMsgs.length];
						for (int i = 0; i < rawMsgs.length; i++) {
							msgs[i] = (NdefMessage) rawMsgs[i];
							NdefRecord record = msgs[i].getRecords()[i];
							//Infos sur le tag...
							//byte[] idRec = "".getBytes();
							//short tnf = 0;
							byte[] type = "".getBytes();
							try {
								//idRec = record.getId();
								//tnf = record.getTnf();
								type = record.getType();
							} catch (Exception e) {
								// Les infos du tag sont incomplètes
							}
							//Message contenu sur le tag sous forme d'URI
							if (Arrays.equals(type, NdefRecord.RTD_SMART_POSTER) ||
								Arrays.equals(type, NdefRecord.RTD_URI) ||
								Arrays.equals(type, NdefRecord.RTD_TEXT)) {
								Uri uri = record.toUri();
								receivedMessages += uri.toString().replace(DOMAIN,"")+" ";
							}
							isNdefMsgFound = true;
						}
				} catch (Exception e) {
					// Le contenu du tag est mal formé
					Toast.makeText(this, "NDEF type not managed!..", 
							Toast.LENGTH_SHORT).show();
				}
			}
			if (isNdefMsgFound) {
				//Affichage dans l'activité TagActivity
				Bundle bundle = new Bundle();
	            bundle.putString(MESSAGE, receivedMessages);
	            Intent main = new Intent(getBaseContext(), TagActivity.class);
	            main.putExtras(bundle);
	            startActivity(main);
	  			finish();
			}
			//Sinon, attente de détection du tag à écrire...
		}
	@Override
	public void onResume() {
		super.onResume();
        if (!message.equals("")) {
            //Créer le message à écrire
            NfcAdapter nfcAdapter =
    		NfcAdapter.getDefaultAdapter(getApplicationContext());
    		//Utilisation de la méthode crée précédemment :
            //On insère le domaine (URL) pour que le tag soit détecté 
            //par cette appli en priorité (cf. manifeste)=> dans notre 
            //exemple, nous n'utiliserons pas le type mime...
    		msg = createNdefMessage(DOMAIN+message, "text/plain");
    		//Passer le message Ndef, ainsi que l’activité en cours à l’adaptateur :
    		//Si un périphérique NFC est en proximité, le message sera envoyé en mode passif
    		//(ne fonctionne pas pour un tag passif)
    		nfcAdapter.setNdefPushMessage(msg, this);      
        }

        if (nfcAdapter==null)
        	nfcAdapter = NfcAdapter.getDefaultAdapter(this);
		mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this,
				getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
		// Intent filters 
		ndefDetected = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
		Intent intent = getIntent();
		//Lecture/Ecriture...
		resolveIntent(intent);
	}

	public static boolean writeTag(final NdefMessage msg, final Tag	tag) {
		try {
			int size = msg.toByteArray().length;
			Ndef ndef = Ndef.get(tag);
			if (ndef != null) {
				ndef.connect();
				if (!ndef.isWritable()) {
					return false;
				}
				if (ndef.getMaxSize() < size) {
					return false;
				}
				ndef.writeNdefMessage(msg);
				ndef.close();
				return true;
			} else {
				//Tags qui nécessitent un formatage :
				NdefFormatable format =	NdefFormatable.get(tag);
				if (format != null) {
					//Inspiré de la source à cette URL : 
					//http://www.jessechen.net/blog/how-tonfc-on-the-android-platform
					try {
						format.connect();
						//Formatage et écriture du message:
						format.format(msg);
						//ou en verrouillant le tag en écriture :
						//formatable.formatReadOnly(message);
						format.close() ;
						return true;
					} catch (IOException e) {
						return false;
					}
				} else {
					return false;
				}
			}
		} catch (Exception e) {
			return false;
		}
	}
	
	@Override
	public void onPause() {
		super.onPause();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		new MenuInflater(getApplication()).inflate(R.menu.activity_second, menu);
		leMenu = menu;
		return (super.onCreateOptionsMenu(menu));
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
	        //if the Chocolate item is selected 
	        case R.id.retour:
	        	AlertDialog.Builder builder = new AlertDialog.Builder(
						NFCReader.this);
				builder.setTitle("Retour")
						.setMessage(
						"Voulez-vous vraiment annuler l'ecriture du message ?")
						.setCancelable(false)
						.setPositiveButton("Oui",
								new DialogInterface.OnClickListener() {
									public void onClick(
											DialogInterface dialog, int id) {
										message = "";
										msg=null;
										Toast.makeText(NFCReader.this, "Annulation. Le message " + message + " n'a pas ete ecrit.", 
												Toast.LENGTH_SHORT).show();
							            finish();
									}
								})
						.setNegativeButton("Non", null);
				AlertDialog alert = builder.create();
				alert.show();
				return true;
	        default:
	        	return false;
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			setResult(RESULT_CANCELED);
			finish();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
	
	protected void onDestroy() {
		super.onDestroy();
		unRegisterBaseActivityReceiver();
	}
}
