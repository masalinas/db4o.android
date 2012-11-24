package com.thingtrack.db4o.android;

import java.util.List;

import com.db4o.Db4oEmbedded;
import com.db4o.ObjectContainer;
import com.db4o.ObjectSet;
import com.db4o.config.ConfigScope;
import com.db4o.config.EmbeddedConfiguration;
import com.db4o.cs.Db4oClientServer;
import com.db4o.drs.ObjectState;
import com.db4o.drs.Replication;
import com.db4o.drs.ReplicationEvent;
import com.db4o.drs.ReplicationEventListener;
import com.db4o.drs.ReplicationSession;
import com.db4o.drs.db4o.Db4oClientServerReplicationProvider;
import com.db4o.drs.db4o.Db4oEmbeddedReplicationProvider;

import com.thingtrack.db4o.domain.User;
import com.thingtrack.db4o.android.UserActivity.UserActivityListener;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;

public class HelloAndroidActivity extends Activity implements ServerInfo {

    private static String TAG = "db4o.android";
    
    private Button btnRefresh;
    private Button btnAddUser;
    private Button btnReplicateUser;
    
    private ListView lvUsers;
    
    private final static String LOCAL_SERVER = "konekti.db4o";
    
    private ObjectContainer remoteServer = null;
    private ObjectContainer localServer = null;
    
    private Db4oClientServerReplicationProvider providerA = null;
    private Db4oEmbeddedReplicationProvider providerB = null;
    
    private UserAdapter userAdapter;
    
    /**
     * Called when the activity is first created.
     * @param savedInstanceState If the activity is being re-initialized after 
     * previously being shut down then this Bundle contains the data it most 
     * recently supplied in onSaveInstanceState(Bundle). <b>Note: Otherwise it is null.</b>
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		Log.i(TAG, "onCreate");
        setContentView(R.layout.main);
        
        // replicate on start up
        openLocalServer(getBaseContext());
                
        lvUsers= (ListView) findViewById(R.id.lv_users);
        
        btnRefresh= (Button) findViewById(R.id.btn_refresh);
        btnRefresh.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	LoadUsers();
            	                   	
            }
        });
        
        btnAddUser = (Button) findViewById(R.id.btn_add);
        btnAddUser.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				User user = new User();
				
				showUserForm(user);
							
			}
        	
        });               
        
        btnReplicateUser = (Button) findViewById(R.id.btn_replicate);
        btnReplicateUser.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				replicate();

				LoadUsers();
			}
        	
        });
        
        // onload initial replication
        //replicate();
    }
    
    private void openLocalServer(Context context) {
    	try {
	    	if (localServer == null || localServer.ext().isClosed()) {
	    		// configuration for replicate
	    		EmbeddedConfiguration configuration = Db4oEmbedded.newConfiguration();
	    		
	    		configuration.file().generateUUIDs(ConfigScope.GLOBALLY);
	    		configuration.file().generateCommitTimestamps(true);
	    		
		    	localServer = Db4oEmbedded.openFile(configuration, db4oDBFullPath(context));
		    	
		    	providerB = new Db4oEmbeddedReplicationProvider(localServer);
	    	}
    	}
    	catch (Exception e) {
    		if (localServer != null && !localServer.ext().isClosed())
    			localServer.close();    		    		
    		
			e.getMessage();
		}
    }
    
    @SuppressWarnings("rawtypes")
	private void replicate() {
    	ReplicationSession replicationSession = null;
    	
    	btnReplicateUser.setEnabled(false);
    	btnAddUser.setEnabled(false);
    	btnRefresh.setEnabled(false);
    	
    	try {
    		// open remote server and create replication provider
    		if (remoteServer == null || remoteServer.ext().isClosed()) {
    			remoteServer = Db4oClientServer.openClient("192.168.1.49", 4488, "db4o", "db4o");		    
		    	    
    			providerA = new Db4oClientServerReplicationProvider(remoteServer);
    		}
		    
    		// start bidirectional replication		    
		    replicationSession = Replication.begin(providerA, providerB, new ReplicationEventListener() {
                public void onReplicate(ReplicationEvent replicationEvent) {
                	if (replicationEvent.isConflict()) {
                        ObjectState stateOfTheDesktop = replicationEvent.stateInProviderA();
                        replicationEvent.overrideWith(stateOfTheDesktop);
                    }
                	
                    ObjectState stateInDesktop = replicationEvent.stateInProviderA();
                    if (stateInDesktop.isKnown()) {
                        System.out.println("Object '" + stateInDesktop.getObject() + "' is known on desktop database");
                    }
                    if (stateInDesktop.isNew()) {
                        System.out.println("Object '" + stateInDesktop.getObject() + "' is new on desktop database");
                    }
                    if (stateInDesktop.wasModified()) {
                        System.out.println("Object '" + stateInDesktop.getObject() + "' was modified on desktop database");
                    }
                }
            });
		    		    
		    // First get the changes of the two replication-partners: remote server and local server
		    ObjectSet changesOnRemoteServer = replicationSession.providerA().objectsChangedSinceLastReplication();
		    ObjectSet changesOnLocalServer = replicationSession.providerB().objectsChangedSinceLastReplication();
		    
		    // then iterate over both change-sets and replicate it
		    for (Object changedObjectRemoteServer : changesOnRemoteServer) { 
		        replicationSession.replicate(changedObjectRemoteServer);
		    }
		    
		    for (Object changedObjectOnLocalServer : changesOnLocalServer) {
		        replicationSession.replicate(changedObjectOnLocalServer);
		    }
		    
		    // replicate deletions
		    replicationSession.replicateDeletions(User.class);
		    
		    // commit all changes
		    replicationSession.commit();
    	}
    	catch (Exception e) {
    		if (remoteServer != null && !remoteServer.ext().isClosed())
    			remoteServer.close();
    		
    		if (replicationSession != null) {
    			try {
    				replicationSession.rollback();
    			}
    			catch(Exception ex) {}
    			
    			replicationSession.close();
    		}
    		
			e.getMessage();
		}
    	finally {
        	btnReplicateUser.setEnabled(true);
        	btnAddUser.setEnabled(true);
        	btnRefresh.setEnabled(true);
    	}
    }
    
	/**
	* Returns the path for the database location
	*/	
	private String db4oDBFullPath(Context ctx) {	
		return ctx.getDir("data", 0) + "/" + LOCAL_SERVER;
	
	}

