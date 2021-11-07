package sammyt.jotnotes.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import sammyt.jotnotes.R;

public class DeleteAccountDialog extends DialogFragment {

    private final String LOG_TAG = this.getClass().getSimpleName();

    private DialogClickListener mListener;

    public interface DialogClickListener{
        void onPositiveClick(DeleteAccountDialog dialog);
        void onNegativeClick(DeleteAccountDialog dialog);
    }

    public void setDialogListener(DialogClickListener l){
        mListener = l;
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState){

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());

        builder.setTitle(getString(R.string.delete_account_title))
                .setMessage(getString(R.string.delete_account_confirm))
                .setPositiveButton(getString(R.string.confirm), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        if(mListener != null){
                            mListener.onPositiveClick(DeleteAccountDialog.this);
                        }
                    }
                })
                .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        if(mListener != null){
                            mListener.onNegativeClick(DeleteAccountDialog.this);
                        }
                    }
                });

        return builder.create();
    }
}