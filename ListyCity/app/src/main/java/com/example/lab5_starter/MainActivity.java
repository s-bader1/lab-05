package com.example.lab5_starter;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;


public class MainActivity extends AppCompatActivity implements CityDialogFragment.CityDialogListener {

    private Button addCityButton;
    private ListView cityListView;

    private ArrayList<City> cityArrayList;
    private ArrayAdapter<City> cityArrayAdapter;

    private FirebaseFirestore db;
    private CollectionReference cititesRef;

    private Button deleteButton;
    private int selectedPos = -1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Set views
        addCityButton = findViewById(R.id.buttonAddCity);
        cityListView = findViewById(R.id.listviewCities);

        // create city array
        cityArrayList = new ArrayList<>();
        cityArrayAdapter = new CityArrayAdapter(this, cityArrayList);
        cityListView.setAdapter(cityArrayAdapter);


        db = FirebaseFirestore.getInstance();
        cititesRef = db.collection("cities");

        deleteButton = findViewById(R.id.buttonDeleteCity);
        deleteButton.setEnabled(false);

        cititesRef.addSnapshotListener((value,error) -> {
            if ( error!= null){
                Log.e("Firestore", error.toString());
            }
            if(value != null){
                cityArrayList.clear();
                for (QueryDocumentSnapshot snapshot : value){
                    String name = snapshot.getString("name");
                    String province = snapshot.getString("province");

                    cityArrayList.add(new City(name, province));
                }
                cityArrayAdapter.notifyDataSetChanged();
            }
        });

//        addDummyData();

        // set listeners
        addCityButton.setOnClickListener(view -> {
            CityDialogFragment cityDialogFragment = new CityDialogFragment();
            cityDialogFragment.show(getSupportFragmentManager(),"Add City");
        });

        cityListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        cityListView.setOnItemClickListener((adapterView, view, i, l) -> {
            //This bit is for the delete button
            selectedPos = i;
            deleteButton.setEnabled(true);


            //now back to editing cities
            City city = cityArrayAdapter.getItem(i);
            CityDialogFragment cityDialogFragment = CityDialogFragment.newInstance(city);
            cityDialogFragment.show(getSupportFragmentManager(),"City Details");
        });


        deleteButton.setOnClickListener(v -> {
            if (selectedPos < 0) {
                new androidx.appcompat.app.AlertDialog.Builder(MainActivity.this)
                        .setTitle("Select a city")
                        .setMessage("Please select a city to delete.")
                        .setNegativeButton("OK", null)
                        .show();
                return;
            }

            City city = cityArrayAdapter.getItem(selectedPos);
            if (city == null) return;

            new androidx.appcompat.app.AlertDialog.Builder(MainActivity.this)
                    .setTitle("Delete city")
                    .setMessage("Delete \"" + city.getName() + ", " + city.getProvince() + "\"?")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Delete", (d, w) -> {

                        cityArrayList.remove(selectedPos);
                        cityArrayAdapter.notifyDataSetChanged();

                        cityListView.clearChoices();
                        selectedPos = -1;
                        deleteButton.setEnabled(false);

                        cititesRef.document(city.getName())
                                .delete()
                                .addOnSuccessListener(unused ->
                                        Log.d("Firestore", "Deleted city: " + city.getName()));
                    })
                    .show();
        });



    }

    @Override
    public void updateCity(City city, String newCity, String newProv) {
        String oldName = city.getName();
        city.setName(newCity);
        city.setProvince(newProv);
        cityArrayAdapter.notifyDataSetChanged();

        // udating the db using delete and addition
        if (!oldName.equals(newCity)) {
            cititesRef.document(oldName).delete()
                    .addOnSuccessListener(unused ->
                            cititesRef.document(newCity).set(city)
                                    .addOnSuccessListener(v ->
                                            Log.d("Firestore", "Renamed " + oldName + " -> " + newCity)));
        } else {
            cititesRef.document(newCity).set(city, com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener(unused ->
                            Log.d("Firestore", "Updated city: " + newCity));
        }
    }

    @Override
    public void addCity(City city){
        cityArrayList.add(city);
        cityArrayAdapter.notifyDataSetChanged();

//        DocumentReference docRef = cititesRef.document(city.getName());
        cititesRef.document(city.getName())
                .set(city)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d("Firestore", "DocumentSnapshot successfully written!");
                    }
                });

    }

    public void addDummyData(){
        City m1 = new City("Edmonton", "AB");
        City m2 = new City("Vancouver", "BC");
        cityArrayList.add(m1);
        cityArrayList.add(m2);
        cityArrayAdapter.notifyDataSetChanged();
    }
}

