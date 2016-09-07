package com.extremeboredom.locationlist;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.extremeboredom.locationlist.model.LocationItem;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import co.moonmonkeylabs.realmrecyclerview.RealmRecyclerView;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;
import io.realm.Sort;

public class ListActivity extends AppCompatActivity {

    private Realm realm;

    @BindView(R.id.realm_recycler_view)
    RealmRecyclerView realmRecyclerView;

    LocationRealmAdapter locationRealmAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        ButterKnife.bind(this);
        // Create a RealmConfiguration that saves the Realm file in the app's "files" directory.
        RealmConfiguration realmConfig = new RealmConfiguration.Builder(getApplicationContext()).deleteRealmIfMigrationNeeded().build();
        Realm.setDefaultConfiguration(realmConfig);
        // 'realm' is a field variable
        realm = Realm.getDefaultInstance();
        RealmResults<LocationItem> toDoItems = realm
                .where(LocationItem.class)
                .findAllSorted("id", Sort.ASCENDING);
        locationRealmAdapter = new LocationRealmAdapter(this, toDoItems, true, true, this);
        realmRecyclerView.setAdapter(locationRealmAdapter);
    }

    @OnClick(R.id.fab)
    void fab(){
        Intent myIntent = new Intent(ListActivity.this, LocationActivity.class);
        ListActivity.this.startActivityForResult(myIntent, 123);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //means user simply hit back
        if(data == null){
            return;
        }
        Bundle extras = data.getExtras();
        if(extras != null){
            Object edit = extras.get("edit");
            LocationItem details = null;
            if(edit instanceof LocationItem){
                details = (LocationItem) edit;
            }
            realm.beginTransaction();
            realm.copyToRealmOrUpdate(details);
            realm.commitTransaction();
            locationRealmAdapter.notifyDataSetChanged();
            Toast.makeText(getApplicationContext(), "Saved", Toast.LENGTH_SHORT).show();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void openMapWithData(LocationItem locationItem){
        Intent intent = new Intent(ListActivity.this, LocationActivity.class);
        if(locationItem!=null){
            intent.putExtra("edit", locationItem);
        }
        ListActivity.this.startActivityForResult(intent, 123);
    }
}
