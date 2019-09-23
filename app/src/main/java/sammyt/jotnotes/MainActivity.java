package sammyt.jotnotes;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.FirebaseFunctionsException;
import com.google.firebase.functions.HttpsCallableResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import sammyt.jotnotes.data.NoteAdapter;

public class MainActivity extends AppCompatActivity {

    private final String LOG_TAG = this.getClass().getSimpleName();

    private FirebaseAuth mAuth;
    private FirebaseUser mUser;

    private NoteAdapter mAdapter;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch(id) {
            case R.id.action_about:
                openAboutPage();
                return true;

            case R.id.action_delete:
                //// TODO: Verify action & re-authenticate user before performing delete
                deleteAccount();
                return true;

            case R.id.action_sign_out:
                mAuth.signOut();

                Log.d(LOG_TAG, "Signed out");
                Toast.makeText(MainActivity.this, "Signed out", Toast.LENGTH_SHORT).show();

                redirectToLogin();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // Open the About url in a browser
    public void openAboutPage(){
        Uri webpage = Uri.parse("https://trackforest.net/jot_notes/index.html");
        Intent intent = new Intent(Intent.ACTION_VIEW, webpage);

        if(intent.resolveActivity(this.getPackageManager()) != null){
            startActivity(intent);
        }
    }

    // Deletes the user's data from Firestore
    private void deleteAccount(){
        String path = "users/" + mUser.getUid();

        deleteRecursive(path)
                .addOnCompleteListener(new OnCompleteListener<HashMap>() {
                    @Override
                    public void onComplete(@NonNull Task<HashMap> task) {
                        if(task.isSuccessful()){
                            Log.d(LOG_TAG, "Successful notes delete. " + task.getResult());
                            //// TODO: Delete user
                        }else{
                            Exception e = task.getException();
                            Log.e(LOG_TAG, "Error deleting notes.", task.getException());

                            if(e instanceof FirebaseFunctionsException){
                                FirebaseFunctionsException ffe = (FirebaseFunctionsException) e;
                                FirebaseFunctionsException.Code code = ffe.getCode();
                                Object details = ffe.getDetails();

                                Log.e(LOG_TAG, "Functions Exception on Delete Notes. code: " + code
                                        + "\ndetails: " + details, ffe);
                            }
                        }
                    }
                });
    }

    // Gets the Recursive Delete callable Cloud Function
    private Task<HashMap> deleteRecursive(String path){
        // Create the arguments to the callable function.
        Map<String, Object> data = new HashMap<>();
        data.put("path", path);

        return FirebaseFunctions.getInstance()
                .getHttpsCallable("recursiveDelete")
                .call(data)
                .continueWith(new Continuation<HttpsCallableResult, HashMap>() {
                    @Override
                    public HashMap then(@NonNull Task<HttpsCallableResult> task) throws Exception {
                        // This continuation runs on either success or failure, but if the task
                        // has failed then getResult() will throw an Exception which will be
                        // propagated down
                        HashMap result = (HashMap) task.getResult().getData();
                        return result;
                    }
                });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        RecyclerView notesRecycler = findViewById(R.id.notes_recycler);
        FloatingActionButton addNoteFab = findViewById(R.id.add_note_fab);
        AdView adView = findViewById(R.id.ad_view);

        // Initialize the Mobile Ads SDK w/ my App ID
        MobileAds.initialize(this, getString(R.string.admob_app_id));

        // Load an ad
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);

        addNoteFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, EditActivity.class);
                startActivity(intent);
            }
        });

        StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(2,
                StaggeredGridLayoutManager.VERTICAL);
        notesRecycler.setLayoutManager(layoutManager);

        mAdapter = new NoteAdapter(MainActivity.this, null);
        notesRecycler.setAdapter(mAdapter);

        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    protected void onStart(){
        super.onStart();

        // Check for the currently signed in user
        mUser = mAuth.getCurrentUser();

        if(mUser == null){
            Toast.makeText(MainActivity.this, "Not signed in", Toast.LENGTH_SHORT)
                    .show();
            redirectToLogin();
        }else{
            loadNotes();
        }
    }

    private void redirectToLogin(){
        Intent loginIntent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(loginIntent);
    }

    // Loads the data to populate the Recyclerview
    private void loadNotes(){
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users")
                .document(mUser.getUid())
                .collection("notes")
                .orderBy("last_updated", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(task.isSuccessful()){
                            // Retrieve each Document ID and Note Text then update the adapter
                            ArrayList<String[]> noteData = new ArrayList<>();

                            for(QueryDocumentSnapshot document: task.getResult()){
                                String[] docData = {document.getId(), document.get("note_text").toString()};
                                noteData.add(docData);
                            }

                            mAdapter.updateNotes(noteData);

                        }else{
                            Log.e(LOG_TAG, "Error retrieving notes.", task.getException());
                        }
                    }
                });
    }
}
