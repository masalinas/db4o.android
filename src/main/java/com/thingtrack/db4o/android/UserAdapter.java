package com.thingtrack.db4o.android;

import java.util.List;

import com.thingtrack.db4o.domain.User;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

public class UserAdapter extends ArrayAdapter<User> {
	 private int resource;	 
	    
	public UserAdapter(Context context, int resource, List<User> items) {
		super(context, resource, items);
	
		this.resource=resource;
	}

	 @Override
	 public View getView(int position, View convertView, ViewGroup parent) {
		 LinearLayout userView = null;
		 
		 //Get the current alert object
         User user = getItem(position);
 
         //Inflate the view
         if(convertView == null) {
        	userView = new LinearLayout(getContext());
            String inflater = Context.LAYOUT_INFLATER_SERVICE;
            
            LayoutInflater vi = (LayoutInflater)getContext().getSystemService(inflater);
            vi.inflate(resource, userView, true);
         }
         else {
        	 userView = (LinearLayout) convertView;
         }
         
         //Get the text boxes from the listitem.xml file
         TextView username =(TextView)userView.findViewById(R.id.txtUsername);
         TextView password =(TextView)userView.findViewById(R.id.txtPassword);
 
         //Assign the appropriate data from our alert object above
         username.setText(user.getUsername());
         password.setText(user.getPassword());
	        
		 return userView;
	 }
}