	private void LoadUsers() {
		try {
	   		// list users
			List<User> users = localServer.query(com.thingtrack.db4o.domain.User.class);
			
	        //Initialize our array adapter notice how it references the listitems.xml layout
	        userAdapter = new UserAdapter(HelloAndroidActivity.this, R.layout.list_item_user, users);
	                     
	        //Set the above adapter as the adapter of choice for our list
	        lvUsers.setAdapter(userAdapter);
	        lvUsers.setOnItemClickListener(new OnItemClickListener() {
		     	   @Override
		     	   public void onItemClick(AdapterView<?> adapter, View view, int position, long arg) {
		     	      Object listItem = lvUsers.getItemAtPosition(position);
		
		     	      showUserForm((User)listItem);
		        } 
	        });
		}
        catch (Exception e) {
    		if (remoteServer != null)
    			remoteServer.rollback();
    		
			e.getMessage();
		}
	}
	
	private void showUserForm(User user) {
		UserActivity dialog = new UserActivity(HelloAndroidActivity.this, user, new UserActivityListener() {								
			@Override
			public void onOkClick(User user) {
				try {
					localServer.store(user);
					localServer.commit();
				}
				catch (Exception e) {
					e.getMessage();
					localServer.rollback();
				}
				
				// refresh
				LoadUsers();
			}

			@Override
			public void onRemoveClick(User user) {
				try {
					localServer.delete(user);
					localServer.commit();
				}
				catch (Exception e) {
					e.getMessage();
					localServer.rollback();
				}
				
				// refresh
				LoadUsers();
			}
			
			@Override
			public void onCancelClick() {
				// refresh
				LoadUsers();
				
			}

		});
	    
    	dialog.setTitle("User Form");                        	      
      	dialog.show();
	}
}

