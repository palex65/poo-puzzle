package pt.isel.poo.puzzle;

import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

public class FinishDialog extends DialogFragment implements View.OnClickListener {

	public static final int SHUFFLE=1, TERMINATE=2;
	
	Button shuffle;
	
	public FinishDialog() {	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
		View view = inflater.inflate(R.layout.finish, container);
	    getDialog().setTitle(R.string.finishDialogTitle);
	    shuffle = (Button) view.findViewById(R.id.shuffle);
	    shuffle.setOnClickListener(this);
	    ((Button) view.findViewById(R.id.finish))
	      .setOnClickListener(this);
	    return view;
	}
	
	@Override
	public void onClick(View v) {
		Puzzle p = (Puzzle) getActivity();
		p.onFinishDialog( v==shuffle ? SHUFFLE : TERMINATE);
		dismiss();
	}
}
