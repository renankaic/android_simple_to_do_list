package cursoandroid.com.todolist_sqlite;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;

import android.os.Bundle;

import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StrikethroughSpan;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
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

    private EditText textNewTask;
    private ListView listTaskList;
    private SQLiteDatabase database;
    private TextView dayOfTasks;
    private Button btAddTask;
    private Button btAdvanceADay;
    private Button btReturnADay;

    private ArrayList<Integer> idsTarefas;

    private String selectedDate;
    private String actualDay;
    private DateFormat brDate;
    private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private DateFormat brDateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
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

        try {
            //Activity components
            textNewTask = findViewById(R.id.textoId);
            btAddTask = findViewById(R.id.botaoAdicionarId);
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

            //Save task button
            btAddTask.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String textoTarefa = textNewTask.getText().toString();

                    if( !textoTarefa.equals("")) {
                        saveTask(textoTarefa);
                        textNewTask.setText("");
                    }
                }
            });

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
                Toast.makeText(this, "Tarefa salva com sucesso!", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(MainActivity.this, "Tarefa removida com sucesso", Toast.LENGTH_SHORT).show();
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
