package in.placeitnow.placeitnow.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.*;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

import in.placeitnow.placeitnow.activities.LoginActivity;
import in.placeitnow.placeitnow.activities.MainActivity;
import in.placeitnow.placeitnow.R;
import in.placeitnow.placeitnow.recycleradapters.RecyclerAdapter;
import in.placeitnow.placeitnow.pojo.Vendor;

/**
 * Created by Pranav Gupta on 12/22/2016.
 */
public class Order extends Fragment {
    private TextView loading;
    private RecyclerView recyclerView;
    private RecyclerAdapter adapter;
    private ArrayList<Vendor> vendorsArrayList;
    private ArrayList<Vendor> selectedVendorList;

    private FirebaseAuth auth;             //FirebaseAuthentication
    private FirebaseAuth.AuthStateListener mAuthListener;
    private String uid;
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference databaseReference;
    private DatabaseReference vendors_list;
    private ChildEventListener vendors_list_listener;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View  view=  getActivity().getLayoutInflater().inflate(R.layout.order,container,false);


        DatabaseReference connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected");
        connectedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                boolean connected = snapshot.getValue(Boolean.class);
                if (connected) {
                    System.out.println("connected");
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                System.err.println("Listener was cancelled");
            }
        });

        /** getSupportActionBar is only present in AppCompatActivity while getActivity returns FragmentActivity so we first
         * need to cast to AppCompatActivity to use that method
         *
         * */
        //((AppCompatActivity) getActivity()).getSupportActionBar().setTitle("Order");


        //Firebase Auth
        auth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    uid = user.getUid();

                } else {
                    //User is signed out
                    Toast.makeText(getActivity(),"Please Sign In First",Toast.LENGTH_SHORT).show();
                    Intent i = new Intent(getActivity(),LoginActivity.class);
                    startActivity(i);
                }
                // ...
            }
        };


        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference();

        vendorsArrayList = new ArrayList<>();
        selectedVendorList = new ArrayList<>();
        loading = (TextView)view.findViewById(R.id.loading);
        recyclerView = (RecyclerView)view.findViewById(R.id.recycler_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);

        adapter = new RecyclerAdapter(getActivity(), vendorsArrayList);
        recyclerView.setAdapter(adapter);

        vendors_list = databaseReference.child("vendors");
        vendors_list.keepSynced(true);
        vendors_list_listener = vendors_list.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                final String vid = dataSnapshot.getKey();
                final String name = dataSnapshot.child("details").child("displayName").getValue(String.class);
                final Boolean status = dataSnapshot.child("details").child("status").getValue(Boolean.class);
                //show(vid +" "+name+" "+ status);
                vendorsArrayList.add(new Vendor(vid,name,status));
                loading.setText("");
                adapter.notifyDataSetChanged();

                vendors_list.child(vid).child("orders").orderByChild("orderDone").equalTo(false).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for(int i=0;i<vendorsArrayList.size();i++){
                            if(vendorsArrayList.get(i).getVid().contentEquals(vid)){
                                vendorsArrayList.get(i).setOrder_current(dataSnapshot.getChildrenCount());
                                adapter.notifyDataSetChanged();
                            }
                        }
                    }
                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                Boolean status = dataSnapshot.child("details").child("status").getValue(Boolean.class);
                for(int i=0;i<vendorsArrayList.size();i++){
                    if(vendorsArrayList.get(i).getVid().contentEquals(dataSnapshot.getKey())){
                        vendorsArrayList.get(i).setStatus(status);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                getActivity().finish();
                getActivity().overridePendingTransition( 0, 0);
                startActivity(getActivity().getIntent());
                getActivity().overridePendingTransition( 0, 0);
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        return view;

    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(android.view.Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.dashboard,menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.dashboard:
                startActivity(new Intent(getActivity(),MainActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }

    private void show(String msg){
        Toast.makeText(getActivity(),msg,Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(vendors_list!=null&&vendors_list_listener!=null){
            vendors_list.removeEventListener(vendors_list_listener);
        }
    }
}


