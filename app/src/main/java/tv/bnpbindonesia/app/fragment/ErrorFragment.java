package tv.bnpbindonesia.app.fragment;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import tv.bnpbindonesia.app.MainActivity;
import tv.bnpbindonesia.app.R;

public class ErrorFragment extends Fragment {
    private static final String ARG_ACTION = "action";
    private static final String ARG_MESSAGE = "message";

    private String action;
    private String message;

    private TextView viewMessage;
    private Button viewRetry;

    public ErrorFragment() {
        // Required empty public constructor
    }

    public static ErrorFragment newInstance(String action, String message) {
        ErrorFragment fragment = new ErrorFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ACTION, action);
        args.putString(ARG_MESSAGE, message);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            action = getArguments().getString(ARG_ACTION);
            message = getArguments().getString(ARG_MESSAGE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_error, container, false);

        viewMessage = (TextView) rootView.findViewById(R.id.message);
        viewRetry = (Button) rootView.findViewById(R.id.retry);

        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewMessage.setText(message);
        viewRetry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) getActivity()).onRetry(action);
            }
        });
    }
}