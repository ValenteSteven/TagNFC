package org.mbds.android.tagnfc;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

public class TagActivity extends AppBaseActivity {
	public static final String TAG = "NFC Creator-Reader";
	private EditText editText;
	private Button btnShare;
	private Button btnClear;
	private CheckBox notification;
	private Menu leMenu = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setTitle(TAG);
		setContentView(R.layout.activity_main);
		registerBaseActivityReceiver();
		btnShare = (Button) findViewById(R.id.buttonShare);
		btnClear = (Button) findViewById(R.id.buttonClear);
		editText = (EditText) findViewById(R.id.textViewTagNFC);
		if (editText.getText().toString().equals("")) {
			btnClear.setEnabled(false);
			btnClear.setTextColor(Color.GRAY);
		}
		editText.addTextChangedListener(new TextWatcher() {
			public void afterTextChanged(Editable s) {
				if (!editText.getText().toString().equals("")) {
					btnClear.setEnabled(true);
					btnClear.setTextColor(Color.WHITE);
					if (editText.getError() != null) {
						editText.setError(null);
					}
				} else {
					btnClear.setEnabled(false);
					btnClear.setTextColor(Color.GRAY);
				}
			}

			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}

			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
			}
		});

		btnShare.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				final String message = ((EditText) findViewById(R.id.textViewTagNFC))
						.getText().toString();
				if (message != null & !message.trim().isEmpty()) {
					AlertDialog.Builder builder = new AlertDialog.Builder(
							TagActivity.this);
					builder.setMessage(
							"Voulez-vous encoder ce message sur le tag NFC ?")
							.setCancelable(false)
							.setPositiveButton("OK",
									new DialogInterface.OnClickListener() {
										public void onClick(
												DialogInterface dialog, int id) {
											Bundle bundle = new Bundle();
											bundle.putString(NFCReader.MESSAGE,
													message);
											notification = (CheckBox) findViewById(R.id.notifier);
											if (notification.isChecked()) {
												// Si l'API le permet,
												if (android.os.Build.VERSION.SDK_INT >= 11) {
													// Creation d'une
													// notification si
													// l'utilisateur l'a
													// souhaite.
													creerNotification(message);
												}
												// Sinon,
												else {
													Toast.makeText(
															getApplicationContext(),
															"La notification n'a pas pu aboutir (necessite API > 10).",
															Toast.LENGTH_LONG)
															.show();
												}
											}
											Intent nfcReader = new Intent(
													getBaseContext(),
													NFCReader.class);
											nfcReader.putExtras(bundle);
											startActivity(nfcReader);
										}
									})
							.setNegativeButton("Annuler",
									new DialogInterface.OnClickListener() {
										public void onClick(
												DialogInterface arg0, int id) {
										}

									});
					AlertDialog alert = builder.create();
					alert.show();
				} else {
					editText.setError("Vous devez indiquer un message dans la zone de texte !");
				}

				// Récupérer le texte saisi par l'utilisateur

			}

		});
		btnClear.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				AlertDialog.Builder builder = new AlertDialog.Builder(
						TagActivity.this);
				builder.setMessage("Voulez-vous vraiment effacer ce message ?")
						.setCancelable(false)
						.setPositiveButton("OK",
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int id) {
										EditText txt = (EditText) findViewById(R.id.textViewTagNFC);
										txt.setText("");
										txt.invalidate();
									}
								})
						.setNegativeButton("Annuler",
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface arg0,
											int id) {
									}

								});
				AlertDialog alert = builder.create();
				alert.show();
			}

		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		new MenuInflater(getApplication()).inflate(R.menu.tag, menu);
		leMenu = menu;
		return (super.onCreateOptionsMenu(menu));
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		// if the Chocolate item is selected
		case R.id.apropos:
			// Creation et ouverture d'une boite de dialogue comportant les
			// infos de base du logiciel.
			AlertDialog.Builder alerteAPropos = new AlertDialog.Builder(
					TagActivity.this);
			alerteAPropos.setTitle("A propos");
			alerteAPropos
					.setMessage("NFC Creator-Reader. \n\nVersion v2.0. \n\nRealise en collaboration par CHAABANE Mohamed, ERISEY Nicolas & LE HALPER Nicolas. \n\nCopyright (c) 2013. Tous droits reserves. ");
			// Retour au menu principal.
			alerteAPropos.setPositiveButton("Retour", null);
			alerteAPropos.show();
			return true;
		case R.id.quitter:
			AlertDialog.Builder builder = new AlertDialog.Builder(
					TagActivity.this);
			builder.setTitle("Quitter")
					.setMessage("Quitter l'application ?")
					.setCancelable(false)
					.setPositiveButton("Oui",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									TagActivity.this.closeAllActivities();
								}
							}).setNegativeButton("Non", null);
			AlertDialog alert = builder.create();
			alert.show();
			return true;
		default:
			return false;
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		// initialisation
		String message = "";

		try {
			Bundle bundle = this.getIntent().getExtras();
			message = bundle.getString(NFCReader.MESSAGE, "");
			if (!message.equals("")) {
				((EditText) findViewById(R.id.textViewTagNFC)).setText(message);
				Toast.makeText(this,
						"Le message " + message + " a bien ete lu !",
						Toast.LENGTH_SHORT).show();
				btnClear = (Button) findViewById(R.id.buttonClear);
				btnClear.setEnabled(true);
				btnClear.setTextColor(Color.WHITE);
			} else {
			}
		} catch (Exception e) {
			// Pas de message à afficher
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

	/**
	 * Cree une nouvelle notification
	 * 
	 * @param corps
	 *            corps de la note lie a la notification
	 */
	@SuppressLint("NewApi")
	private final void creerNotification(String corps) {
		// Instantiation de notre "manager" de notifs.
		final NotificationManager mNotification = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		// Creation de la nouvelle notification a partir des donnees de notre
		// appli.
		Notification notification = new Notification(R.drawable.ic_launcher,
				"NFC Creator/Reader", System.currentTimeMillis());
		final Intent launchNotifiactionIntent = new Intent(this,
				TagActivity.class);
		final PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
				launchNotifiactionIntent, PendingIntent.FLAG_ONE_SHOT);

		// Insertion des donnees que contiendra notre notif.
		notification.setLatestEventInfo(this,
				"Nouvelle ecriture de tag en cours !", corps, pendingIntent);

		// Notification sur le portable.
		mNotification.notify(1, notification);
	}
}
