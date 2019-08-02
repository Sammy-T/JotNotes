package sammyt.jotnotes;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private final String LOG_TAG = this.getClass().getSimpleName();

    private FirebaseAuth mAuth;
    private FirebaseUser mUser;

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
//                redirectToLogin();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    protected void onStart(){
        super.onStart();

        // Check for the currently signed in user
        mUser = mAuth.getCurrentUser();

        if(mUser == null){
            Toast.makeText(MainActivity.this, "no user signed in - redirecting", Toast.LENGTH_SHORT)
                    .show();
            redirectToLogin();
        }
    }

    private void redirectToLogin(){
        Intent loginIntent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(loginIntent);
    }
}