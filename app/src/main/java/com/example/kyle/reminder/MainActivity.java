package com.example.kyle.reminder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;


public class MainActivity extends AppCompatActivity {

    private reminderDatabase database;
    private SimpleCursorAdapter cursorAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // sets listView in mainActivity to contents of database
        database = new reminderDatabase(this);
        final Cursor cursor = database.getAllItems();

        //broadcastManager to wait for AlarmService to finish
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
        IntentFilter filter = new IntentFilter("FINISHED");
        broadcastManager.registerReceiver(deleteReceiver, filter);

        String[] columns = new String[]{
                reminderDatabase.DB_COLUMN_TITLE,
                reminderDatabase.DB_COLUMN_CONTENT
        };
        int[] widgets = new int[]{
                R.id.title,
                R.id.reminder
        };

        cursorAdapter = new SimpleCursorAdapter(this, R.layout.list_item_layout,
                cursor, columns, widgets, 0);

        ListView listView = (ListView) findViewById(R.id.reminderList);
        listView.setAdapter(cursorAdapter);
        refresh();


        //short press checks for item type and executes corresponding activity
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Cursor item = (Cursor) adapterView.getItemAtPosition(i);
                int id = item.getInt(item.getColumnIndex(reminderDatabase.DB_COLUMN_ID));
                String type = item.getString(item.getColumnIndex(reminderDatabase.DB_COLUMN_TYPE));

                if (type.equalsIgnoreCase("note")) {
                    Intent intent = new Intent(MainActivity.this, createOrEditNote.class);
                    intent.putExtra("noteID", id);
                    startActivity(intent);
                    finish();
                } else {
                    Intent intent = new Intent(MainActivity.this, createOrEditAlert.class);
                    intent.putExtra("alertID", id);
                    startActivity(intent);
                    finish();
                }


            }
        });
        //long press for delete
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                Cursor item = (Cursor) adapterView.getItemAtPosition(i);
                int id = item.getInt(item.getColumnIndex(reminderDatabase.DB_COLUMN_ID));
                deleteDialog(id).show();
                return true;
            }
        });


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add_note:
                startActivity(new Intent(this, createOrEditNote.class));
                finish();
                break;
            case R.id.action_add_alert:
                startActivity(new Intent(this, createOrEditAlert.class));
                finish();
                break;
            case R.id.action_settings:
                break;
            default:
                break;
        }

        return true;
    }

    private AlertDialog deleteDialog(int id) {
        final int deleteId = id;
        final Cursor cursor = database.getItem(id);
        cursor.moveToFirst();
        return new AlertDialog.Builder(this)
                .setTitle("Confirm")
                .setMessage("Do you want to delete?")

                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int i) {
                        //if the selected item for deletion is an alert, cancel the alarm
                        if ((cursor.getString(cursor.getColumnIndex(reminderDatabase.DB_COLUMN_TYPE)).equals("alert"))) {
                            Intent cancel = new Intent(MainActivity.this, AlarmService.class);
                            cancel.putExtra("id", deleteId);
                            cancel.putExtra("deleteFromMain", true);
                            cancel.setAction(AlarmService.CANCEL);
                            startService(cancel);
                        } else {
                            database.deleteItem(deleteId);
                        }
                        refresh();
                        dialog.dismiss();
                    }

                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int i) {
                        dialog.dismiss();

                    }
                })
                .create();

    }

    private void refresh() {
        Cursor cursor = database.getAllItems();
        cursorAdapter.changeCursor(cursor);
    }

    //receives signal of deletion of alarm from AlarmService and then refreshes UI
    private BroadcastReceiver deleteReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("FINISHED")) {
                refresh();
            }
        }
    };
}

