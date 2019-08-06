package sammyt.jotnotes;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;

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
            case R.id.action_settings:
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        RecyclerView notesRecycler = findViewById(R.id.notes_recycler);
        FloatingActionButton addNoteFab = findViewById(R.id.add_note_fab);

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
