package sammyt.jotnotes.widget;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import sammyt.jotnotes.R;
import sammyt.jotnotes.data.NoteAdapter;

public class JotNotesWidgetService extends RemoteViewsService {

    private final String LOG_TAG = this.getClass().getSimpleName();

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent){
        return new NotesRemoteViewsFactory(this.getApplicationContext());
    }
}

class NotesRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory{

    private final String LOG_TAG = this.getClass().getSimpleName();

    private Context mContext;
    private ArrayList<String[]> mNoteItems = new ArrayList<>();
    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
    private FirebaseFirestore mDb;

    public NotesRemoteViewsFactory(Context context){
        mContext = context;
    }

    public void onCreate(){
        // In onCreate() you setup any connections / cursors to your data source. Heavy lifting,
        // for example downloading or creating content etc, should be deferred to onDataSetChanged()
        // or getViewAt(). Taking more than 20 seconds in this call will result in an ANR.
    }

    public void onDestroy() {
        // In onDestroy() you should tear down anything that was setup for your data source,
        // eg. cursors, connections, etc.
    }

    public int getCount() {
        return mNoteItems.size();
    }

    public RemoteViews getViewAt(int position){

        String docId = mNoteItems.get(position)[0];
        String noteText = mNoteItems.get(position)[1];

        RemoteViews rv = new RemoteViews(mContext.getPackageName(), R.layout.widget_item);
        rv.setTextViewText(R.id.widget_item_text, noteText);

        // Set the Fill-in Intent with the view's Document ID passed in as an extra
        Intent fillInIntent = new Intent();
        fillInIntent.putExtra(NoteAdapter.EXTRA_DOC_ID, docId);

        rv.setOnClickFillInIntent(R.id.widget_item_text, fillInIntent);

        return rv;
    }

    public RemoteViews getLoadingView() {
        // You can create a custom loading view (for instance when getViewAt() is slow.) If you
        // return null here, you will get the default loading view.
        return null;
    }

    public int getViewTypeCount() {
        return 1;
    }

    public long getItemId(int position) {
        return position;
    }

    public boolean hasStableIds() {
        return true;
    }

    public void onDataSetChanged() {
        // This is triggered when you call AppWidgetManager notifyAppWidgetViewDataChanged
        // on the collection view corresponding to this factory. You can do heaving lifting in
        // here, synchronously. For example, if you need to process an image, fetch something
        // from the network, etc., it is ok to do it here, synchronously. The widget will remain
        // in its current state while work is being done here, so you don't need to worry about
        // locking up the widget.

        if(mAuth == null){
            mAuth = FirebaseAuth.getInstance();
            mUser = mAuth.getCurrentUser();
        }

        if(mUser == null){
            Log.e(LOG_TAG, "Not logged in");
            return;
        }

        if(mDb == null){
            mDb = FirebaseFirestore.getInstance();
        }

        // Query Firestore for the user's collection of notes
        // and store a reference to the query's task
        Task<QuerySnapshot> task = mDb.collection("users")
                .document(mUser.getUid())
                .collection("notes")
                .orderBy("last_updated", Query.Direction.DESCENDING)
                .get();

        try{
            // Wait for the Query task to finish and return the QuerySnapshot
            // so we can build the data set synchronously
            QuerySnapshot querySnapshot = Tasks.await(task);

            mNoteItems.clear(); // Clear any previous data if we're refreshing

            for(DocumentSnapshot document: querySnapshot.getDocuments()){
                String[] docData = {document.getId(), document.get("note_text").toString()};
                mNoteItems.add(docData);
            }

        }catch(ExecutionException | InterruptedException e){
            Log.e(LOG_TAG, "Error awaiting task " + task.toString(), e);
        }
    }
}
