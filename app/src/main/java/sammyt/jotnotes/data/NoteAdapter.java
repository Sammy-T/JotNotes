package sammyt.jotnotes.data;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Map;

import sammyt.jotnotes.EditActivity;
import sammyt.jotnotes.R;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.ViewHolder>{

    private final String LOG_TAG = this.getClass().getSimpleName();

    private Context mContext;
    private ArrayList<String[]> mNoteList = new ArrayList<>(); // String[Document ID, Note Text]

    public static final String EXTRA_DOC_ID = "extra_doc_id";

    public static class ViewHolder extends RecyclerView.ViewHolder{

        TextView noteText;

        public ViewHolder(View view){
            super(view);

            noteText = view.findViewById(R.id.note_text);
        }
    }

    public NoteAdapter(Context context, ArrayList<String[]> noteList){
        mContext = context;

        if(noteList != null){
            mNoteList = noteList;
        }
    }

    // Create new views (invoked by Layout Manager)
    @Override
    @NonNull
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
        // set the view's size, margins, paddings and layout parameters here if needed

        int layout = R.layout.note_item;

        View view = LayoutInflater.from(parent.getContext())
                .inflate(layout, parent, false);

        ViewHolder viewHolder = new ViewHolder(view);

        return viewHolder;
    }

    // Replace the contents of the view (invoked by Layout Manager)
    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position){
        String text = mNoteList.get(position)[1];
        holder.noteText.setText(text);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int pos = holder.getAdapterPosition();
                String docId = mNoteList.get(pos)[0];

                // Open the Edit Activity w/ the Document ID passed in
                Intent intent = new Intent(mContext, EditActivity.class);
                intent.putExtra(EXTRA_DOC_ID, docId);

                mContext.startActivity(intent);
            }
        });
    }

    // Return the size of the dataset (invoked by Layout Manager)
    @Override
    public int getItemCount(){
        return mNoteList.size();
    }

    public void updateNotes(ArrayList<String[]> noteList){
        if(noteList != null){
            mNoteList = noteList;
            notifyDataSetChanged();
        }
    }
}
