package com.kb2fty7.exampleapp_loadimageforwallofvk_1;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import java.net.UnknownHostException;
import java.net.UnknownServiceException;
import java.util.ArrayList;
import java.util.Collection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.kb2fty7.exampleapp_loadimageforwallofvk_1.LoginActivity.VkontakteWebViewClient;
import com.perm.kate.api.Api;
import com.perm.kate.api.Photo;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class StartActivity extends Activity {

	private final int REQUEST_LOGIN = 0001;
	private final int REQUEST_GALLERY = 0002;

	Button authorize;
	Button selectImage;
	Button logout;

	Account account = new Account();
	Api api;
	String pathImage;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_start);
		setupUI();

		account.restore(this);
		if (account.access_token != null) {
			api = new Api(account.access_token,
					getString(R.string.id_application));
		}
		showButtons();
	}

	private void setupUI() {
		authorize = (Button) findViewById(R.id.authorize);
		authorize.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startLoginActivity();
			}
		});
		selectImage = (Button) findViewById(R.id.selectImageWidthGallery);
		selectImage.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				getPathImage();
			}
		});
		logout = (Button) findViewById(R.id.logout);
		logout.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				logOut();
			}
		});
	}

	private void logOut() {
		api = null;
		account.access_token = null;
		account.user_id = 0;
		account.save(StartActivity.this);
		showButtons();
	}

	private void startLoginActivity() {
		Intent intent = new Intent();
		intent.setClass(this, LoginActivity.class);
		startActivityForResult(intent, REQUEST_LOGIN);
	}

	private void getPathImage() {
		Intent intent = new Intent();
		intent.setType("image/*");
		intent.setAction(Intent.ACTION_GET_CONTENT);
		startActivityForResult(Intent.createChooser(intent, "Select Picture"),
				REQUEST_GALLERY);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_LOGIN) {
			if (resultCode == RESULT_OK) {
				Toast.makeText(getApplicationContext(),
						getString(R.string.text_successful_authorize),
						Toast.LENGTH_SHORT).show();
				account.access_token = data.getStringExtra("token");
				account.user_id = data.getLongExtra("user_id", 0);
				account.save(StartActivity.this);
				api = new Api(account.access_token,
						getString(R.string.id_application));
				showButtons();
			}
		}
		if (requestCode == REQUEST_GALLERY) {
			if (resultCode == RESULT_OK) {
				Uri selectedImage = data.getData();
				String[] filePathColumn = { MediaStore.Images.Media.DATA };

				Cursor cursor = getContentResolver().query(selectedImage,
						filePathColumn, null, null, null);
				cursor.moveToFirst();

				int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
				pathImage = cursor.getString(columnIndex);
				cursor.close();
				postImageToWall();
			}
		}
	}

	private void postImageToWall() {
		new Thread() {
			@Override
			public void run() {
				try {
					String loadServer = api.photosGetWallUploadServer(
							account.user_id, null);
					File file = new File(pathImage);
					Bundle params = new Bundle();
					try {
						byte[] data = getBytesFromFile(file);
						params.putByteArray("photo", data);
					} catch (IOException e) {
						e.printStackTrace();
					}

					JSONObject jsonObj = new JSONObject(openUrl(loadServer,
							"POST", params));
					String photo = jsonObj.getString("photo");
					String server = jsonObj.getString("server");
					String hash = jsonObj.getString("hash");
					api.saveWallPhoto(server, photo, hash, account.user_id,
							null);

					ArrayList<Photo> photoS;
					photoS = api.saveWallPhoto(server, photo, hash, null, null);
					String userID = null;
					String ownID = null;
					for (int i = 0; i < photoS.size(); i++) {
						userID = String.valueOf(photoS.get(i).pid);
						ownID = photoS.get(i).owner_id;
					}
					final String pid = userID;
					final String owner_id = ownID;
					Collection<String> attachments = new ArrayList<String>() {
						{
							add("photo" + owner_id + "_" + pid);
						}
					};
					attachments.add("photo_" + userID);
					api.createWallPost(account.user_id,
							getString(R.string.app_name), attachments, null,
							false, false, false, null, null, null, null);
					runOnUiThread(successRunnable);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}.start();
	}

	public static String openUrl(String url, String method, Bundle params)
			throws MalformedURLException, IOException, JSONException {
		String boundary = "Asrf456BGe4h";
		String endLine = "\r\n";
		String twoHyphens = "--";
		OutputStream os;
		HttpURLConnection connection = (HttpURLConnection) new URL(url)
				.openConnection();
		if (!method.equals("GET")) {
			Bundle dataparams = new Bundle();
			for (String key : params.keySet()) {
				Object parameter = params.get(key);
				if (parameter instanceof byte[]) {
					dataparams.putByteArray(key, (byte[]) parameter);
				}
			}
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type",
					"multipart/form-data; boundary=" + boundary);
			connection.setRequestProperty("Connection", "keep-alive");
			connection.setDoOutput(true);
			connection.setDoInput(true);
			connection.connect();
			os = new BufferedOutputStream(connection.getOutputStream());
			os.write((twoHyphens + boundary + endLine).getBytes());
			if (!dataparams.isEmpty()) {
				for (String key : dataparams.keySet()) {
					os.write(("Content-Disposition: form-data; name=\"photo\"; filename=\"filename\"")
							.getBytes());
					os.write(("Content-Type: image/jpeg" + endLine + endLine)
							.getBytes());
					os.write(dataparams.getByteArray(key));
					os.write((endLine + twoHyphens + boundary + twoHyphens + endLine)
							.getBytes());
				}
			}
			os.flush();
			os.close();
		}
		String response = read(connection.getInputStream());
		return response;
	}

	private static String read(InputStream in) throws IOException {
		StringBuilder sb = new StringBuilder();
		BufferedReader r = new BufferedReader(new InputStreamReader(in), 1000);
		for (String line = r.readLine(); line != null; line = r.readLine()) {
			sb.append(line);
		}
		in.close();
		return sb.toString();
	}

	Runnable successRunnable = new Runnable() {
		@Override
		public void run() {
			Toast.makeText(getApplicationContext(), "Запись успешно добавлена",
					Toast.LENGTH_LONG).show();
		}
	};

	/*
	 * private void createPostRequestVk(String loadServer, File file){
	 * StringBuffer requestBody = new StringBuffer(); try{
	 * 
	 * URLConnection connection = null; URL url = new URL(loadServer);
	 * connection = url.openConnection(); HttpURLConnection httpConnection =
	 * (HttpURLConnection) connection; httpConnection.setRequestMethod("POST");
	 * httpConnection.setUseCaches(false); httpConnection.setDoOutput(true);
	 * httpConnection.setDoInput(true); requestBody.append("'photo':");
	 * httpConnection.setRequestProperty("Content-Length",
	 * String.valueOf(requestBody.toString().length()+file.length()));
	 * httpConnection.setRequestProperty("Content-Type", "application/photo");
	 * httpConnection.connect(); DataOutputStream dataOS = new DataOutputStream(
	 * httpConnection.getOutputStream());
	 * dataOS.writeBytes(requestBody.toString());
	 * dataOS.write(getBytesFromFile(file)); dataOS.flush(); dataOS.close();
	 * InputStream in = httpConnection.getInputStream(); InputStreamReader isr =
	 * new InputStreamReader(in, "UTF-8"); StringBuffer data = new
	 * StringBuffer(); int c; while ((c = isr.read()) != -1){ data.append((char)
	 * c); } api.createWallPost(account.user_id,data.toString(), null, null,
	 * false, false, false, null, null, null, null); requestBody = null; }
	 * catch(Exception e){
	 * 
	 * }
	 * 
	 * }
	 */

	public byte[] getBytesFromFile(File file) throws IOException {
		InputStream is = new FileInputStream(file);
		long length = file.length();
		if (length > Integer.MAX_VALUE) {
			return null;
		}
		byte[] bytes = new byte[(int) length];
		int offset = 0;
		int numRead = 0;
		while (offset < bytes.length
				&& (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
			offset += numRead;
		}
		if (offset < bytes.length) {
			throw new IOException("Could not completely read file "
					+ file.getName());
		}
		is.close();
		return bytes;
	}

	void showButtons() {
		if (api != null) {
			authorize.setVisibility(View.GONE);
			logout.setVisibility(View.VISIBLE);
			selectImage.setVisibility(View.VISIBLE);
		} else {
			authorize.setVisibility(View.VISIBLE);
			logout.setVisibility(View.GONE);
			selectImage.setVisibility(View.GONE);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		return true;
	}

}
