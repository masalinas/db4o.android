package com.thingtrack.db4o.android;

import com.thingtrack.db4o.domain.User;

import android.app.Dialog;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.os.Bundle;

public class UserActivity extends Dialog implements OnClickListener {
	private EditText edtUsername;
	private EditText edtPassword;
	
	private Button btnOk;
	private Button btnRemove;
	private Button btnCancel;
	
	private UserActivityListener listener;
	private User user;
	
	public interface UserActivityListener {
		public void onOkClick(User user);
		public void onRemoveClick(User user);
		public void onCancelClick();
	}
	
	public UserActivity(Context context, User user, UserActivityListener userActivityListener) {
		super(context);
		Log.d("db4o.android", "User Dialog");
		setContentView(R.layout.user_view);
	
		this.user = user;
		this.listener = userActivityListener;
	}

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);		
		Log.d("db40.android", "User Dialog");
		setContentView(R.layout.user_view);
				
		edtUsername = (EditText)findViewById(R.id.edt_username);
		edtPassword = (EditText)findViewById(R.id.edt_password);
		edtUsername.setText(user.getUsername());
		edtPassword.setText(user.getPassword());
		
		btnOk = (Button)findViewById(R.id.btn_ok);
		btnRemove = (Button)findViewById(R.id.btn_remove);
		btnCancel = (Button)findViewById(R.id.btn_cancel);
		
		btnOk.setOnClickListener(this);
		btnRemove.setOnClickListener(this);
		btnCancel.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.btn_ok:
				user.setUsername(edtUsername.getText().toString());
				user.setPassword(edtPassword.getText().toString());
				
				listener.onOkClick(user); 
				dismiss();
			break;
			case R.id.btn_remove:
				listener.onRemoveClick(user); 
				dismiss();
			break;
			case R.id.btn_cancel:
				cancel();
			break;
		}
		
	}

}
