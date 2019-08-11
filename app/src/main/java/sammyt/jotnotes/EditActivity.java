package sammyt.jotnotes;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import sammyt.jotnotes.data.NoteAdapter;
import sammyt.jotnotes.widget.JotNotesWidget;

public class EditActivity extends AppCompatActivity {

    private final String LOG_TAG = this.getClass().getSimpleName();

    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
    private FirebaseFirestore mDb;

    private String mDocId;

    private EditText mNoteEdit;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_edit, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch(id) {
            case R.id.action_about:
                openAboutPage();
                return true;

            case R.id.action_sign_out:
                mAuth.signOut();

                Log.d(LOG_TAG, "Signed out");
                Toast.makeText(EditActivity.this, "Signed out", Toast.LENGTH_SHORT).show();
                redirectToLogin();
                return true;

            case R.id.action_save:
                saveNoteData();
                return true;

            case R.id.action_delete:
                deleteNoteData();
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        mNoteEdit = findViewById(R.id.edit_note);

        // Check if there's a document ID passed in
        Intent intent = getIntent();
        if(intent.hasExtra(NoteAdapter.EXTRA_DOC_ID)){
            mDocId = intent.getStringExtra(NoteAdapter.EXTRA_DOC_ID);
        }

        mAuth = FirebaseAuth.getInstance();
        mDb = FirebaseFirestore.getInstance();
    }

    @Override
    protected void onStart(){
        super.onStart();

        // Check for the currently signed in user
        mUser = mAuth.getCurrentUser();

        if(mUser == null){
            Toast.makeText(EditActivity.this, "Not signed in", Toast.LENGTH_SHORT)
                    .show();
            redirectToLogin();

        }else if(mDocId != null){ // Check if we're working on an existing document
            loadNoteData();
        }
    }

    private void redirectToLogin(){
        Intent loginIntent = new Intent(EditActivity.this, LoginActivity.class);
        startActivity(loginIntent);
    }

    private void loadNoteData(){
        mDb.collection("users")
                .document(mUser.getUid())
                .collection("notes")
                .document(mDocId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        String noteText = documentSnapshot.get("note_text").toString();
                        mNoteEdit.setText(noteText);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(LOG_TAG, "Error retrieving note data.", e);
                        Toast.makeText(EditActivity.this, "Error retrieving note data",
                                Toast.LENGTH_SHORT)
                                .show();
                    }
                });
    }

    private void saveNoteData(){
        hideKeyboard();

        String noteText = mNoteEdit.getText().toString();

        Map<String, Object> noteData = new HashMap<>();
        noteData.put("note_text", noteText);
        noteData.put("last_updated", new Timestamp(new Date()));

        CollectionReference userNotesRef = mDb.collection("users")
                .document(mUser.getUid())
                .collection("notes");

        if(mDocId != null){
            // Update the current note document
            userNotesRef.document(mDocId)
                    .update(noteData)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if(task.isSuccessful()){
                                Toast.makeText(EditActivity.this, "Saved", Toast.LENGTH_SHORT)
                                        .show();
                                updateWidgets();
                            }else{
                                Log.e(LOG_TAG, "Error updating note document", task.getException());
                                Toast.makeText(EditActivity.this, "Error updating note",
                                        Toast.LENGTH_SHORT)
                                        .show();
                            }
                        }
                    });
        }else{
            // Create a new note document
            userNotesRef.add(noteData)
                    .addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentReference> task) {
                            if(task.isSuccessful()){
                                mDocId = task.getResult().getId();
                                Toast.makeText(EditActivity.this, "Saved", Toast.LENGTH_SHORT)
                                        .show();
                                updateWidgets();
                            }else{
                                Log.e(LOG_TAG, "Error creating note document", task.getException());
                                Toast.makeText(EditActivity.this, "Error creating note",
                                        Toast.LENGTH_SHORT)
                                        .show();
                            }
                        }
                    });
        }
    }

    // Deletes the current note from Firestore and returns to the Main Activity
    private void deleteNoteData(){
        if(mDocId == null){
            return; // Return early if there's no Document ID to delete
        }

        hideKeyboard();

        mDb.collection("users")
                .document(mUser.getUid())
                .collection("notes")
                .document(mDocId)
                .delete()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()){
                            Toast.makeText(EditActivity.this, "Note Deleted", Toast.LENGTH_SHORT)
                                    .show();
                            updateWidgets();
                            onBackPressed(); // Return to the previous activity
                        }else{
                            Log.e(LOG_TAG, "Error deleting note document", task.getException());
                            Toast.makeText(EditActivity.this, "Error deleting note", Toast.LENGTH_SHORT)
                                    .show();
                        }
                    }
                });
    }

    private void hideKeyboard(){
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
    }

    // Notifies any set widgets to update their data
    private void updateWidgets(){
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(EditActivity.this);
        int[] ids = appWidgetManager.getAppWidgetIds(new ComponentName(EditActivity.this,
                JotNotesWidget.class));
        appWidgetManager.notifyAppWidgetViewDataChanged(ids, R.id.widget_list);
    }
}
