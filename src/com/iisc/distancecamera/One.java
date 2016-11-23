package com.iisc.distancecamera;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.content.Intent;

public class One extends Activity {

    EditText inputHeight;
    EditText inputDistance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_one);

        inputHeight = (EditText) findViewById(R.id.height);
        inputDistance = (EditText) findViewById(R.id.distance);
        Button button1 = (Button) findViewById(R.id.buttonheight);
        Button button2 = (Button) findViewById(R.id.buttondistance);

        // Listening to button event
        button1.setOnClickListener(new View.OnClickListener() {

            public void onClick(View arg0) {
                // Starting a new Intent
                Intent nextScreen = new Intent(getApplicationContext(), Two.class);

                // Sending data to another Activity
                nextScreen.putExtra("height", inputHeight.getText().toString());
                nextScreen.putExtra("temp", "0");

                // starting new activity
                startActivity(nextScreen);
            }
        });

        button2.setOnClickListener(new View.OnClickListener() {

            public void onClick(View arg0) {
                // Starting a new Intent
                Intent nextScreen = new Intent(getApplicationContext(), Two.class);
                // Sending data to another Activity
                nextScreen.putExtra("distance", inputDistance.getText().toString());
                nextScreen.putExtra("temp", "1");
                // starting new activity
                startActivity(nextScreen);

            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.one, menu);
        return true;
    }

}
