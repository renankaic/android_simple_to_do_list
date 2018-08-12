package cursoandroid.com.todolist_sqlite;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;

import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;

import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StrikethroughSpan;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;

import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity {

    //Date fonts
    //https://www.youtube.com/watch?v=hwe1abDO2Ag


    private ListView listTaskList;
    private SQLiteDatabase database;
    private TextView dayOfTasks;
    private Button btAdvanceADay;
    private Button btReturnADay;
    private DatePickerDialog.OnDateSetListener mDateSetListener;
    private FloatingActionButton fabNewTaskButton;

    private ArrayList<Integer> idsTarefas;

    private String selectedDate;
    private String actualDay;
    private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private DateFormat brDateFormat = new SimpleDateFormat("E dd MMM yyyy", Locale.getDefault());

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        //https://stackoverflow.com/questions/6210895/listview-inside-scrollview-is-not-scrolling-on-android
        ListView lv = findViewById(R.id.listViewId);
        lv.setOnTouchListener(new ListView.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        // Disallow ScrollView to intercept touch events.
                        v.getParent().requestDisallowInterceptTouchEvent(true);
                        break;

                    case MotionEvent.ACTION_UP:
                        // Allow ScrollView to intercept touch events.
                        v.getParent().requestDisallowInterceptTouchEvent(false);
                        break;
                }

                // Handle ListView touch events.
                v.onTouchEvent(event);
                return true;
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {

            getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        }

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(MainActivity.this, R.color.blackgray));
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }

        try {
            //Activity components
            fabNewTaskButton = findViewById(R.id.addNewTaskFAB);
            dayOfTasks = findViewById(R.id.textDayOfTasks);
            btAdvanceADay = findViewById(R.id.btAvancaDia);
            btReturnADay = findViewById(R.id.btRetornaDia);

            //Database
            database = openOrCreateDatabase("app-myTasks", MODE_PRIVATE, null);

            //Creates the table
            database.execSQL("CREATE TABLE IF NOT EXISTS tarefas(id INTEGER PRIMARY KEY AUTOINCREMENT, tarefa VARCHAR , concluida VARCHAR, dataParaConclusao DATE, repeticao VARCHAR)");

            //Gets the actual day
            selectedDate = dateFormat.format(new Date());
            actualDay = selectedDate;
            brDateFormat.getTimeZone();

            //Sets the actual day in textview
            dayOfTasks.setText("Hoje");

            //Change day buttons
            //advance
            btAdvanceADay.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        changeDayOfTasks("plus");
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
            });
            //return
            btReturnADay.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        changeDayOfTasks("minus");
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
            });

            //List
            listTaskList = findViewById(R.id.listViewId);
            listTaskList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    final int posicaoItem = position;

                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setMessage(R.string.pergunta_tarefa)
                            .setPositiveButton(R.string.resposta_tarefa_concluir, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    //Botão para marcar a tarefa como concluída
                                    concluirTarefa(idsTarefas.get(posicaoItem));
                                }
                            })
                            .setNegativeButton(R.string.resposta_tarefa_excluir, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    //Botão para remover a tarefa
                                    removerTarefa( idsTarefas.get( posicaoItem ) );
                                }
                            })
                            .setNeutralButton("Cancelar", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    //Botão cancelar para não fazer nada
                                }
                            });

                    AlertDialog alertDialog = builder.create();
                    alertDialog.show();
                }
            });

            //http://android.pcsalt.com/create-alertdialog-with-custom-layout-using-xml-layout/
            //Fab button to initalize the fab
            fabNewTaskButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    LayoutInflater inflater = getLayoutInflater();
                    View alertLayout = inflater.inflate(R.layout.layout_newtask_dialog, null);
                    final EditText newTaskName = alertLayout.findViewById(R.id.editNewTaskName);

                    AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
                    alert.setTitle("Digite a nova tarefa");

                    // this is set the view from XML inside AlertDialog
                    alert.setView(alertLayout);

                    // disallow cancel of AlertDialog on click of back button and outside touch
                    alert.setCancelable(false);
                    alert.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    });

                    alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String taskText = newTaskName.getText().toString();

                            if( !taskText.equals("")) {
                                saveTask(taskText);
                                newTaskName.setText("");
                            }
                        }
                    });

                    AlertDialog dialog = alert.create();
                    dialog.show();
                }
            });

            //Textview onclick to change the day via android calendar
            dayOfTasks.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Calendar cal = Calendar.getInstance();
                    try {
                        cal.setTime(dateFormat.parse(selectedDate));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    int year = cal.get(Calendar.YEAR);
                    int month = cal.get(Calendar.MONTH);
                    int day = cal.get(Calendar.DAY_OF_MONTH);

                    DatePickerDialog dialog = new DatePickerDialog(
                            MainActivity.this,
                            android.R.style.Theme_Holo_Dialog_MinWidth,
                            mDateSetListener,
                            year, month, day);
                    dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                    dialog.show();
                }
            });

            mDateSetListener = new DatePickerDialog.OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                    try {

                        month++;
                        Date newDate = dateFormat.parse(year + "-" + month + "-" + dayOfMonth);

                        Calendar calendar = Calendar.getInstance();
                        calendar.setTime(newDate);

                        selectedDate = dateFormat.format(calendar.getTime());
                        getAllTasksFromDate();

                        if (actualDay.equals(selectedDate)){
                            dayOfTasks.setText("Hoje");
                        } else {
                            dayOfTasks.setText( brDateFormat.format(calendar.getTime()) );
                        }

                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
            };

            //Gets all the tasks for the task list
            getAllTasksFromDate();

        } catch (Exception e){

            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("Erro")
                    .setMessage(e.getMessage());

            AlertDialog alertDialog = builder.create();
            alertDialog.show();

            e.printStackTrace();
        }
    }

    private void saveTask(String text){
        try{
            if (!text.equals("")) {

                database.execSQL("INSERT INTO tarefas (tarefa, concluida, dataParaConclusao) VALUES ('" + text + "', 'N', '" + selectedDate + "')");
                getAllTasksFromDate();

            }  else {
                Toast.makeText(MainActivity.this, "Digite a descrição da tarefa antes de adicionar", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e){
            Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void getAllTasksFromDate(){

        try {
            //Gets the tasks  
            Cursor cursor = database.rawQuery("SELECT * FROM tarefas WHERE dataParaConclusao = '"+ selectedDate + "' ORDER BY id DESC",null);

            //recuperar ids das colunas
            int indiceColunaId = cursor.getColumnIndex("id");
            int indiceColunaTarefa = cursor.getColumnIndex("tarefa");
            int indiceColunaConcluida = cursor.getColumnIndex("concluida");

            //cria o adaptador
            ArrayList<SpannableString> itens = new ArrayList<>();
            ArrayAdapter<SpannableString> itensAdaptador = new ArrayAdapter<>(
                    getApplicationContext(),
                    R.layout.items_list,
                    android.R.id.text1,
                    itens
            );

            idsTarefas = new ArrayList<>();
            listTaskList.setAdapter(itensAdaptador);

            //List the tasks
            while (cursor.moveToNext()){
                String tarefaBanco = cursor.getString(indiceColunaTarefa);
                SpannableString textoTarefa = new SpannableString(tarefaBanco);

                //Checks if the task has completed
                if ( cursor.getString(indiceColunaConcluida).equals("s") )
                {
                    textoTarefa.setSpan(new StrikethroughSpan(), 0, textoTarefa.length(), 0 );
                    textoTarefa.setSpan(new ForegroundColorSpan(Color.GRAY), 0, textoTarefa.length(), 0);
                }

                itens.add(textoTarefa);
                idsTarefas.add( Integer.parseInt( cursor.getString(indiceColunaId) ) );
            }

            cursor.close();
        } catch (Exception e){

            e.printStackTrace();
        }

    }

    private void removerTarefa(Integer id){
        try{

            database.execSQL("DELETE FROM tarefas WHERE id = " + id);
            getAllTasksFromDate();

        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private void concluirTarefa(Integer id){
        try{
            database.execSQL("UPDATE tarefas SET concluida = 's' WHERE id = " + id);
            getAllTasksFromDate();
        } catch (Exception e){
            e.printStackTrace();
        }
    }
    
    private  void changeDayOfTasks(String operation) throws ParseException {

        Date nextDay = dateFormat.parse(selectedDate);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(nextDay);

        if (operation.equals("plus")){
            calendar.add(Calendar.DATE, 1);
        } else if (operation.equals("minus")){
            calendar.add(Calendar.DATE, -1);
        }

        selectedDate = dateFormat.format(calendar.getTime());
        getAllTasksFromDate();

        if (actualDay.equals(selectedDate)){
            dayOfTasks.setText("Hoje");
        } else {
            dayOfTasks.setText( brDateFormat.format(calendar.getTime()) );
        }
    }
}
